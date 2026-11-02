package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.dto.WsaSearchPage;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import cn.smallyoung.familydocsentinel.exception.BizException;
import com.alibaba.fastjson2.JSONObject;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.wsa.v20250508.WsaClient;
import com.tencentcloudapi.wsa.v20250508.models.SearchProRequest;
import com.tencentcloudapi.wsa.v20250508.models.SearchProResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 元宝搜索（WSA）检索策略与调用预算管控服务（对应需求文档 4.4、4.10、5 节）
 * <p>
 * 核心升级：
 * 1. 自适应 Freshness：时效敏感词先以近半年（m6）探查最新指南，证据不足自适应回退全时段，兼顾时效与长效权威；
 * 2. 精准 Site 定向：政务升级检索阶段按论断领域靶向定向国家药监局(nmpa.gov.cn)、中国疾控(chinacdc.cn)或国家卫健委(nhc.gov.cn)；
 * 3. 结构化 Deeplinks：开启富媒体子链检索，支持权威词条 VR 实体提权；
 * 4. 预算与双链熔断保持严格约束：单论断 <=3 次，单任务 <=12 次。
 * </p>
 */
@Slf4j
@Service
public class WsaSearchService {

    /**
     * 混合检索模式常量（0：自然检索，1：VR，2：混合模式）
     */
    private static final long MODE_HYBRID = 2L;

    /**
     * 每次检索返回条数常量（20 条）
     */
    private static final long CNT_DEFAULT = 20L;

    /**
     * 政府类行业过滤参数
     */
    private static final String INDUSTRY_GOV = "gov";

    /**
     * 学术类行业过滤参数
     */
    private static final String INDUSTRY_ACAD = "acad";

    private final WsaClient wsaClient;
    private final FactCheckProperties properties;
    private final EvidenceAdmissionService evidenceAdmissionService;
    private final UsageTrackingService usageTrackingService;

    @org.springframework.beans.factory.annotation.Autowired
    public WsaSearchService(WsaClient wsaClient,
                            FactCheckProperties properties,
                            EvidenceAdmissionService evidenceAdmissionService,
                            @org.springframework.beans.factory.annotation.Autowired(required = false) UsageTrackingService usageTrackingService) {
        this.wsaClient = wsaClient;
        this.properties = properties;
        this.evidenceAdmissionService = evidenceAdmissionService;
        this.usageTrackingService = usageTrackingService;
    }

    public WsaSearchService(WsaClient wsaClient,
                            FactCheckProperties properties,
                            EvidenceAdmissionService evidenceAdmissionService) {
        this(wsaClient, properties, evidenceAdmissionService, null);
    }

    private volatile LocalDate quotaDate = LocalDate.now();
    private final AtomicInteger dailyUsedCount = new AtomicInteger(0);
    private final AtomicBoolean dailyWarnLogged = new AtomicBoolean(false);

    public record ClaimSearchOutcome(
            List<EvidenceItemDto> admittedEvidences,
            int callsUsed,
            boolean failedOrTimedOut,
            String errorMessage
    ) {
    }

    /**
     * 为单条论断执行完整自适应检索流程（时效探测/主检索 -> 充分性判定 -> 精准 Site/政务升级检索）
     */
    public ClaimSearchOutcome searchForClaim(
            String neutralQuery,
            AtomicInteger requestBudgetUsed,
            AtomicBoolean cancelled,
            Consumer<ClaimVerifyStage> stageStatusCallback) {

        int maxPerClaim = properties.getSearch().getMaxPerClaim();
        AtomicInteger claimCallsUsed = new AtomicInteger(0);
        AtomicBoolean retryUsed = new AtomicBoolean(false);
        List<WsaSearchPage> accumulatedPages = new ArrayList<>();

        if (cancelled != null && cancelled.get()) {
            return new ClaimSearchOutcome(Collections.emptyList(), 0, true, "任务已取消");
        }
        notifyStage(stageStatusCallback, ClaimVerifyStage.PRIMARY_SEARCHING);

        // 1. 阶段一：时效敏感判断与自适应检索
        boolean freshnessSensitive = isFreshnessSensitive(neutralQuery);
        String freshnessParam = freshnessSensitive ? "m6" : null;

        SingleCallResult primaryResult = executeWithRetryIfEligible(
                neutralQuery, null, null, freshnessParam, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
        );
        if (!primaryResult.success()) {
            return new ClaimSearchOutcome(
                    Collections.emptyList(),
                    claimCallsUsed.get(),
                    true,
                    primaryResult.errorMessage()
            );
        }
        accumulatedPages.addAll(primaryResult.pages());

        // 2. 阶段二：证据准入与充分性评估
        notifyStage(stageStatusCallback, ClaimVerifyStage.EVIDENCE_ADMITTING);
        List<EvidenceItemDto> admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
        if (evidenceAdmissionService.isEvidenceSufficient(admitted)) {
            return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), false, null);
        }

        // 若时效探测（m6）结果证据不足且仍有预算，自动透明回退（Fallback）至全时段检索获取长效权威指南
        if (freshnessSensitive && canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            log.info("Freshness probe returned insufficient evidence, fallback to all-time search: query={}", neutralQuery);
            SingleCallResult fallbackResult = executeWithRetryIfEligible(
                    neutralQuery, null, null, null, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
            );
            if (fallbackResult.success() && !fallbackResult.pages().isEmpty()) {
                accumulatedPages.addAll(fallbackResult.pages());
                admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
                if (evidenceAdmissionService.isEvidenceSufficient(admitted)) {
                    return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), false, null);
                }
            }
        }

        // 3. 阶段三：主检索证据不充分，执行精准 Site 定向与政务升级检索
        if (cancelled != null && cancelled.get()) {
            return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), true, "任务已取消");
        }
        if (canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            notifyStage(stageStatusCallback, ClaimVerifyStage.UPGRADE_SEARCHING);
            String targetSite = resolveTargetSite(neutralQuery);

            // 先执行带精准官方 Site 定向的政务升级检索
            SingleCallResult siteGovResult = executeWithRetryIfEligible(
                    neutralQuery, INDUSTRY_GOV, targetSite, null, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
            );
            if (siteGovResult.success() && !siteGovResult.pages().isEmpty()) {
                accumulatedPages.addAll(siteGovResult.pages());
                notifyStage(stageStatusCallback, ClaimVerifyStage.EVIDENCE_ADMITTING);
                admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
            } else if (canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
                // 若垂直定向站点无结果，平滑回退至通用政务检索（Industry=gov）
                SingleCallResult generalGovResult = executeWithRetryIfEligible(
                        neutralQuery, INDUSTRY_GOV, null, null, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
                );
                if (generalGovResult.success() && !generalGovResult.pages().isEmpty()) {
                    accumulatedPages.addAll(generalGovResult.pages());
                    notifyStage(stageStatusCallback, ClaimVerifyStage.EVIDENCE_ADMITTING);
                    admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
                }
            }
        }

        // 4. 可选阶段四：学术类行业检索（默认关闭）
        if (properties.getSearch().isEnableAcademic()
                && !evidenceAdmissionService.isEvidenceSufficient(admitted)
                && canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            SingleCallResult acadResult = executeWithRetryIfEligible(
                    neutralQuery, INDUSTRY_ACAD, null, null, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
            );
            if (acadResult.success() && !acadResult.pages().isEmpty()) {
                accumulatedPages.addAll(acadResult.pages());
                admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
            }
        }

        return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), false, null);
    }

    /**
     * 判断中性检索词是否具有明显的时效敏感性（如当季病毒、变种、最新指南）
     */
    public boolean isFreshnessSensitive(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        String q = query.toLowerCase();
        return q.contains("最新") || q.contains("近期") || q.contains("当季") || q.contains("变种")
                || q.contains("变异株") || q.contains("流行") || q.contains("新药") || q.contains("新型") || q.contains("上市") || q.contains("2024")
                || q.contains("2025") || q.contains("2026") || q.contains("支原体") || q.contains("登革热")
                || q.contains("甲流") || q.contains("乙流") || q.contains("诺如");
    }

    /**
     * 根据中性检索词智能判定政务精准定向站点（国家级垂直监管与疾控域名）
     */
    public String resolveTargetSite(String query) {
        if (query == null || query.isBlank()) {
            return "nhc.gov.cn";
        }
        String q = query.toLowerCase();
        if (q.contains("药") || q.contains("他汀") || q.contains("阿司匹林") || q.contains("胶囊")
                || q.contains("片") || q.contains("剂量") || q.contains("停药") || q.contains("换药")
                || q.contains("副作用") || q.contains("降压") || q.contains("胰岛素") || q.contains("保健品")) {
            return "nmpa.gov.cn";
        }
        if (q.contains("疾控") || q.contains("疫苗") || q.contains("传染") || q.contains("流感")
                || q.contains("病毒") || q.contains("感染") || q.contains("支原体") || q.contains("登革热")
                || q.contains("肺炎") || q.contains("新冠")) {
            return "chinacdc.cn";
        }
        return "nhc.gov.cn";
    }

    public void checkDailyQuotaAvailable() {
        refreshQuotaDateIfNeeded();
        int dailyQuota = properties.getSearch().getDailyQuota();
        if (dailyUsedCount.get() >= dailyQuota) {
            log.error("[QuotaAlert] 今日全局检索额度已耗尽: used={}, limit={}", dailyUsedCount.get(), dailyQuota);
            throw BizException.of("今日核验额度已用完，请明天再试");
        }
    }

    private record SingleCallResult(boolean success, List<WsaSearchPage> pages, String errorMessage) {
    }

    private SingleCallResult executeWithRetryIfEligible(
            String query,
            String industry,
            String site,
            String freshness,
            AtomicInteger claimCallsUsed,
            AtomicInteger requestBudgetUsed,
            AtomicBoolean retryUsed,
            int maxPerClaim) {

        SingleCallResult firstAttempt = doSingleSearchCall(
                query, industry, site, freshness, claimCallsUsed, requestBudgetUsed, maxPerClaim
        );
        if (firstAttempt.success()) {
            return firstAttempt;
        }

        if (isRetryableError(firstAttempt.errorMessage())
                && retryUsed.compareAndSet(false, true)
                && canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            log.warn("WSA search transient failure, retrying once: query={}, industry={}, site={}, reason={}",
                    query, industry, site, firstAttempt.errorMessage());
            return doSingleSearchCall(
                    query, industry, site, freshness, claimCallsUsed, requestBudgetUsed, maxPerClaim
            );
        }

        return firstAttempt;
    }

    private SingleCallResult doSingleSearchCall(
            String query,
            String industry,
            String site,
            String freshness,
            AtomicInteger claimCallsUsed,
            AtomicInteger requestBudgetUsed,
            int maxPerClaim) {

        Long currentUserId = cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.getUserId();
        if (currentUserId == null) {
            currentUserId = cn.smallyoung.familydocsentinel.security.UserContext.getUserId();
        }
        if (usageTrackingService != null && currentUserId != null) {
            try {
                usageTrackingService.checkUserQuotaAvailable(currentUserId);
            } catch (BizException quotaEx) {
                return new SingleCallResult(false, Collections.emptyList(), quotaEx.getMessage());
            }
        }

        int currentClaimCalls = claimCallsUsed.incrementAndGet();
        if (currentClaimCalls > maxPerClaim) {
            claimCallsUsed.decrementAndGet();
            return new SingleCallResult(false, Collections.emptyList(), "超过单论断检索次数上限");
        }

        int maxPerRequest = properties.getSearch().getMaxPerRequest();
        int currentReqCalls = requestBudgetUsed.incrementAndGet();
        if (currentReqCalls > maxPerRequest) {
            requestBudgetUsed.decrementAndGet();
            claimCallsUsed.decrementAndGet();
            log.warn("Request search budget reached limit ({}): skipping query={}", maxPerRequest, query);
            return new SingleCallResult(false, Collections.emptyList(), "超过单次请求总检索预算上限");
        }

        if (!tryAcquireDailyQuota()) {
            requestBudgetUsed.decrementAndGet();
            claimCallsUsed.decrementAndGet();
            return new SingleCallResult(false, Collections.emptyList(), "今日核验额度已用完，请明天再试");
        }

        SearchProRequest req = new SearchProRequest();
        req.setQuery(query);
        req.setMode(MODE_HYBRID);
        req.setCnt(CNT_DEFAULT);
        req.setDeeplinks(Boolean.TRUE); // 开启富媒体子链检索，用于 VR 实体提权
        if (industry != null && !industry.isBlank()) {
            req.setIndustry(industry);
        }
        if (site != null && !site.isBlank()) {
            req.setSite(site.strip());
        }
        if (freshness != null && !freshness.isBlank()) {
            req.setFreshness(freshness.strip());
        }

        long searchStartTime = System.currentTimeMillis();
        try {
            SearchProResponse res = wsaClient.SearchPro(req);
            if (res == null || res.getPages() == null || res.getPages().length == 0) {
                return new SingleCallResult(true, Collections.emptyList(), null);
            }
            List<WsaSearchPage> pages = new ArrayList<>(res.getPages().length);
            for (String pageJson : res.getPages()) {
                if (pageJson == null || pageJson.isBlank()) {
                    continue;
                }
                try {
                    WsaSearchPage page = JSONObject.parseObject(pageJson, WsaSearchPage.class);
                    if (page != null) {
                        if (site != null && !site.isBlank()) {
                            page.setMatchedSite(site.strip());
                        }
                        pages.add(page);
                    }
                } catch (Exception parseEx) {
                    log.debug("Failed to parse single WsaSearchPage item", parseEx);
                }
            }
            return new SingleCallResult(true, pages, null);
        } catch (TencentCloudSDKException e) {
            String errCode = e.getErrorCode() != null ? e.getErrorCode() : "";
            String errMsg = e.getMessage() != null ? e.getMessage() : "";
            if (errCode.contains("AuthFailure") || errCode.contains("UnauthorizedOperation")) {
                log.error("[WsaAuthAlert] 检索服务鉴权失败，请检查密钥配置! errorCode={}, requestId={}",
                        errCode, e.getRequestId());
                return new SingleCallResult(false, Collections.emptyList(), "检索服务鉴权失败，请检查密钥配置");
            }
            log.warn("WSA SearchPro SDK exception: industry={}, site={}, code={}, msg={}", industry, site, errCode, errMsg);
            return new SingleCallResult(false, Collections.emptyList(), "SDK_ERR:" + errCode + ":" + errMsg);
        } catch (Exception e) {
            log.warn("WSA SearchPro unexpected error: industry={}, site={}, msg={}", industry, site, e.getMessage());
            return new SingleCallResult(false, Collections.emptyList(), "TIMEOUT_OR_NET:" + e.getMessage());
        } finally {
            long cost = System.currentTimeMillis() - searchStartTime;
            if (usageTrackingService != null) {
                usageTrackingService.recordSearchUsage(
                        currentUserId,
                        cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.getTaskId(),
                        1,
                        cost
                );
            }
        }
    }

    private boolean tryAcquireDailyQuota() {
        refreshQuotaDateIfNeeded();
        int limit = properties.getSearch().getDailyQuota();
        int used = dailyUsedCount.incrementAndGet();
        if (used > limit) {
            dailyUsedCount.decrementAndGet();
            log.error("[QuotaAlert] 今日全局检索额度已耗尽: used={}, limit={}", used - 1, limit);
            return false;
        }
        if (used >= (int) (limit * 0.8) && dailyWarnLogged.compareAndSet(false, true)) {
            log.warn("[QuotaAlert] 今日检索调用已达 80% 警戒线: used={}, limit={}", used, limit);
        }
        return true;
    }

    private void refreshQuotaDateIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(quotaDate)) {
            synchronized (this) {
                if (!today.equals(quotaDate)) {
                    quotaDate = today;
                    dailyUsedCount.set(0);
                    dailyWarnLogged.set(false);
                    log.info("WSA search daily quota counter refreshed for date={}", today);
                }
            }
        }
    }

    private boolean canInvokeNextSearch(int claimCalls, int maxPerClaim, int requestBudgetUsed) {
        return (claimCalls < maxPerClaim) && (requestBudgetUsed < properties.getSearch().getMaxPerRequest());
    }

    private boolean isRetryableError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return false;
        }
        String lower = errorMessage.toLowerCase();
        return lower.contains("timeout") || lower.contains("500") || lower.contains("502")
                || lower.contains("503") || lower.contains("504") || lower.contains("network")
                || lower.contains("connect");
    }

    private void notifyStage(Consumer<ClaimVerifyStage> callback, ClaimVerifyStage stage) {
        if (callback != null && stage != null) {
            try {
                callback.accept(stage);
            } catch (Exception ex) {
                log.debug("Stage status callback exception ignored", ex);
            }
        }
    }
}
