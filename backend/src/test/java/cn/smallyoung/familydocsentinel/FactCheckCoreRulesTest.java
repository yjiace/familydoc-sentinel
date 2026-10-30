package cn.smallyoung.familydocsentinel;

import cn.smallyoung.familydocsentinel.chain.adjudication.*;
import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.*;
import cn.smallyoung.familydocsentinel.enums.*;
import cn.smallyoung.familydocsentinel.service.*;
import com.alibaba.fastjson2.JSONObject;
import com.tencentcloudapi.wsa.v20250508.WsaClient;
import com.tencentcloudapi.wsa.v20250508.models.SearchProRequest;
import com.tencentcloudapi.wsa.v20250508.models.SearchProResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 家医智核·流言粉碎 核心业务规则、检索策略、四项硬校验与责任链裁决全量单元测试
 */
class FactCheckCoreRulesTest {

    private FactCheckProperties properties;
    private ContentSanitizerService sanitizerService;
    private RedFlagService redFlagService;
    private ClaimExtractionService claimExtractionService;
    private EvidenceAdmissionService evidenceAdmissionService;
    private StanceAnnotationService stanceAnnotationService;
    private ClaimAdjudicationEngine adjudicationEngine;
    private ArticleSummaryGenerator summaryGenerator;

    @BeforeEach
    void setUp() {
        properties = new FactCheckProperties();
        properties.getWhitelist().setTier1Domains(List.of(
                "nhc.gov.cn", "chinacdc.cn", "nmpa.gov.cn", "who.int", "wjw.beijing.gov.cn"
        ));
        properties.getWhitelist().setTier1SiteKeywords(List.of("卫生健康委", "卫健委", "疾控中心", "药监局"));
        properties.getWhitelist().setTier2HospitalDomains(List.of("pumch.cn", "wchscu.cn"));
        properties.getWhitelist().setTier2MediaDomains(List.of("people.com.cn", "health.people.com.cn", "xinhuanet.com"));
        properties.getWhitelist().setTier2MediaSiteKeywords(List.of("人民网", "新华网"));
        properties.getWhitelist().setTier2ScienceDomains(List.of("dxy.cn", "guokr.com", "kepuchina.cn", "dayi.org.cn"));
        properties.getWhitelist().setTier2ScienceSiteKeywords(List.of("丁香医生", "果壳", "科普中国", "中国医药信息查询平台"));

        properties.getCompliance().setQueryForbiddenWords(List.of(
                "辟谣", "谣言", "骗局", "假消息", "流言", "真相",
                "卫健委", "疾控中心", "世卫组织", "丁香医生", "人民网", "央视"
        ));
        properties.getCompliance().setExplanationForbiddenReplacements(Map.of(
                "权威机构", "可信来源",
                "权威专家", "查证来源",
                "权威", "可信来源",
                "100%正确", "与证据相符",
                "绝对属实", "与证据相符",
                "名医背书", "查证来源"
        ));

        sanitizerService = new ContentSanitizerService();
        redFlagService = new RedFlagService(properties);
        claimExtractionService = new ClaimExtractionService(null, properties);
        evidenceAdmissionService = new EvidenceAdmissionService(properties);
        stanceAnnotationService = new StanceAnnotationService(null, properties);
        adjudicationEngine = new ClaimAdjudicationEngine(List.of(
                new PersonalMedicationRule(),
                new RuntimeExceptionRetryRule(),
                new EmptyOrInsufficientStanceRule(),
                new ConflictResolutionRule(),
                new RefuteOnlyRule(),
                new ConditionalRule(),
                new SupportOnlyRule()
        ));
        summaryGenerator = new ArticleSummaryGenerator();
    }

    @Test
    @DisplayName("3.1 & 4.3 文本去噪归一化与中性检索词禁用词剥离测试")
    void testSanitizerAndNeutralQueryRules() {
        // 1. 微信震惊体标签与转发引导语清洗 + SHA-256 指纹一致性
        ContentSanitizerService.SanitizedContent s1 = sanitizerService.sanitize(
                "  【紧急通知】喝醋能软化血管治高血压！！！\n\n\n快转给身边的长辈！  "
        );
        ContentSanitizerService.SanitizedContent s2 = sanitizerService.sanitize(
                "喝醋能软化血管治高血压。"
        );
        assertFalse(s1.cleanText().contains("紧急通知"));
        assertFalse(s1.cleanText().contains("快转给身边的长辈"));
        assertEquals(s1.queryHash(), s2.queryHash());

        // 2. 中性检索词强制剥离“辟谣/谣言/骗局”与机构名
        String rawQuery = "卫健委辟谣喝醋软化血管骗局真相";
        String stripped = claimExtractionService.stripForbiddenQueryWords(rawQuery);
        assertFalse(stripped.contains("卫健委"));
        assertFalse(stripped.contains("辟谣"));
        assertFalse(stripped.contains("骗局"));
        assertFalse(stripped.contains("真相"));
        assertTrue(stripped.contains("喝醋软化血管"));
    }

    @Test
    @DisplayName("4.1 & 6 危急红线双层扫描与正反样本集评测（当前症状召回>=95%，科普转述误拦<=10%）")
    void testRedFlagScanRulesAndDatasetEvaluation() {
        // 1. 典型发病求助正样本集（10条：覆盖胸痛、心梗、脑卒中偏瘫、血压>=180/110、低血糖<=2.8、高血糖mg/dL折算等）
        List<String> currentEmergencySamples = List.of(
                "我爸现在突然剧烈胸痛，大汗淋漓，该怎么办？",
                "我刚刚量血压 185/115，头很晕要不要去医院",
                "老伴刚才突然言语不清、单侧肢体无力，要不要送急诊？",
                "我妈正在喘不上气、严重呼吸困难，救命！",
                "爷爷现在空腹血糖 2.4，冒冷汗叫不醒怎么办",
                "爸爸刚刚血压飙到 195，突发爆炸样剧烈头痛，该怎么办",
                "我此刻胸口压榨样胸痛，要送医院吗",
                "家里人突然昏迷不省人事，快救救",
                "妈妈现在突然大口吐血，需要去急诊吗",
                "我刚刚测随机血糖 360（mg/dL折算为20.0mmol/L），现在头晕怎么办"
        );

        int hardRecalled = 0;
        for (String sample : currentEmergencySamples) {
            RedFlagResult res = redFlagService.scan(sample);
            if (res.getLevel() == RedFlagResult.Level.HARD && res.isBlocked()) {
                hardRecalled++;
            }
        }
        double recallRate = (double) hardRecalled / currentEmergencySamples.size();
        assertTrue(recallRate >= 0.95, "当前危急症状召回率须 >= 95%，实测: " + recallRate);

        // 2. 含急症词汇的科普/流言转述负样本集（10条：均不应触发 HARD 阻断，确保可进入核验）
        List<String> articleRumorSamples = List.of(
                "网上说拍打手肘窝能急救心肌梗死，这是真的吗？",
                "突发心梗千万别慌，用力拍打左右胳膊肘内侧就能急救活命！",
                "每天吃大蒜能彻底预防脑出血和急性脑梗吗？",
                "听说一旦发生剧烈胸痛或者昏迷，扎十宣穴放血就能治好？",
                "高血压超过 180 的患者只要坚持喝芹菜汁就能不用吃降压药？",
                "民间偏方说偏瘫和半身不遂只要每天泡脚就能彻底康复？",
                "文章里讲严重呼吸困难时喝浓茶可以扩张支气管，有科学依据吗？",
                "老一辈说遇到全身抽搐不止时掐人中就能立刻苏醒，是真的吗？",
                "我平时血压正常，没有剧烈胸痛，也没有昏迷，吃三七粉能保健吗？",
                "科普文章提到空腹血糖低于 2.8 属于低血糖症，平时该如何预防？"
        );

        int falseBlocked = 0;
        for (String sample : articleRumorSamples) {
            RedFlagResult res = redFlagService.scan(sample);
            if (res.isBlocked()) {
                falseBlocked++;
            }
        }
        double falseBlockRate = (double) falseBlocked / articleRumorSamples.size();
        assertTrue(falseBlockRate <= 0.10, "转述内容误拦截率须 <= 10%，实测: " + falseBlockRate);

        // 3. 验证用户点击“这只是文章里的内容（继续核验）”降级机制
        RedFlagResult downgraded = redFlagService.scan("我爸现在突然剧烈胸痛，大汗淋漓，该怎么办？", true);
        assertEquals(RedFlagResult.Level.WARN, downgraded.getLevel());
        assertFalse(downgraded.isBlocked());
    }

    @Test
    @DisplayName("4.4 元宝搜索渐进式检索策略（主检索充分即止 vs 不充分触发gov升级检索）与调用预算管控测试")
    void testWsaProgressiveSearchAndBudgetControl() throws Exception {
        // 构造模拟 WsaClient：验证主检索充分时仅调用 1 次，主检索不充分时自动触发第 2 次 gov 升级检索
        List<SearchProRequest> capturedRequests = new ArrayList<>();
        WsaClient stubWsaClient = new WsaClient(null, null) {
            @Override
            public SearchProResponse SearchPro(SearchProRequest req) {
                capturedRequests.add(req);
                SearchProResponse resp = new SearchProResponse();
                if ("喝醋软化血管有无科学依据".equals(req.getQuery())) {
                    // 场景 A：主检索直接返回 1 条一级来源（nhc.gov.cn，score=0.85 >= 0.5），满足证据充分性
                    JSONObject page = new JSONObject();
                    page.put("url", "https://www.nhc.gov.cn/kp/202505/vinegar.shtml");
                    page.put("site", "国家卫生健康委员会");
                    page.put("title", "喝醋不能软化血管");
                    page.put("content", "食醋无法直接进入血液软化血管，盲目大量喝醋易伤胃。");
                    page.put("score", 0.85);
                    resp.setPages(new String[]{page.toJSONString()});
                } else {
                    // 场景 B：主检索仅返回 1 条二级来源（不充分），升级检索 Industry="gov" 时返回一级来源
                    if (req.getIndustry() == null) {
                        JSONObject page = new JSONObject();
                        page.put("url", "https://www.kepuchina.cn/article/101");
                        page.put("site", "科普中国");
                        page.put("title", "黑木耳降血压缺乏依据");
                        page.put("content", "单一食物不能替代降压药。");
                        page.put("score", 0.75);
                        resp.setPages(new String[]{page.toJSONString()});
                    } else if ("gov".equals(req.getIndustry())) {
                        JSONObject govPage = new JSONObject();
                        govPage.put("url", "https://wjw.beijing.gov.cn/kp/202504/fungus.html");
                        govPage.put("site", "北京市卫生健康委员会");
                        govPage.put("title", "单靠吃黑木耳不能软化血管降血压");
                        govPage.put("content", "没有任何科学证据表明吃黑木耳可以直接软化血管或降低血压。");
                        govPage.put("score", 0.88);
                        resp.setPages(new String[]{govPage.toJSONString()});
                    }
                }
                return resp;
            }
        };

        WsaSearchService wsaSearchService = new WsaSearchService(stubWsaClient, properties, evidenceAdmissionService);
        AtomicInteger requestBudget = new AtomicInteger(0);

        // 1. 场景 A：主检索已充分 -> 仅调用 1 次，不触发 gov 升级检索
        WsaSearchService.ClaimSearchOutcome outcomeA = wsaSearchService.searchForClaim(
                "喝醋软化血管有无科学依据", requestBudget, new AtomicBoolean(false), null
        );
        assertEquals(1, outcomeA.callsUsed());
        assertFalse(outcomeA.failedOrTimedOut());
        assertEquals(1, outcomeA.admittedEvidences().size());
        assertEquals(SourceTier.TIER_1.name(), outcomeA.admittedEvidences().getFirst().getTierCode());

        // 2. 场景 B：主检索仅 1 条二级来源（不充分） -> 自动触发 Industry="gov" 升级检索，共调用 2 次
        WsaSearchService.ClaimSearchOutcome outcomeB = wsaSearchService.searchForClaim(
                "吃黑木耳软化血管降低血压有无科学依据", requestBudget, new AtomicBoolean(false), null
        );
        assertEquals(2, outcomeB.callsUsed());
        assertEquals("gov", capturedRequests.getLast().getIndustry());
        assertEquals(2, outcomeB.admittedEvidences().size());
        // 升级检索召回的一级来源（0.88 * 1.5 = 1.32）排在二级来源（0.75 * 1.2 = 0.90）之前
        assertEquals(SourceTier.TIER_1.name(), outcomeB.admittedEvidences().getFirst().getTierCode());

        // 两条论断累计消耗 3 次检索，平均每条论断 1.5 次（符合平均 <= 1.5 次的设计标准）
        assertEquals(3, requestBudget.get());
    }

    @Test
    @DisplayName("4.5 证据准入与排序：零级剔除、媒体报道打标、同站同标题去重及一级1.5/二级1.2排序")
    void testEvidenceAdmissionAndRanking() {
        WsaSearchPage tier0SelfMedia = new WsaSearchPage();
        tier0SelfMedia.setUrl("https://blog.some-selfmedia.com/post/1");
        tier0SelfMedia.setSite("养生自媒体");
        tier0SelfMedia.setTitle("喝醋软化血管是真的");
        tier0SelfMedia.setPassage("每天喝醋就能软化血管。");
        tier0SelfMedia.setScore(0.95);

        WsaSearchPage tier2Media = new WsaSearchPage();
        tier2Media.setUrl("http://health.people.com.cn/n1/2025/0501/c14739-12345.html?utm_source=wechat");
        tier2Media.setSite("人民网");
        tier2Media.setTitle("喝醋并不能软化血管");
        tier2Media.setPassage("食醋进入消化道后不会直接进入血液软化血管。");
        tier2Media.setScore(0.80); // rankScore = 0.80 * 1.2 = 0.96

        // 同站同标题重复页面（带不同追踪参数）应被去重
        WsaSearchPage tier2MediaDuplicate = new WsaSearchPage();
        tier2MediaDuplicate.setUrl("http://health.people.com.cn/n1/2025/0502/c14739-99999.html");
        tier2MediaDuplicate.setSite("人民网");
        tier2MediaDuplicate.setTitle("喝醋并不能软化血管！");
        tier2MediaDuplicate.setPassage("重复报道内容。");
        tier2MediaDuplicate.setScore(0.75);

        WsaSearchPage tier1Gov = new WsaSearchPage();
        tier1Gov.setUrl("https://www.nhc.gov.cn/xcs/kpzs/202506/abc.shtml");
        tier1Gov.setSite("国家卫生健康委员会");
        tier1Gov.setTitle("老年人血管健康科普");
        tier1Gov.setPassage("动脉粥样硬化不可通过饮用食醋逆转，盲目大量喝醋反而损伤胃黏膜。");
        tier1Gov.setScore(0.70); // rankScore = 0.70 * 1.5 = 1.05

        List<EvidenceItemDto> admitted = evidenceAdmissionService.admitAndRank(
                List.of(tier0SelfMedia, tier2Media, tier2MediaDuplicate, tier1Gov)
        );

        // 零级自媒体被过滤、同站同标题被去重，仅保留 2 条
        assertEquals(2, admitted.size());
        // 一级来源排序分 1.05 > 二级来源 0.96，排在第 1 位
        assertEquals(SourceTier.TIER_1.name(), admitted.get(0).getTierCode());
        assertEquals("日期未标注", admitted.get(0).getPublishDate());
        // 二级主流媒体标注“媒体报道”，且 URL 中的 utm_source 被清洗
        assertEquals(SourceTier.TIER_2.name(), admitted.get(1).getTierCode());
        assertTrue(admitted.get(1).isMediaReport());
        assertEquals("媒体报道", admitted.get(1).getSourceTag());
        assertFalse(admitted.get(1).getUrl().contains("utm_source"));
    }

    @Test
    @DisplayName("4.6 四项硬校验：逐字引文校验、禁用词替换与数字越界安全退化（Fallback）机制测试")
    void testStanceValidationAndSafeFallback() {
        String summary = "动脉粥样硬化不可通过饮用食醋逆转，每日饮用超过 50 毫升浓醋可能损伤胃黏膜。";
        assertTrue(stanceAnnotationService.isQuoteVerbatimValid("动脉粥样硬化不可通过饮用食醋逆转", summary));
        assertTrue(stanceAnnotationService.isQuoteVerbatimValid("每日饮用超过50毫升浓醋可能损伤胃黏膜", summary));
        // 篡改原文的引文必须校验不通过
        assertFalse(stanceAnnotationService.isQuoteVerbatimValid("喝醋绝对不能软化动脉血管", summary));

        // 数字溯源校验：引文中有 50，说明文字出现 50 通过，出现未提及的 90% 不通过
        assertTrue(stanceAnnotationService.areAllNumbersInQuotes("饮用超过50毫升可能伤胃", "每日饮用超过 50 毫升浓醋"));
        assertFalse(stanceAnnotationService.areAllNumbersInQuotes("90%的人喝超过50毫升会伤胃", "每日饮用超过 50 毫升浓醋"));

        // 当说明文字包含未在引文中出现的虚构数字（如 90%）时，自动退化为仅由有效引文拼接的客观转述，确保 0 越界
        EvidenceItemDto validEv = EvidenceItemDto.builder()
                .sourceOrg("国家卫生健康委员会")
                .quote("每日饮用超过 50 毫升浓醋可能损伤胃黏膜")
                .build();
        String safeExplanation = stanceAnnotationService.sanitizeAndValidateExplanation(
                "权威专家指出，90%的人每天喝超过50毫升浓醋会伤胃，100%正确。",
                validEv.getQuote(),
                List.of(validEv)
        );
        assertNotNull(safeExplanation);
        assertFalse(safeExplanation.contains("90"), "越界数字 90 必须被安全退化机制过滤");
        assertFalse(safeExplanation.contains("权威"), "禁用词必须被过滤");
        assertTrue(safeExplanation.contains("50"), "引文中真实存在的数字 50 正常保留");
    }

    @Test
    @DisplayName("4.7 裁决规则责任链顺序 0~6 全路径验证（含冲突消歧、证据偏弱与药物安全提示）")
    void testAdjudicationChainRules() {
        // 1. 顺序 0：个体用药咨询 -> 问医生
        ClaimItemDto medAdviceClaim = ClaimItemDto.builder()
                .claimId("c-1")
                .claimText("我爸血压150能不能停吃降压药")
                .categoryCode(ClaimCategory.DRUG.getCode())
                .personalMedication(true)
                .build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder().claim(medAdviceClaim).build());
        assertEquals(VerdictType.ASK_DOCTOR.getCode(), medAdviceClaim.getVerdictCode());
        assertEquals("问医生", medAdviceClaim.getColloquialVerdict());
        assertEquals(ClaimItemDto.DRUG_SAFETY_NOTICE, medAdviceClaim.getDrugSafetyNotice());

        // 2. 顺序 1：检索或模型超时异常 -> 待重试（不出印章）
        ClaimItemDto retryClaim = ClaimItemDto.builder().claimId("c-retry").claimText("测试超时论断").build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder()
                .claim(retryClaim)
                .runtimeErrorOrTimeout(true)
                .build());
        assertEquals(VerdictType.RETRY_NEEDED.getCode(), retryClaim.getVerdictCode());
        assertNull(retryClaim.getStamp());
        assertTrue(retryClaim.isRetryable());

        // 3. 顺序 2：四项校验全部作废 -> 没查实·引用未通过校验
        ClaimItemDto invalidQuoteClaim = ClaimItemDto.builder().claimId("c-invalid").claimText("测试校验作废").build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder()
                .claim(invalidQuoteClaim)
                .activeEvidences(List.of())
                .allInvalidatedByValidation(true)
                .build());
        assertEquals(VerdictType.UNVERIFIED.getCode(), invalidQuoteClaim.getVerdictCode());
        assertEquals(UnverifiedCause.CITATION_VALIDATION_FAILED.getCode(), invalidQuoteClaim.getUnverifiedCauseCode());

        // 4. 顺序 3 + 顺序 4：否定与支持同时存在，但仅否定方含一级来源 -> 采信一级来源方判定为“有误导”，另一方作“口径不同”附注
        EvidenceItemDto tier1Refute = EvidenceItemDto.builder()
                .url("https://www.nhc.gov.cn/1")
                .domain("nhc.gov.cn")
                .tierCode(SourceTier.TIER_1.name())
                .stanceCode(StanceType.REFUTE.getCode())
                .quote("他汀类药物不可自行停药")
                .build();
        EvidenceItemDto tier2Support = EvidenceItemDto.builder()
                .url("https://www.dxy.cn/2")
                .domain("dxy.cn")
                .tierCode(SourceTier.TIER_2.name())
                .stanceCode(StanceType.SUPPORT.getCode())
                .quote("转氨酶升高可暂停")
                .build();

        ClaimItemDto statinClaim = ClaimItemDto.builder()
                .claimId("c-2")
                .claimText("吃他汀伤肝必须马上停药")
                .categoryCode(ClaimCategory.DRUG.getCode())
                .personalMedication(false)
                .build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder()
                .claim(statinClaim)
                .activeEvidences(List.of(tier1Refute, tier2Support))
                .validatedExplanation("查证可信来源指出，他汀类药物不可自行停药。")
                .build());

        assertEquals(VerdictType.MISLEADING.getCode(), statinClaim.getVerdictCode());
        assertEquals("有误导", statinClaim.getColloquialVerdict());
        assertEquals(1, statinClaim.getOpposingEvidences().size());
        assertEquals("口径不同", statinClaim.getOpposingEvidences().getFirst().getNote());
        assertEquals(ClaimItemDto.DRUG_SAFETY_NOTICE, statinClaim.getDrugSafetyNotice());

        // 5. 顺序 4（未达定论强度）：仅有 1 条二级来源否定 -> 没查实·证据偏弱
        EvidenceItemDto singleTier2Refute = EvidenceItemDto.builder()
                .url("https://www.dxy.cn/only1")
                .domain("dxy.cn")
                .tierCode(SourceTier.TIER_2.name())
                .stanceCode(StanceType.REFUTE.getCode())
                .quote("单一来源否定")
                .build();
        ClaimItemDto weakClaim = ClaimItemDto.builder().claimId("c-weak").claimText("偏门养生说法").build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder()
                .claim(weakClaim)
                .activeEvidences(List.of(singleTier2Refute))
                .build());
        assertEquals(VerdictType.UNVERIFIED.getCode(), weakClaim.getVerdictCode());
        assertEquals(UnverifiedCause.WEAK_EVIDENCE.getCode(), weakClaim.getUnverifiedCauseCode());

        // 6. 顺序 6：支持与缺乏证据并存 -> 仍按第 6 条判“有依据”，附注“另有来源称研究不足”，置信度强制为“中”
        EvidenceItemDto tier1Support = EvidenceItemDto.builder()
                .url("https://www.nhc.gov.cn/s1")
                .domain("nhc.gov.cn")
                .tierCode(SourceTier.TIER_1.name())
                .stanceCode(StanceType.SUPPORT.getCode())
                .quote("适度补充维生素D有助于骨骼健康")
                .build();
        EvidenceItemDto tier2Support2 = EvidenceItemDto.builder()
                .url("https://www.pumch.cn/s2")
                .domain("pumch.cn")
                .tierCode(SourceTier.TIER_2.name())
                .stanceCode(StanceType.SUPPORT.getCode())
                .quote("维生素D促进钙吸收")
                .build();
        EvidenceItemDto tier2Insufficient = EvidenceItemDto.builder()
                .url("https://www.guokr.com/s3")
                .domain("guokr.com")
                .tierCode(SourceTier.TIER_2.name())
                .stanceCode(StanceType.INSUFFICIENT.getCode())
                .quote("部分人群额外补充的远期获益研究不足")
                .build();

        ClaimItemDto vdClaim = ClaimItemDto.builder()
                .claimId("c-3")
                .claimText("补充维生素D促进钙吸收")
                .categoryCode(ClaimCategory.LIFESTYLE.getCode())
                .build();
        adjudicationEngine.adjudicate(ClaimAdjudicationContext.builder()
                .claim(vdClaim)
                .activeEvidences(List.of(tier1Support, tier2Support2, tier2Insufficient))
                .build());

        assertEquals(VerdictType.SUPPORTED.getCode(), vdClaim.getVerdictCode());
        assertEquals(ConfidenceLevel.MEDIUM.getCode(), vdClaim.getConfidenceCode());
        assertTrue(vdClaim.getExtraNote().contains("另有来源称研究不足"));
    }

    @Test
    @DisplayName("4.8 文章级摘要规则模板测试")
    void testArticleSummaryGenerator() {
        ClaimItemDto c1 = ClaimItemDto.builder().verdictCode(VerdictType.MISLEADING.getCode()).build();
        ClaimItemDto c2 = ClaimItemDto.builder().verdictCode(VerdictType.SUPPORTED.getCode()).build();
        ClaimItemDto c3 = ClaimItemDto.builder().verdictCode(VerdictType.ASK_DOCTOR.getCode()).build();
        ClaimItemDto c4 = ClaimItemDto.builder().verdictCode(VerdictType.RETRY_NEEDED.getCode()).build();
        UnverifiedItemDto u1 = UnverifiedItemDto.of("某某牌神药包治百病", UnverifiedReason.NAMED_PRODUCT_PROMOTION);

        String summary = summaryGenerator.generateSummary(List.of(c1, c2, c3, c4), List.of(u1));
        assertTrue(summary.contains("共核验 4 条说法：1 条有误导，1 条有依据"));
        assertFalse(summary.contains("0 条"));
        assertTrue(summary.contains("另有 1 条未纳入核验"));
        assertTrue(summary.contains("1 条暂时没查成，可点击重试"));
        assertTrue(summary.contains("文中含与依据矛盾的说法，转发前请留意"));
        assertTrue(summary.contains("文中有涉及个人用药的问题，请咨询医生或药师"));
    }

    @Test
    @DisplayName("双链额度（Token & 元宝搜索 · 每日/每周/每月）短板熔断与跨期重置测试")
    void testUserDualChainQuotaDailyWeeklyMonthly() throws Exception {
        UsageTrackingService usageService = new UsageTrackingService();
        cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity stat =
                cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity.builder()
                        .id(1L)
                        .userId(100L)
                        .dailyPeriodDate(java.time.LocalDate.now())
                        .weeklyPeriodDate(java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)))
                        .monthlyPeriodDate(java.time.LocalDate.now().with(java.time.temporal.TemporalAdjusters.firstDayOfMonth()))
                        .dailyTokens(1000L)
                        .weeklyTokens(5000L)
                        .monthlyTokens(10000L)
                        .dailySearchCount(5L)
                        .weeklySearchCount(20L)
                        .monthlySearchCount(50L)
                        .build();

        cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository mockStatRepo =
                (cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository) java.lang.reflect.Proxy.newProxyInstance(
                        cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository.class.getClassLoader(),
                        new Class[]{cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository.class},
                        (proxy, method, args) -> {
                            if ("findByUserId".equals(method.getName())) {
                                return java.util.Optional.of(stat);
                            }
                            if ("save".equals(method.getName())) {
                                return args[0];
                            }
                            return null;
                        }
                );

        java.lang.reflect.Field repoField = UsageTrackingService.class.getDeclaredField("userUsageStatRepository");
        repoField.setAccessible(true);
        repoField.set(usageService, mockStatRepo);

        java.lang.reflect.Field propField = UsageTrackingService.class.getDeclaredField("factCheckProperties");
        propField.setAccessible(true);
        propField.set(usageService, properties);

        // 1. 额度充足时正常通过
        assertDoesNotThrow(() -> usageService.checkUserQuotaAvailable(100L));

        // 2. 每日 Token 额度耗尽 -> 停止服务
        stat.setDailyTokens(50000L);
        cn.smallyoung.familydocsentinel.exception.BizException ex1 = assertThrows(
                cn.smallyoung.familydocsentinel.exception.BizException.class,
                () -> usageService.checkUserQuotaAvailable(100L)
        );
        assertTrue(ex1.getMessage().contains("今日 Token 额度已用完"));

        // 3. 每日元宝搜索额度耗尽 -> 停止服务
        stat.setDailyTokens(1000L);
        stat.setDailySearchCount(30L);
        cn.smallyoung.familydocsentinel.exception.BizException ex2 = assertThrows(
                cn.smallyoung.familydocsentinel.exception.BizException.class,
                () -> usageService.checkUserQuotaAvailable(100L)
        );
        assertTrue(ex2.getMessage().contains("今日元宝搜索次数额度已用完"));

        // 4. 每周元宝搜索额度耗尽 -> 停止服务
        stat.setDailySearchCount(5L);
        stat.setWeeklySearchCount(120L);
        cn.smallyoung.familydocsentinel.exception.BizException ex3 = assertThrows(
                cn.smallyoung.familydocsentinel.exception.BizException.class,
                () -> usageService.checkUserQuotaAvailable(100L)
        );
        assertTrue(ex3.getMessage().contains("本周元宝搜索次数额度已用完"));

        // 5. 每月 Token 额度耗尽 -> 停止服务
        stat.setWeeklySearchCount(20L);
        stat.setMonthlyTokens(500000L);
        cn.smallyoung.familydocsentinel.exception.BizException ex4 = assertThrows(
                cn.smallyoung.familydocsentinel.exception.BizException.class,
                () -> usageService.checkUserQuotaAvailable(100L)
        );
        assertTrue(ex4.getMessage().contains("本月 Token 额度已用完"));

        // 6. 跨日自动重置：昨日满额但在新自然日访问时自动归零恢复服务
        stat.setMonthlyTokens(10000L);
        stat.setDailySearchCount(30L);
        stat.setDailyPeriodDate(java.time.LocalDate.now().minusDays(1));
        assertDoesNotThrow(() -> usageService.checkUserQuotaAvailable(100L));
        assertEquals(0L, stat.getDailySearchCount());
    }
}
