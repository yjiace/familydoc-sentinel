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
import lombok.RequiredArgsConstructor;
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
 * 核心职责：
 * 1. 主检索：1 次，混合模式（Mode=2），取 20 条（Cnt=20），使用中性检索词，不传时效参数；
 * 2. 升级检索：主检索后准入证据不充分时，限定政府类行业（Industry="gov"）升级检索 1 次；
 * 3. 超时/5xx 重试：最多重试 1 次，且占用调用上限；
 * 4. 预算硬上限：单论断最多 3 次（含重试），单次请求总检索最多 12 次；
 * 5. 全局日度额度监控：达 80% 记录运维告警，耗尽时阻断外部调用并提示“今日核验额度已用完，请明天再试”；
 * 6. 鉴权失败保护：捕获 AuthFailure 错误码，不暴露密钥，记录告警并返回“核查服务暂时受限，请稍后再试”。
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

    /**
     * 当前统计自然日
     */
    private volatile LocalDate quotaDate = LocalDate.now();

    /**
     * 当日累计已调用检索次数
     */
    private final AtomicInteger dailyUsedCount = new AtomicInteger(0);

    /**
     * 当日 80% 额度告警是否已触发标记
     */
    private final AtomicBoolean dailyWarnLogged = new AtomicBoolean(false);

    /**
     * 单条论断检索执行汇总结果
     *
     * @param admittedEvidences 经过白名单准入、去重与排序后的前 4 条证据
     * @param callsUsed         本条论断实际消耗的检索次数
     * @param failedOrTimedOut  是否因检索超时、限流或服务异常导致检索未能正常完成
     * @param errorMessage      异常或受限提示信息
     */
    public record ClaimSearchOutcome(
            List<EvidenceItemDto> admittedEvidences,
            int callsUsed,
            boolean failedOrTimedOut,
            String errorMessage
    ) {
    }

    /**
     * 为单条论断执行完整检索流程（主检索 -> 充分性判定 -> 政务升级检索），并实时回调检索子阶段
     *
     * @param neutralQuery        中性检索词
     * @param requestBudgetUsed   本次请求级已用检索次数计数器（上限 12 次）
     * @param cancelled           任务取消标志
     * @param stageStatusCallback 实时检索阶段更新回调
     * @return 检索与证据准入汇总结果
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

        // 1. 阶段一：执行混合模式主检索
        if (cancelled != null && cancelled.get()) {
            return new ClaimSearchOutcome(Collections.emptyList(), 0, true, "任务已取消");
        }
        notifyStage(stageStatusCallback, ClaimVerifyStage.PRIMARY_SEARCHING);

        SingleCallResult primaryResult = executeWithRetryIfEligible(
                neutralQuery, null, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
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

        // 3. 阶段三：主检索证据不充分，若预算允许则执行 1 次限定政府类行业（gov）的升级检索
        if (cancelled != null && cancelled.get()) {
            return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), true, "任务已取消");
        }
        if (canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            notifyStage(stageStatusCallback, ClaimVerifyStage.UPGRADE_SEARCHING);
            SingleCallResult govResult = executeWithRetryIfEligible(
                    neutralQuery, INDUSTRY_GOV, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
            );
            if (govResult.success() && !govResult.pages().isEmpty()) {
                accumulatedPages.addAll(govResult.pages());
                notifyStage(stageStatusCallback, ClaimVerifyStage.EVIDENCE_ADMITTING);
                admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
            } else if (!govResult.success() && admitted.isEmpty()) {
                // 若主检索无准入证据且升级检索发生超时/限流异常，则标记为待重试
                return new ClaimSearchOutcome(
                        Collections.emptyList(),
                        claimCallsUsed.get(),
                        true,
                        govResult.errorMessage()
                );
            }
        }

        // 4. 可选阶段四：若配置显式开启了学术类行业检索（默认关闭）且证据仍不充分且预算允许
        if (properties.getSearch().isEnableAcademic()
                && !evidenceAdmissionService.isEvidenceSufficient(admitted)
                && canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            SingleCallResult acadResult = executeWithRetryIfEligible(
                    neutralQuery, INDUSTRY_ACAD, claimCallsUsed, requestBudgetUsed, retryUsed, maxPerClaim
            );
            if (acadResult.success() && !acadResult.pages().isEmpty()) {
                accumulatedPages.addAll(acadResult.pages());
                admitted = evidenceAdmissionService.admitAndRank(accumulatedPages);
            }
        }

        return new ClaimSearchOutcome(admitted, claimCallsUsed.get(), false, null);
    }

    /**
     * 检查每日全局检索额度是否已耗尽（供任务前置检查使用）
     */
    public void checkDailyQuotaAvailable() {
        refreshQuotaDateIfNeeded();
        int dailyQuota = properties.getSearch().getDailyQuota();
        if (dailyUsedCount.get() >= dailyQuota) {
            log.error("[QuotaAlert] 今日全局检索额度已耗尽: used={}, limit={}", dailyUsedCount.get(), dailyQuota);
            throw BizException.of("今日核验额度已用完，请明天再试");
        }
    }

    // ===================== 内部调用与重试封装 =====================

    private record SingleCallResult(boolean success, List<WsaSearchPage> pages, String errorMessage) {
    }

    /**
     * 执行单次检索调用；仅当超时或服务端 5xx 且尚未重试过、同时未超预算上限时重试最多 1 次
     */
    private SingleCallResult executeWithRetryIfEligible(
            String query,
            String industry,
            AtomicInteger claimCallsUsed,
            AtomicInteger requestBudgetUsed,
            AtomicBoolean retryUsed,
            int maxPerClaim) {

        SingleCallResult firstAttempt = doSingleSearchCall(query, industry, claimCallsUsed, requestBudgetUsed, maxPerClaim);
        if (firstAttempt.success()) {
            return firstAttempt;
        }

        // 仅超时或 5xx 服务端异常且未重试过、且预算允许时重试 1 次（需求 4.4）
        if (isRetryableError(firstAttempt.errorMessage())
                && retryUsed.compareAndSet(false, true)
                && canInvokeNextSearch(claimCallsUsed.get(), maxPerClaim, requestBudgetUsed.get())) {
            log.warn("WSA search transient failure, retrying once: query={}, industry={}, reason={}",
                    query, industry, firstAttempt.errorMessage());
            return doSingleSearchCall(query, industry, claimCallsUsed, requestBudgetUsed, maxPerClaim);
        }

        return firstAttempt;
    }

    private SingleCallResult doSingleSearchCall(
            String query,
            String industry,
            AtomicInteger claimCallsUsed,
            AtomicInteger requestBudgetUsed,
            int maxPerClaim) {

        // 1. 检查单论断预算上限（<=3）
        int currentClaimCalls = claimCallsUsed.incrementAndGet();
        if (currentClaimCalls > maxPerClaim) {
            claimCallsUsed.decrementAndGet();
            return new SingleCallResult(false, Collections.emptyList(), "超过单论断检索次数上限");
        }

        // 2. 检查单次请求总预算上限（<=12）
        int maxPerRequest = properties.getSearch().getMaxPerRequest();
        int currentReqCalls = requestBudgetUsed.incrementAndGet();
        if (currentReqCalls > maxPerRequest) {
            requestBudgetUsed.decrementAndGet();
            claimCallsUsed.decrementAndGet();
            log.warn("Request search budget reached limit ({}): skipping query={}", maxPerRequest, query);
            return new SingleCallResult(false, Collections.emptyList(), "超过单次请求总检索预算上限");
        }

        // 3. 检查并扣减每日全局额度（达 80% 告警，达 100% 阻断）
        if (!tryAcquireDailyQuota()) {
            requestBudgetUsed.decrementAndGet();
            claimCallsUsed.decrementAndGet();
            return new SingleCallResult(false, Collections.emptyList(), "今日核验额度已用完，请明天再试");
        }

        // 4. 组装腾讯云 WSA SearchPro 请求（时效参数 Freshness 默认不传）
        SearchProRequest req = new SearchProRequest();
        req.setQuery(query);
        req.setMode(MODE_HYBRID);
        req.setCnt(CNT_DEFAULT);
        if (industry != null && !industry.isBlank()) {
            req.setIndustry(industry);
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
            // 需求第 5 节：检索服务鉴权失败 -> “核查服务暂时受限，请稍后再试”，不暴露密钥，记录告警
            if (errCode.contains("AuthFailure") || errCode.contains("UnauthorizedOperation")) {
                log.error("[WsaAuthAlert] 检索服务鉴权失败，请检查密钥配置! errorCode={}, requestId={}",
                        errCode, e.getRequestId());
                return new SingleCallResult(false, Collections.emptyList(), "检索服务鉴权失败，请检查密钥配置");
            }
            log.warn("WSA SearchPro SDK exception: industry={}, code={}, msg={}", industry, errCode, errMsg);
            return new SingleCallResult(false, Collections.emptyList(), "SDK_ERR:" + errCode + ":" + errMsg);
        } catch (Exception e) {
            log.warn("WSA SearchPro unexpected error: industry={}, msg={}", industry, e.getMessage());
            return new SingleCallResult(false, Collections.emptyList(), "TIMEOUT_OR_NET:" + e.getMessage());
        } finally {
            long cost = System.currentTimeMillis() - searchStartTime;
            if (usageTrackingService != null) {
                usageTrackingService.recordSearchUsage(
                        cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.getUserId(),
                        cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.getTaskId(),
                        1,
                        cost
                );
            }
        }
    }

    private boolean canInvokeNextSearch(int currentClaimCalls, int maxPerClaim, int currentRequestCalls) {
        return currentClaimCalls < maxPerClaim
                && currentRequestCalls < properties.getSearch().getMaxPerRequest();
    }

    private boolean isRetryableError(String errorMsg) {
        if (errorMsg == null) {
            return false;
        }
        String lower = errorMsg.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("internalerror")
                || lower.contains("serviceunavailable")
                || lower.contains("500")
                || lower.contains("502")
                || lower.contains("503")
                || lower.contains("504");
    }

    /**
     * 尝试扣减每日全局额度，达 80% 输出运维告警，超限返回 false
     */
    private boolean tryAcquireDailyQuota() {
        refreshQuotaDateIfNeeded();
        int limit = properties.getSearch().getDailyQuota();
        int used = dailyUsedCount.incrementAndGet();
        if (used > limit) {
            dailyUsedCount.decrementAndGet();
            log.error("[QuotaAlert] 今日核验检索额度已耗尽: used={}, limit={}", used - 1, limit);
            return false;
        }
        int warnThreshold = (int) Math.ceil(limit * properties.getSearch().getQuotaWarnRatio());
        if (used >= warnThreshold && dailyWarnLogged.compareAndSet(false, true)) {
            log.warn("[QuotaAlert] 今日核验检索额度已达到 80% 告警线: used={}, limit={}", used, limit);
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
                }
            }
        }
    }

    private static void notifyStage(Consumer<ClaimVerifyStage> callback, ClaimVerifyStage stage) {
        if (callback != null && stage != null) {
            callback.accept(stage);
        }
    }
}

