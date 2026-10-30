package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.enums.SourceTier;
import cn.smallyoung.familydocsentinel.enums.StanceType;
import cn.smallyoung.familydocsentinel.enums.UnverifiedCause;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 证据立场标注与四项硬校验服务（模型步骤 2，对应需求文档 3.7、3.8、4.6、4.10、5 节）
 * <p>
 * 核心原则：
 * 1. 模型只负责读证据、标立场、摘引文、写转述说明，绝不下最终结论；
 * 2. 除“未涉及（IRRELEVANT）”外，每条立场须附 <=60 字的摘要逐字引文；
 * 3. 说明文字 <=120 字，只能转述已摘引文，不得引入新的医学结论；
 * 4. 严格执行四项硬校验（任一不通过则该立场条目作废）：
 *    (1) 链接必须在本次检索真实返回的链接集合中；
 *    (2) 引文必须在对应摘要中逐字出现（忽略空白差异）且 <=60 字；
 *    (3) 被引来源必须是一级或二级；
 *    (4) 说明文字通过禁用词扫描，且其中出现的数字必须能在引文中找到；
 * 5. 全部作废时标记 allInvalidated=true，后续按“没查实·引用未通过校验”处理，并附已检索材料标注“未据此下结论”。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StanceAnnotationService {

    /**
     * 单条引文最大字符数限制（4.6 节：<=60 字）
     */
    private static final int MAX_QUOTE_LENGTH = 60;

    /**
     * 说明文字最大字符数限制（4.6 节：<=120 字）
     */
    private static final int MAX_EXPLANATION_LENGTH = 120;

    /**
     * 提取文本中所有数字（整数或小数）用于数字溯源校验
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");

    /**
     * 匹配所有空白字符（用于忽略空白差异的逐字包含比对）
     */
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    private final ChatClient chatClient;
    private final FactCheckProperties properties;

    private static final String STANCE_SYSTEM_PROMPT = """
            你是医学事实核验系统的“证据立场标注与引文摘录器”（模型步骤2）。
            你【严禁】使用外部知识，只能严格依据输入的候选证据摘要，对目标论断逐条标注立场并摘录原文。
            
            一、立场分类（stance，五选一）：
            - SUPPORT：来源明确支持该论断
            - REFUTE：来源直接否定，明确表述该说法不成立、无效或属于误区
            - CONDITIONAL：仅在特定剂量、特定人群、特定体质或前提条件下成立
            - INSUFFICIENT：来源明确提到目前尚无充分科学证据或研究不足，但未直接否定
            - IRRELEVANT：来源未涉及该论断核心内容
            
            二、逐字引文要求（quote）：
            - 除 IRRELEVANT 外，每条证据必须从其【来源摘要】中【一字不差】地复制 1 句核心原话作为 quote；
            - quote 长度必须严格 <= 60 个字符，不得改写、不得省略号拼接、不得编造，必须是来源摘要的连续子串；
            - 若 stance 为 IRRELEVANT，quote 传空字符串 ""。
            
            三、说明文字要求（explanation）：
            - 基于已摘录的 quote 编写一段 <= 120 字的通俗说明文字，面向中老年读者解释证据说了什么；
            - 只能转述本次已摘录的 quote 内容，严禁引入摘要之外的任何新医学结论或用药建议；
            - 说明文字中如果出现任何数字，该数字必须已经在你摘录的 quote 中出现过；
            - 严禁使用“权威”、“100%正确”、“绝对属实”、“名医背书”等词汇，统一使用“可信来源”、“查证来源”。
            
            只输出合法 JSON 对象，格式如下：
            {
              "annotations": [
                {
                  "index": 1,
                  "url": "与输入完全一致的链接",
                  "stance": "SUPPORT|REFUTE|CONDITIONAL|INSUFFICIENT|IRRELEVANT",
                  "quote": "从该条来源摘要中逐字复制的不超过60字的原话"
                }
              ],
              "explanation": "不超过120字的引文转述说明"
            }
            """;

    /**
     * 立场标注与四项硬校验执行结果
     *
     * @param validEvidences        通过全部四项校验且立场非“未涉及”的有效证据列表
     * @param invalidatedMaterials  当存在非 IRRELEVANT 标注但全部未通过校验作废时，附带展示的原始材料（已标“未据此下结论”）
     * @param validatedExplanation  通过禁用词与数字校验的说明文字（<=120字）
     * @param allInvalidatedByCheck 是否因四项校验不通过导致全部非“未涉及”立场条目作废
     * @param modelFailed           是否因模型调用超时或格式错误（重试1次仍失败）导致无法完成
     */
    public record StanceCheckResult(
            List<EvidenceItemDto> validEvidences,
            List<EvidenceItemDto> invalidatedMaterials,
            String validatedExplanation,
            boolean allInvalidatedByCheck,
            boolean modelFailed
    ) {
    }

    /**
     * 对准入的前 4 条证据执行大模型立场标注与代码级四项硬校验
     *
     * @param claimText         待核验原子论断
     * @param admittedEvidences 准入的 Top 4 证据列表
     * @return 标注与校验结果
     */
    public StanceCheckResult annotateAndValidate(String claimText, List<EvidenceItemDto> admittedEvidences) {
        if (admittedEvidences == null || admittedEvidences.isEmpty()) {
            return new StanceCheckResult(List.of(), List.of(), "", false, false);
        }

        // 1. 构建候选证据输入文本与真实链接映射表
        Map<String, EvidenceItemDto> urlToEvidenceMap = new LinkedHashMap<>();
        Map<Integer, EvidenceItemDto> indexToEvidenceMap = new LinkedHashMap<>();
        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("【待核验论断】：").append(claimText).append("\n\n【候选证据列表】：\n");

        for (int i = 0; i < admittedEvidences.size(); i++) {
            EvidenceItemDto ev = admittedEvidences.get(i);
            int idx = ev.getIndex() != null ? ev.getIndex() : (i + 1);
            ev.setIndex(idx);
            urlToEvidenceMap.put(ev.getUrl(), ev);
            indexToEvidenceMap.put(idx, ev);

            userPromptBuilder.append("证据 ").append(idx).append("：\n")
                    .append("- 链接(url)：").append(ev.getUrl()).append("\n")
                    .append("- 来源机构：").append(ev.getSourceOrg()).append("（").append(ev.getTierLabel()).append("）\n")
                    .append("- 标题：").append(ev.getTitle()).append("\n")
                    .append("- 来源摘要：").append(ev.getSourceSummary()).append("\n\n");
        }

        // 2. 调用大模型（格式异常时重试 1 次，不占检索额度，对应需求第 5 节）
        JSONObject modelJson = callModelWithRetry(STANCE_SYSTEM_PROMPT, userPromptBuilder.toString(), 1200, 1);
        if (modelJson == null) {
            return new StanceCheckResult(List.of(), List.of(), "", false, true);
        }

        // 3. 解析并逐条执行前 3 项硬校验（链接真实性、逐字引文包含、来源等级）
        JSONArray annotations = modelJson.getJSONArray("annotations");
        String rawExplanation = modelJson.getString("explanation");

        List<EvidenceItemDto> passedList = new ArrayList<>();
        int nonIrrelevantAttemptCount = 0;

        if (annotations != null) {
            Set<String> processedUrls = new HashSet<>();
            for (int i = 0; i < annotations.size(); i++) {
                JSONObject itemObj = annotations.getJSONObject(i);
                if (itemObj == null) {
                    continue;
                }
                StanceType stance = StanceType.from(itemObj.getString("stance"));
                if (stance == StanceType.IRRELEVANT) {
                    continue;
                }
                nonIrrelevantAttemptCount++;

                String returnedUrl = itemObj.getString("url") != null ? itemObj.getString("url").strip() : "";
                Integer returnedIdx = itemObj.getInteger("index");
                String quote = itemObj.getString("quote") != null ? itemObj.getString("quote").strip() : "";

                // 校验 1：链接必须在本次检索真实返回的链接集合中
                EvidenceItemDto matchedSource = urlToEvidenceMap.get(returnedUrl);
                if (matchedSource == null && returnedIdx != null) {
                    // 若模型仅返回了序号且未篡改域名，检查序号对应项且要求 returnedUrl 为空或与真实链接同源
                    EvidenceItemDto byIdx = indexToEvidenceMap.get(returnedIdx);
                    if (byIdx != null && (returnedUrl.isEmpty() || byIdx.getUrl().equalsIgnoreCase(returnedUrl))) {
                        matchedSource = byIdx;
                    }
                }
                if (matchedSource == null || !urlToEvidenceMap.containsKey(matchedSource.getUrl())) {
                    log.warn("Validation failed [Rule 1 - Link not in search results]: url={}", returnedUrl);
                    continue;
                }
                if (!processedUrls.add(matchedSource.getUrl())) {
                    continue;
                }

                // 校验 2：引文必须在对应来源摘要中逐字出现（忽略空白差异），且非空、<=60 字
                if (!isQuoteVerbatimValid(quote, matchedSource.getSourceSummary())) {
                    log.warn("Validation failed [Rule 2 - Quote not verbatim or >60 chars]: quote={}, url={}",
                            quote, matchedSource.getUrl());
                    continue;
                }

                // 校验 3：被引来源必须是一级或二级
                if (!isTierAdmitted(matchedSource.getTierCode())) {
                    log.warn("Validation failed [Rule 3 - Source tier invalid]: tier={}, url={}",
                            matchedSource.getTierCode(), matchedSource.getUrl());
                    continue;
                }

                // 复制并填充通过前3项校验的证据条目
                EvidenceItemDto validEv = copyEvidence(matchedSource);
                validEv.setStanceCode(stance.getCode());
                validEv.setStanceLabel(stance.getLabel());
                validEv.setQuote(quote);
                validEv.setCitationValid(true);
                passedList.add(validEv);
            }
        }

        // 4. 校验 4：说明文字通过禁用词扫描，且其中出现的数字须能在引文中找到
        String combinedQuotes = passedList.stream()
                .map(EvidenceItemDto::getQuote)
                .filter(Objects::nonNull)
                .reduce("", (a, b) -> a + " " + b);

        String sanitizedExplanation = sanitizeAndValidateExplanation(rawExplanation, combinedQuotes, passedList);
        if (sanitizedExplanation == null) {
            // 若说明文字数字越界且无法安全回退，按校验不通过作废
            log.warn("Validation failed [Rule 4 - Explanation numbers out of quote bounds]: rawExplanation={}", rawExplanation);
            passedList.clear();
        }

        // 5. 若模型标注了非“未涉及”条目（或候选有证据但说明文字越界）导致全部作废 -> 按“没查实·引用未通过校验”处理
        boolean allInvalidated = (nonIrrelevantAttemptCount > 0 && passedList.isEmpty());
        List<EvidenceItemDto> fallbackMaterials = new ArrayList<>();
        if (allInvalidated) {
            for (EvidenceItemDto src : admittedEvidences) {
                EvidenceItemDto material = copyEvidence(src);
                material.setCitationValid(false);
                material.setNote(UnverifiedCause.INVALIDATED_EVIDENCE_NOTE);
                fallbackMaterials.add(material);
            }
        }

        return new StanceCheckResult(
                passedList,
                fallbackMaterials,
                sanitizedExplanation != null ? sanitizedExplanation : "",
                allInvalidated,
                false
        );
    }

    /**
     * 校验 2：验证引文是否非空、<=60 字，且在来源摘要中逐字出现（忽略空白符差异）
     */
    public boolean isQuoteVerbatimValid(String quote, String sourceSummary) {
        if (quote == null || quote.isBlank() || sourceSummary == null || sourceSummary.isBlank()) {
            return false;
        }
        String trimmedQuote = quote.strip();
        if (trimmedQuote.length() > MAX_QUOTE_LENGTH) {
            return false;
        }
        String normalizedQuote = WHITESPACE_PATTERN.matcher(trimmedQuote).replaceAll("");
        String normalizedSummary = WHITESPACE_PATTERN.matcher(sourceSummary).replaceAll("");
        return !normalizedQuote.isEmpty() && normalizedSummary.contains(normalizedQuote);
    }

    /**
     * 校验 3：验证来源等级是否属于一级（TIER_1）或二级（TIER_2）
     */
    public boolean isTierAdmitted(String tierCode) {
        return SourceTier.TIER_1.name().equals(tierCode) || SourceTier.TIER_2.name().equals(tierCode);
    }

    /**
     * 校验 4：对说明文字执行禁用词替换、长度控制（<=120字）与数字溯源校验
     * <p>
     * 若模型生成的说明文字包含未在有效引文中出现的数字，则自动退化为仅由有效引文拼接的客观转述，
     * 从而确保 100% 不越界（需求第 6 节：说明文字越界 0 条）。
     * </p>
     */
    public String sanitizeAndValidateExplanation(
            String rawExplanation,
            String combinedQuotes,
            List<EvidenceItemDto> validEvidences) {

        if (validEvidences == null || validEvidences.isEmpty()) {
            return "";
        }
        String candidate = rawExplanation != null ? rawExplanation.strip() : "";
        candidate = replaceForbiddenWords(candidate);

        // 检查 candidate 中的所有数字是否均在 combinedQuotes 中出现
        if (!candidate.isEmpty() && areAllNumbersInQuotes(candidate, combinedQuotes)) {
            return truncateExplanation(candidate);
        }

        // 若模型生成的说明文字为空或出现了引文之外的数字，使用有效引文安全构造纯转述说明（确保包含的数字100%来自引文）
        EvidenceItemDto top = validEvidences.getFirst();
        String safeFallback = "查证来源（" + top.getSourceOrg() + "）摘录指出：“" + top.getQuote() + "”";
        safeFallback = replaceForbiddenWords(safeFallback);
        if (areAllNumbersInQuotes(safeFallback, top.getSourceOrg() + " " + combinedQuotes)) {
            return truncateExplanation(safeFallback);
        }
        return null;
    }

    /**
     * 检查说明文字中出现的所有数字是否都能在引文中找到（对应需求 4.6 节第 4 条校验）
     */
    public boolean areAllNumbersInQuotes(String explanation, String quotesText) {
        if (explanation == null || explanation.isBlank()) {
            return true;
        }
        String safeQuotes = quotesText == null ? "" : quotesText;
        Matcher matcher = NUMBER_PATTERN.matcher(explanation);
        while (matcher.find()) {
            String num = matcher.group();
            if (!safeQuotes.contains(num)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 扫描并替换说明文字中的禁用词（对应需求 4.10 节）
     */
    public String replaceForbiddenWords(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String result = text;
        Map<String, String> replacements = properties.getCompliance().getExplanationForbiddenReplacements();
        if (replacements != null) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isEmpty() && entry.getValue() != null) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    private String truncateExplanation(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").strip();
        if (cleaned.length() <= MAX_EXPLANATION_LENGTH) {
            return cleaned;
        }
        return cleaned.substring(0, MAX_EXPLANATION_LENGTH - 1) + "…";
    }

    private JSONObject callModelWithRetry(String systemPrompt, String userPrompt, int maxTokens, int maxRetries) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                String raw = chatClient.prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .options(DeepSeekChatOptions.builder().maxTokens(maxTokens))
                        .call()
                        .content();
                if (raw != null && !raw.isBlank()) {
                    int start = raw.indexOf('{');
                    int end = raw.lastIndexOf('}');
                    if (start >= 0 && end > start) {
                        JSONObject obj = JSON.parseObject(raw.substring(start, end + 1));
                        if (obj != null && obj.containsKey("annotations")) {
                            return obj;
                        }
                    }
                }
                log.warn("StanceAnnotation model format invalid on attempt {}, raw={}", attempt + 1, raw);
            } catch (cn.smallyoung.familydocsentinel.exception.BizException bizEx) {
                throw bizEx;
            } catch (Exception e) {
                log.warn("StanceAnnotation model call error on attempt {}", attempt + 1, e);
            }
        }
        return null;
    }

    private static EvidenceItemDto copyEvidence(EvidenceItemDto src) {
        return EvidenceItemDto.builder()
                .index(src.getIndex())
                .sourceOrg(src.getSourceOrg())
                .domain(src.getDomain())
                .title(src.getTitle())
                .publishDate(src.getPublishDate())
                .sourceSummary(src.getSourceSummary())
                .url(src.getUrl())
                .tierCode(src.getTierCode())
                .tierLabel(src.getTierLabel())
                .mediaReport(src.isMediaReport())
                .sourceTag(src.getSourceTag())
                .relevanceScore(src.getRelevanceScore())
                .rankScore(src.getRankScore())
                .stanceCode(src.getStanceCode())
                .stanceLabel(src.getStanceLabel())
                .quote(src.getQuote())
                .citationValid(src.isCitationValid())
                .note(src.getNote())
                .build();
    }
}
