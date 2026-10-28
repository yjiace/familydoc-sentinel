package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.chain.adjudication.ClaimAdjudicationContext;
import cn.smallyoung.familydocsentinel.chain.adjudication.ClaimAdjudicationEngine;
import cn.smallyoung.familydocsentinel.chain.pipeline.FactCheckContext;
import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条论断核验全流程执行服务（串联个体咨询短路、检索与升级、证据准入、立场标注、四项校验与规则裁决链）
 * <p>
 * 同时服务于：
 * 1. 主流程责任链中的多论断并行核验（{@code ParallelClaimVerifyHandler}）；
 * 2. 单条“待重试”论断的独立安全重试接口（对应需求 US-07 与 4.10 节）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SingleClaimVerifyService {

    private final WsaSearchService wsaSearchService;
    private final StanceAnnotationService stanceAnnotationService;
    private final ClaimAdjudicationEngine claimAdjudicationEngine;

    /**
     * 对单条论断执行完整核验闭环，并在每个子阶段实时同步检索进度至 {@link FactCheckContext}
     *
     * @param context 任务主上下文
     * @param claim   目标论断 DTO
     */
    public void verifySingleClaim(FactCheckContext context, ClaimItemDto claim) {
        if (context.isCancelled()) {
            return;
        }

        // 1. 顺序 0：若前置分流识别为个体用药咨询，直接通过裁决引擎 Rule 0 产出“问医生”结论，不发起任何检索
        if (claim.isPersonalMedication()) {
            context.updateClaimStage(claim, ClaimVerifyStage.ADJUDICATING);
            ClaimAdjudicationContext adjCtx = ClaimAdjudicationContext.builder()
                    .claim(claim)
                    .build();
            claimAdjudicationEngine.adjudicate(adjCtx);
            context.refreshParallelVerifyProgress();
            return;
        }

        try {
            // 2. 执行检索策略（主检索 -> 证据充分性判定 -> 政府行业升级检索 -> 白名单准入与排序 Top 4）
            // 检索过程中通过 Lambda 回调实时更新 claim 的检索子状态与任务整体进度文案
            WsaSearchService.ClaimSearchOutcome searchOutcome = wsaSearchService.searchForClaim(
                    claim.getNeutralQuery(),
                    context.getRequestSearchBudgetUsed(),
                    context.getCancelled(),
                    stage -> context.updateClaimStage(claim, stage)
            );
            claim.setSearchCount(claim.getSearchCount() + searchOutcome.callsUsed());

            if (context.isCancelled()) {
                return;
            }

            // 3. 若检索因超时、限流或异常未能完成 -> 进入裁决规则顺序 1（待重试）
            if (searchOutcome.failedOrTimedOut()) {
                ClaimAdjudicationContext retryCtx = ClaimAdjudicationContext.builder()
                        .claim(claim)
                        .runtimeErrorOrTimeout(true)
                        .build();
                claimAdjudicationEngine.adjudicate(retryCtx);
                context.refreshParallelVerifyProgress();
                return;
            }

            // 4. 若准入证据为空 -> 直接进入裁决规则顺序 2（没查实·证据不足），不消耗大模型步骤 2
            if (searchOutcome.admittedEvidences().isEmpty()) {
                context.updateClaimStage(claim, ClaimVerifyStage.ADJUDICATING);
                ClaimAdjudicationContext emptyCtx = ClaimAdjudicationContext.builder()
                        .claim(claim)
                        .activeEvidences(List.of())
                        .build();
                claimAdjudicationEngine.adjudicate(emptyCtx);
                context.refreshParallelVerifyProgress();
                return;
            }

            // 5. 模型步骤 2：立场标注、引文提取与四项硬校验
            context.updateClaimStage(claim, ClaimVerifyStage.STANCE_ANNOTATING);
            StanceAnnotationService.StanceCheckResult stanceResult = stanceAnnotationService.annotateAndValidate(
                    claim.getClaimText(),
                    searchOutcome.admittedEvidences()
            );

            if (context.isCancelled()) {
                return;
            }

            // 6. 执行第二层裁决规则责任链（Rule 0 ~ Rule 6）
            context.updateClaimStage(claim, ClaimVerifyStage.ADJUDICATING);
            ClaimAdjudicationContext adjContext = ClaimAdjudicationContext.builder()
                    .claim(claim)
                    .runtimeErrorOrTimeout(stanceResult.modelFailed())
                    .allInvalidatedByValidation(stanceResult.allInvalidatedByCheck())
                    .activeEvidences(new ArrayList<>(stanceResult.validEvidences()))
                    .invalidatedMaterials(stanceResult.invalidatedMaterials())
                    .validatedExplanation(stanceResult.validatedExplanation())
                    .build();

            claimAdjudicationEngine.adjudicate(adjContext);
            context.refreshParallelVerifyProgress();

        } catch (Exception e) {
            log.warn("Unexpected error verifying claim [{}]: {}", claim.getClaimId(), e.getMessage(), e);
            ClaimAdjudicationContext fallbackRetryCtx = ClaimAdjudicationContext.builder()
                    .claim(claim)
                    .runtimeErrorOrTimeout(true)
                    .build();
            claimAdjudicationEngine.adjudicate(fallbackRetryCtx);
            context.refreshParallelVerifyProgress();
        }
    }
}

