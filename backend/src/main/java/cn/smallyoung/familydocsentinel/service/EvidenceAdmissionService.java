package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.dto.WsaSearchPage;
import cn.smallyoung.familydocsentinel.enums.SourceTier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 证据清洗、去重、分级准入与排序服务（对应需求文档 US-03、4.4、4.5 节）
 * <p>
 * 核心规则：
 * 1. 来源分级：仅命中一级、二级白名单的来源准入；零级来源一律剔除，不参与结论、不展示引文；
 * 2. 行业过滤返回的站点不自动算一级，仍须命中白名单；
 * 3. 主流媒体健康频道归为二级来源，并强制打标“媒体报道”；
 * 4. 去重规则：按规范化链接去重 + 同站同标题去重；
 * 5. 排序规则：排序分 = 相关度 × 等级系数（一级 1.5，二级 1.2），取前 4 条送入立场标注；
 * 6. 摘要截取：每条来源摘要按句截取至 800 字以内；发布日期缺失显示“日期未标注”。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceAdmissionService {

    private static final int MAX_ADMITTED_EVIDENCES = 4;
    private static final int MAX_SUMMARY_LENGTH = 800;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final FactCheckProperties properties;

    /**
     * 来源分级判定内部结果
     *
     * @param tier        来源等级（TIER_1 / TIER_2 / TIER_0）
     * @param mediaReport 是否为主流媒体健康报道（需标注“媒体报道”）
     * @param domain      提取出的规范化域名
     */
    public record SourceClassification(SourceTier tier, boolean mediaReport, String domain) {
    }

    /**
     * 对检索返回的原始结果集合执行清洗、白名单分级准入、去重、排序并截取前 4 条
     *
     * @param rawPages 原始检索结果列表
     * @return 准入并按排序分降序排列的前 4 条证据列表
     */
    public List<EvidenceItemDto> admitAndRank(List<WsaSearchPage> rawPages) {
        if (rawPages == null || rawPages.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seenUrls = new HashSet<>();
        Set<String> seenSiteTitles = new HashSet<>();
        List<EvidenceItemDto> admittedList = new ArrayList<>();

        for (WsaSearchPage page : rawPages) {
            if (page == null || page.getUrl() == null || page.getUrl().isBlank()) {
                continue;
            }
            String normalizedUrl = normalizeUrl(page.getUrl());
            if (normalizedUrl.isEmpty()) {
                continue;
            }

            // 1. 提取摘要（优先取动态摘要 content，为空时取标准摘要 passage）
            String rawSummary = firstNonBlank(page.getContent(), page.getPassage());
            if (rawSummary.isBlank()) {
                continue;
            }

            // 2. 来源白名单分级准入判定
            SourceClassification classification = classifySource(normalizedUrl, page.getSite(), page.getTitle());
            if (!classification.tier().isAdmitted()) {
                // 零级来源不准入、不参与结论、不展示引文
                continue;
            }

            // 3. 规范化链接去重
            if (!seenUrls.add(normalizedUrl)) {
                continue;
            }

            // 4. 同站同标题去重
            String cleanTitle = normalizeTitle(page.getTitle());
            String siteTitleKey = classification.domain() + "::" + cleanTitle;
            if (!cleanTitle.isEmpty() && !seenSiteTitles.add(siteTitleKey)) {
                continue;
            }

            // 5. 计算相关度与排序分（排序分 = 相关度 × 等级系数）
            double relevanceScore = (page.getScore() != null && page.getScore() > 0)
                    ? Math.min(1.0, page.getScore())
                    : 0.6;
            double rankScore = relevanceScore * classification.tier().getWeightFactor();

            // 6. 摘要按句截取至 800 字以内，日期格式化（缺失显示“日期未标注”）
            String truncatedSummary = truncateBySentence(rawSummary, MAX_SUMMARY_LENGTH);
            String publishDate = page.getDate() != null
                    ? page.getDate().format(DATE_FORMATTER)
                    : "日期未标注";
            String sourceOrg = !isBlank(page.getSite()) ? page.getSite().strip() : classification.domain();

            EvidenceItemDto item = EvidenceItemDto.builder()
                    .sourceOrg(sourceOrg)
                    .domain(classification.domain())
                    .title(isBlank(page.getTitle()) ? sourceOrg : page.getTitle().strip())
                    .publishDate(publishDate)
                    .sourceSummary(truncatedSummary)
                    .url(normalizedUrl)
                    .tierCode(classification.tier().name())
                    .tierLabel(classification.tier().getLabel())
                    .mediaReport(classification.mediaReport())
                    .sourceTag(classification.mediaReport() ? "媒体报道" : classification.tier().getLabel())
                    .relevanceScore(relevanceScore)
                    .rankScore(Math.round(rankScore * 10000.0) / 10000.0)
                    .build();

            admittedList.add(item);
        }

        // 7. 按排序分降序排序，截取前 4 条并赋予序号 1~4
        admittedList.sort(Comparator.comparingDouble(EvidenceItemDto::getRankScore).reversed());
        List<EvidenceItemDto> topEvidences = new ArrayList<>(
                admittedList.subList(0, Math.min(MAX_ADMITTED_EVIDENCES, admittedList.size()))
        );
        for (int i = 0; i < topEvidences.size(); i++) {
            topEvidences.get(i).setIndex(i + 1);
        }
        return topEvidences;
    }

    /**
     * 判断当前已准入的证据集合是否达到“证据充分”标准（对应需求文档 4.4 节）
     * <p>
     * 标准：准入证据中，至少 1 条一级来源（相关度 >= 0.5），
     * 或至少 2 条来自不同域名的二级来源（相关度 >= 0.5）。满足即停止升级检索。
     * </p>
     *
     * @param admittedEvidences 已通过白名单准入的证据列表
     * @return true 表示证据充分，无需执行升级检索
     */
    public boolean isEvidenceSufficient(List<EvidenceItemDto> admittedEvidences) {
        if (admittedEvidences == null || admittedEvidences.isEmpty()) {
            return false;
        }
        double threshold = properties.getSearch().getRelevanceThreshold();
        Set<String> qualifiedTier2Domains = new HashSet<>();

        for (EvidenceItemDto item : admittedEvidences) {
            double score = item.getRelevanceScore() != null ? item.getRelevanceScore() : 0.0;
            if (score < threshold) {
                continue;
            }
            if (SourceTier.TIER_1.name().equals(item.getTierCode())) {
                return true;
            }
            if (SourceTier.TIER_2.name().equals(item.getTierCode()) && item.getDomain() != null) {
                qualifiedTier2Domains.add(item.getDomain());
                if (qualifiedTier2Domains.size() >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据配置的白名单对单条来源进行分级判定
     */
    public SourceClassification classifySource(String url, String siteName, String title) {
        String host = extractHost(url);
        if (host.isEmpty()) {
            return new SourceClassification(SourceTier.TIER_0, false, "");
        }
        String domain = extractRegistrableDomain(host);
        String site = siteName == null ? "" : siteName.strip();
        FactCheckProperties.Whitelist wl = properties.getWhitelist();

        // 1. 一级来源判定：域名精确/后缀匹配一级白名单，或非商业官方域名(.gov.cn/.org.cn/.edu.cn/.ac.cn/.int)且站点名命中一级机构关键词
        if (matchesDomainList(host, wl.getTier1Domains())) {
            return new SourceClassification(SourceTier.TIER_1, false, domain);
        }
        boolean isOfficialDomainSuffix = host.endsWith(".gov.cn") || host.endsWith(".org.cn")
                || host.endsWith(".edu.cn") || host.endsWith(".ac.cn") || host.endsWith(".int")
                || host.endsWith(".gov");
        if (isOfficialDomainSuffix && matchesKeywordList(site, wl.getTier1SiteKeywords())) {
            return new SourceClassification(SourceTier.TIER_1, false, domain);
        }

        // 2. 二级来源 - 三甲医院与重点医科大学官网
        if (matchesDomainList(host, wl.getTier2HospitalDomains())
                || (isOfficialDomainSuffix && matchesKeywordList(site, wl.getTier2HospitalSiteKeywords()))) {
            return new SourceClassification(SourceTier.TIER_2, false, domain);
        }

        // 3. 二级来源 - 主流媒体健康频道（须标注“媒体报道”）
        if (matchesDomainList(host, wl.getTier2MediaDomains())
                || matchesKeywordList(site, wl.getTier2MediaSiteKeywords())) {
            return new SourceClassification(SourceTier.TIER_2, true, domain);
        }

        // 4. 二级来源 - 白名单内专业科普机构
        if (matchesDomainList(host, wl.getTier2ScienceDomains())
                || matchesKeywordList(site, wl.getTier2ScienceSiteKeywords())) {
            return new SourceClassification(SourceTier.TIER_2, false, domain);
        }

        // 5. 未命中任何白名单，一律归为零级来源
        return new SourceClassification(SourceTier.TIER_0, false, domain);
    }

    /**
     * 将摘要按完整句子边界截取至指定字数上限以内（对应需求 4.5 节“每条摘要按句截取至 800 字以内”）
     */
    public String truncateBySentence(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").strip();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        String sub = cleaned.substring(0, maxLength);
        int lastSentenceEnd = -1;
        char[] delimiters = {'。', '！', '？', '；', '.', '!', '?', ';'};
        for (char d : delimiters) {
            int idx = sub.lastIndexOf(d);
            if (idx > lastSentenceEnd) {
                lastSentenceEnd = idx;
            }
        }
        // 若在后半段能找到句号等句末标点，则按完整句子截取
        if (lastSentenceEnd >= maxLength / 2) {
            return sub.substring(0, lastSentenceEnd + 1);
        }
        return sub + "…";
    }

    /**
     * 规范化 URL（仅保留 http/https，移除锚点与常见追踪参数，统一去除末尾斜杠）
     */
    public String normalizeUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(rawUrl.strip());
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase(Locale.ROOT) : "";
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return "";
            }
            String host = uri.getHost() != null ? uri.getHost().toLowerCase(Locale.ROOT) : "";
            if (host.isEmpty()) {
                return "";
            }
            String path = uri.getRawPath() != null ? uri.getRawPath() : "";
            if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            String query = filterTrackingQuery(uri.getRawQuery());
            return scheme + "://" + host + path + (query.isEmpty() ? "" : "?" + query);
        } catch (Exception e) {
            return "";
        }
    }

    private static String filterTrackingQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        String[] pairs = rawQuery.split("&");
        List<String> kept = new ArrayList<>();
        for (String pair : pairs) {
            String lower = pair.toLowerCase(Locale.ROOT);
            if (lower.startsWith("utm_") || lower.startsWith("spm=") || lower.startsWith("from=")) {
                continue;
            }
            kept.add(pair);
        }
        return String.join("&", kept);
    }

    private static String extractHost(String url) {
        try {
            URI uri = URI.create(url.strip());
            String host = uri.getHost();
            if (host == null) {
                return "";
            }
            host = host.toLowerCase(Locale.ROOT);
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 提取可注册主域名，用于判断“是否来自不同域名”（兼容 .gov.cn / .com.cn / .edu.cn / .org.cn 等双后缀）
     */
    public static String extractRegistrableDomain(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        String h = host.toLowerCase(Locale.ROOT);
        if (h.startsWith("www.")) {
            h = h.substring(4);
        }
        String[] parts = h.split("\\.");
        if (parts.length <= 2) {
            return h;
        }
        String lastTwo = parts[parts.length - 2] + "." + parts[parts.length - 1];
        Set<String> compoundTlds = Set.of("gov.cn", "com.cn", "org.cn", "edu.cn", "ac.cn", "net.cn");
        if (compoundTlds.contains(lastTwo) && parts.length >= 3) {
            return parts[parts.length - 3] + "." + lastTwo;
        }
        return lastTwo;
    }

    private static boolean matchesDomainList(String host, List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty() || host.isEmpty()) {
            return false;
        }
        for (String item : whitelist) {
            if (item == null || item.isBlank()) {
                continue;
            }
            String d = item.strip().toLowerCase(Locale.ROOT);
            if (d.startsWith("www.")) {
                d = d.substring(4);
            }
            if (host.equals(d) || host.endsWith("." + d)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesKeywordList(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String kw : keywords) {
            if (kw != null && !kw.isBlank() && text.contains(kw.strip())) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        return title.replaceAll("[\\p{P}\\p{S}\\s]+", "").toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.strip();
        }
        return b != null ? b.strip() : "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

