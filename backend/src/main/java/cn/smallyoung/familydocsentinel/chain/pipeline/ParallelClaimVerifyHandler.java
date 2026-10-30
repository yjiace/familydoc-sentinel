package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.chain.adjudication.ClaimAdjudicationContext;
import cn.smallyoung.familydocsentinel.chain.adjudication.ClaimAdjudicationEngine;
import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import cn.smallyoung.familydocsentinel.service.SingleClaimVerifyService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 主流程责任链节点 6（@Order 60）：多论断并发核验与实时进度同步处理器（对应需求文档 US-07、3.5~3.9、4.4~4.7 节）
 * <p>
 * 职责：
 * 1. 控制单任务内论断并行并发上限（初始为 3，通过 {@link Semaphore} 限流）；
 * 2. 单条论断总时限 12 秒，整次请求总时限 30 秒；
 * 3. 每条论断在检索、证据准入、立场标注、规则裁决各子阶段实时更新进度至 {@link FactCheckContext}；
 * 4. 超时未完成的论断自动进入“待重试（RETRY_NEEDED）”，不影响其他已完成论断的展示。
 * </p>
 */
@Slf4j
@Order(60)
@Component
@RequiredArgsConstructor
public class ParallelClaimVerifyHandler implements FactCheckPipelineHandler {

    private final FactCheckProperties properties;
    private final SingleClaimVerifyService singleClaimVerifyService;
    private final ClaimAdjudicationEngine claimAdjudicationEngine;

    @Resource(name = "claimVerifyExecutor")
    private ThreadPoolTaskExecutor claimVerifyExecutor;

    @Override
    public boolean handle(FactCheckContext context) {
        if (context.isCancelled()) {
            return false;
        }
        List<ClaimItemDto> claims = context.getResponse().getClaims();
        if (claims == null || claims.isEmpty()) {
            return false;
        }

        context.updateStage(TaskStage.PARALLEL_VERIFYING);
        context.refreshParallelVerifyProgress();

        int maxConcurrency = Math.max(1, properties.getSearch().getConcurrency());
        int claimTimeoutSec = Math.max(3, properties.getSearch().getClaimTimeoutSeconds());
        int taskTimeoutSec = Math.max(claimTimeoutSec, properties.getSearch().getTaskTimeoutSeconds());
        Semaphore concurrencyLimiter = new Semaphore(maxConcurrency);

        final Long currentUserId = context.getUserId();
        final String currentTaskId = context.getTaskId();

        List<CompletableFuture<Void>> futures = new ArrayList<>(claims.size());
        for (ClaimItemDto claim : claims) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.set(currentUserId, currentTaskId);
                        boolean acquired = false;
                        try {
                            acquired = concurrencyLimiter.tryAcquire(claimTimeoutSec, TimeUnit.SECONDS);
                            if (!acquired || context.isCancelled()) {
                                markClaimRetryNeeded(context, claim);
                                return;
                            }
                            singleClaimVerifyService.verifySingleClaim(context, claim);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            markClaimRetryNeeded(context, claim);
                        } finally {
                            if (acquired) {
                                concurrencyLimiter.release();
                            }
                            cn.smallyoung.familydocsentinel.security.TaskExecutionHolder.clear();
                        }
                    }, claimVerifyExecutor)
                    .orTimeout(claimTimeoutSec, TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        log.warn("Claim [{}] verification timed out or failed: {}",
                                claim.getClaimId(), ex.getMessage());
                        markClaimRetryNeeded(context, claim);
                        return null;
                    });
            futures.add(future);
        }

        // 等待全部论断完成或达到整次请求 30 秒总时限（需求 4.4 节）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(taskTimeoutSec, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Task [{}] reached overall timeout ({}s) or interrupted: {}",
                    context.getTaskId(), taskTimeoutSec, e.getMessage());
            // 将尚未产出结论的论断统一置为“待重试”
            for (ClaimItemDto claim : claims) {
                if (claim.getVerdictCode() == null || claim.getVerdictCode().isBlank()) {
                    markClaimRetryNeeded(context, claim);
                }
            }
        }

        return !context.isCancelled();
    }

    private void markClaimRetryNeeded(FactCheckContext context, ClaimItemDto claim) {
        synchronized (claim) {
            if (claim.getVerdictCode() != null && !VerdictType.RETRY_NEEDED.getCode().equals(claim.getVerdictCode())) {
                return;
            }
            ClaimAdjudicationContext retryCtx = ClaimAdjudicationContext.builder()
                    .claim(claim)
                    .runtimeErrorOrTimeout(true)
                    .build();
            claimAdjudicationEngine.adjudicate(retryCtx);
        }
        context.refreshParallelVerifyProgress();
    }
}

