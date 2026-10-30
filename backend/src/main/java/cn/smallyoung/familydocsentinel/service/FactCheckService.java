package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.chain.pipeline.FactCheckContext;
import cn.smallyoung.familydocsentinel.chain.pipeline.FactCheckPipelineHandler;
import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.FactCheckRequest;
import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.TaskStatus;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import cn.smallyoung.familydocsentinel.exception.BizException;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 流言粉碎核心业务编排服务（基于双层责任链设计模式重构）
 * <p>
 * 架构设计：
 * 1. 第一层主流程责任链（{@link FactCheckPipelineHandler}）：
 * - 同步前置链：{@code InputSanitizeHandler(@Order 10)} ->
 * {@code RedFlagScanHandler(@Order 20)} -> {@code CacheProbeHandler(@Order 30)}
 * （确保危急红线 HARD 拦截级在任何检索与大模型调用前立即阻断，且输入校验异常直接向客户端反馈）；
 * - 异步核验链：{@code UserIntentHandler(@Order 40)} ->
 * {@code ClaimExtractionHandler(@Order 50)}
 * -> {@code ParallelClaimVerifyHandler(@Order 60)} ->
 * {@code ArticleSummaryHandler(@Order 70)}
 * （在后台线程池中推进，每个阶段与每条论断检索状态实时同步至 {@link FactCheckContext}，前端轮询即可实时展示进度）。
 * 2. 第二层单论断裁决责任链（{@code ClaimAdjudicationRule}）：
 * - 严格按需求 4.7 节顺序 0 ~ 6 依次裁决，首个命中即产出结论并终止。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactCheckService {

    /**
     * 自动按 @Order 顺序注入的主流程责任链处理器列表
     */
    private final List<FactCheckPipelineHandler> pipelineHandlers;

    private final TaskStateManager taskStateManager;
    private final SingleClaimVerifyService singleClaimVerifyService;
    private final ArticleSummaryGenerator articleSummaryGenerator;
    private final WsaSearchService wsaSearchService;
    private final UserHistoryTaskService userHistoryTaskService;
    private final UsageTrackingService usageTrackingService;
    private final cn.smallyoung.familydocsentinel.repository.UserHistoryTaskRepository userHistoryTaskRepository;

    @Resource(name = "factCheckTaskExecutor")
    private Executor factCheckTaskExecutor;

    /**
     * 提交并启动核验任务（同步执行前置校验/红线/缓存节点，通过后异步执行耗时检索与模型节点）
     *
     * @param taskId  任务唯一 ID
     * @param request 核验请求参数（含文本、红线降级确认标记、跳过缓存标记）
     * @return 任务初始响应（若命中红线拦截或命中缓存，则直接返回最终态）
     */
    public FactCheckTaskResponse submitTask(String taskId, FactCheckRequest request) {
        String rawText = request != null ? request.getText() : null;
        boolean ignoreHardRedFlag = request != null && request.isIgnoreHardRedFlag();
        boolean skipCache = request != null && request.isSkipCache();
        Long userId = request != null ? request.getUserId() : null;

        // 前置校验用户双链额度（Token 与元宝搜索次数，日/周/月任一耗尽即停止服务）
        if (userId != null) {
            usageTrackingService.checkUserQuotaAvailable(userId);
        }

        FactCheckContext context = new FactCheckContext(taskId, rawText, ignoreHardRedFlag, skipCache, userId);
        if (userId != null) {
            usageTrackingService.incrementTaskCount(userId);
        }

        // 1. 同步执行前置责任链节点（输入清洗校验 -> 危急红线扫描 -> 缓存探测）
        for (FactCheckPipelineHandler handler : pipelineHandlers) {
            if (handler.isSynchronousPreCheck()) {
                boolean shouldContinue = handler.handle(context);
                if (!shouldContinue) {
                    // 命中危急红线 HARD 拦截或命中完整缓存，注册任务状态并直接返回短路结果
                    taskStateManager.registerTask(context);
                    if (userId != null) {
                        saveTaskSnapshot(context);
                    }
                    return context.getResponse();
                }
            }
        }

        // 2. 前置节点全部通过，注册活跃任务上下文并异步启动后续责任链节点
        taskStateManager.registerTask(context);
        context.updateStage(TaskStage.INTENT_ANALYZING);

        CompletableFuture.runAsync(() -> executeAsyncPipeline(context), factCheckTaskExecutor);
        return context.getResponse();
    }

    /**
     * 兼容原有方法签名：启动核验任务
     *
     * @param taskId 任务 ID
     * @param text   待核验文本
     */
    public FactCheckTaskResponse startAsyncTask(String taskId, String text) {
        return submitTask(taskId, FactCheckRequest.builder().text(text).build());
    }

    /**
     * 异步执行责任链中的耗时节点（意图分析 -> 论断拆解 -> 并行检索与裁决 -> 文章级摘要生成）
     */
    private void executeAsyncPipeline(FactCheckContext context) {
        cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.set(context.getUserId(), context.getTaskId());
        try {
            for (FactCheckPipelineHandler handler : pipelineHandlers) {
                if (!handler.isSynchronousPreCheck()) {
                    if (context.isCancelled()) {
                        log.info("FactCheck task [{}] cancelled by user, stopping pipeline", context.getTaskId());
                        return;
                    }
                    if (context.getUserId() != null) {
                        usageTrackingService.checkUserQuotaAvailable(context.getUserId());
                    }
                    boolean shouldContinue = handler.handle(context);
                    if (!shouldContinue) {
                        log.info("FactCheck task [{}] short-circuited at handler [{}]",
                                context.getTaskId(), handler.getClass().getSimpleName());
                        return;
                    }
                }
            }
        } catch (BizException bizEx) {
            log.warn("FactCheck async pipeline business exception: taskId={}, msg={}",
                    context.getTaskId(), bizEx.getMessage());
            context.markFailed(bizEx.getMessage());
        } catch (Exception ex) {
            log.error("FactCheck async pipeline unexpected error: taskId={}", context.getTaskId(), ex);
            context.markFailed("核查服务暂时繁忙，请稍后再试");
        } finally {
            if (context.getUserId() != null) {
                saveTaskSnapshot(context);
            }
            cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.clear();
        }
    }

    /**
     * 持久化当前任务的快照至数据库
     */
    private void saveTaskSnapshot(FactCheckContext context) {
        try {
            FactCheckTaskResponse resp = context.getResponse();
            String claimsJson = resp.getClaims() != null ? com.alibaba.fastjson2.JSON.toJSONString(resp.getClaims()) : null;
            String redFlagLevel = resp.getRedFlag() != null && resp.getRedFlag().getLevel() != null ?
                    resp.getRedFlag().getLevel().name() : null;
            userHistoryTaskService.saveOrUpdateTask(
                    context.getUserId(),
                    context.getTaskId(),
                    context.getRawInput(),
                    resp.getStatus(),
                    resp.getArticleSummary(),
                    resp.getTotalClaimCount(),
                    claimsJson,
                    redFlagLevel,
                    context.getTokensUsed() != null ? context.getTokensUsed().get() : 0,
                    context.getRequestSearchBudgetUsed() != null ? context.getRequestSearchBudgetUsed().get() : 0
            );
        } catch (Exception e) {
            log.warn("保存任务历史快照失败: taskId={}, error={}", context.getTaskId(), e.getMessage());
        }
    }

    /**
     * 查询任务实时检索状态、阶段进度与已生成的论断印章卡片
     *
     * @param taskId 任务唯一 ID
     * @return 任务当前全量快照
     */
    public FactCheckTaskResponse getTaskStatus(String taskId) {
        return taskStateManager.getTaskContext(taskId)
                .map(FactCheckContext::getResponse)
                .orElseGet(() -> {
                    // 若内存已过期，尝试从数据库恢复
                    return userHistoryTaskRepository.findByTaskId(taskId)
                            .map(history -> {
                                List<ClaimItemDto> claims = null;
                                if (history.getClaimsJson() != null && !history.getClaimsJson().isBlank()) {
                                    try {
                                        claims = com.alibaba.fastjson2.JSON.parseArray(history.getClaimsJson(), ClaimItemDto.class);
                                    } catch (Exception ignored) {
                                    }
                                }
                                return FactCheckTaskResponse.builder()
                                        .taskId(history.getTaskId())
                                        .status(history.getStatus())
                                        .stage(TaskStage.FINISHED.getCode())
                                        .stageDescription("核验已完成")
                                        .progressPercent(100)
                                        .articleSummary(history.getSummary())
                                        .totalClaimCount(history.getClaimCount())
                                        .completedClaimCount(history.getClaimCount())
                                        .claims(claims)
                                        .updatedAt(history.getUpdatedAt())
                                        .build();
                            })
                            .orElseThrow(() -> BizException.of(HttpStatus.NOT_FOUND.value(), "未找到该核验任务或任务已过期"));
                });
    }

    /**
     * 取消正在执行的核验任务（对应需求文档 US-01“核验中可取消”）
     *
     * @param taskId 任务唯一 ID
     * @return 取消后的任务状态
     */
    public FactCheckTaskResponse cancelTask(String taskId) {
        FactCheckContext context = taskStateManager.getTaskContext(taskId)
                .orElseThrow(() -> BizException.of(HttpStatus.NOT_FOUND.value(), "未找到该核验任务或任务已过期"));
        context.markCancelled();
        return context.getResponse();
    }

    /**
     * 单独重试某条失败（待重试）的论断（对应需求文档 US-07 与 4.10 节）
     * <p>
     * 安全校验（4.10 节）：单论断重试请求须经服务端校验，不得作为通用搜索入口。
     * 服务端强制校验 taskId 与 claimId 的真实归属关系，且仅允许状态为 RETRY_NEEDED 的论断使用原中性检索词重试。
     * </p>
     *
     * @param taskId  任务唯一 ID
     * @param claimId 待重试的论断 ID（如 c-1）
     * @return 触发重试或重试更新后的任务状态
     */
    public FactCheckTaskResponse retrySingleClaim(String taskId, String claimId) {
        FactCheckContext context = taskStateManager.getTaskContext(taskId)
                .orElseThrow(() -> BizException.of(HttpStatus.NOT_FOUND.value(), "未找到该核验任务或任务已过期"));

        List<ClaimItemDto> claims = context.getResponse().getClaims();
        if (claims == null || claims.isEmpty()) {
            throw BizException.of(HttpStatus.BAD_REQUEST.value(), "该任务无可重试的论断条目");
        }

        ClaimItemDto targetClaim = claims.stream()
                .filter(c -> c.getClaimId() != null && c.getClaimId().equals(claimId))
                .findFirst()
                .orElseThrow(() -> BizException.of(HttpStatus.BAD_REQUEST.value(), "未找到指定的论断条目"));

        // 校验该条论断是否确实处于“待重试”状态
        if (!targetClaim.isRetryable() && !VerdictType.RETRY_NEEDED.getCode().equals(targetClaim.getVerdictCode())) {
            throw BizException.of(HttpStatus.BAD_REQUEST.value(), "该条论断已核验完成，无需重复重试");
        }

        // 检查用户双链额度（Token 与元宝搜索次数，日/周/月任一耗尽即停止服务）
        Long currentUserId = cn.smallyoung.familydocsentinel.security.UserContext.getUserId();
        Long quotaUserId = currentUserId != null ? currentUserId : context.getUserId();
        if (quotaUserId != null) {
            usageTrackingService.checkUserQuotaAvailable(quotaUserId);
        }

        // 检查全局日度检索额度是否充足
        wsaSearchService.checkDailyQuotaAvailable();

        // 重置该条论断状态为排队检索中，并异步触发单条重验
        synchronized (context) {
            context.getCancelled().set(false);
            targetClaim.setSearchCount(0);
            targetClaim.setRetryable(false);
            targetClaim.setVerdictCode(null);
            targetClaim.setColloquialVerdict(null);
            targetClaim.setRigorousVerdict(null);
            targetClaim.setStamp(null);
            targetClaim.setVerifyStageCode(ClaimVerifyStage.PRIMARY_SEARCHING.getCode());
            targetClaim.setVerifyStageText(ClaimVerifyStage.PRIMARY_SEARCHING.getStatusText());

            context.getResponse().setStatus(TaskStatus.PROCESSING.getCode());
            context.getResponse().setStatusDescription(TaskStatus.PROCESSING.getDescription());
            context.getResponse().setStage(TaskStage.PARALLEL_VERIFYING.getCode());
            context.getResponse().setStageDescription("正在重新检索核验说法：" + targetClaim.getClaimText());
            context.getResponse().setUpdatedAt(LocalDateTime.now());
            context.refreshParallelVerifyProgress();
        }

        CompletableFuture.runAsync(() -> {
            cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.set(context.getUserId(), context.getTaskId());
            try {
                // 若整次请求原检索预算已耗尽，为单条重试补充最多 3 次预算余量
                int maxPerReq = 12;
                if (context.getRequestSearchBudgetUsed().get() >= maxPerReq) {
                    context.getRequestSearchBudgetUsed().set(Math.max(0, maxPerReq - 3));
                }
                singleClaimVerifyService.verifySingleClaim(context, targetClaim);
                if (!TaskStatus.FAILED.getCode().equals(context.getResponse().getStatus())) {
                    String updatedSummary = articleSummaryGenerator.generateSummary(
                            context.getResponse().getClaims(),
                            context.getResponse().getUnverifiedItems());
                    context.getResponse().setArticleSummary(updatedSummary);
                    context.markCompleted();
                    taskStateManager.cacheCompletedResultIfEligible(context.getResponse());
                }
            } catch (Exception e) {
                log.error("Failed to retry claim [{}] in task [{}]", claimId, taskId, e);
                context.markCompleted();
            } finally {
                if (context.getUserId() != null) {
                    saveTaskSnapshot(context);
                }
                cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.clear();
            }
        }, factCheckTaskExecutor);

        return context.getResponse();
    }
}
