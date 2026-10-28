package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.UnverifiedItemDto;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.UnverifiedReason;
import cn.smallyoung.familydocsentinel.service.ClaimExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 主流程责任链节点 5（@Order 50）：论断拆解、分流与中性检索词生成（模型步骤 1，对应需求文档 3.4、4.2、4.3、5 节）
 * <p>
 * 职责：
 * 1. 提取可核验原子论断（上限 5 条，超出按风险等级降序+原文顺序保留，其余转入未核验清单）；
 * 2. 剔除不核验内容（与健康无关或主观感受、具名商品功效宣传、无法验证或过于笼统）；
 * 3. 若未提取到任何可核验论断，提示“没有发现可核验的健康说法，请检查输入内容”，仅展示未核验清单与免责声明并短路。
 * </p>
 */
@Slf4j
@Order(50)
@Component
@RequiredArgsConstructor
public class ClaimExtractionHandler implements FactCheckPipelineHandler {

    private static final String NO_VERIFIABLE_CLAIMS_MSG = "没有发现可核验的健康说法，请检查输入内容";

    private final ClaimExtractionService claimExtractionService;

    @Override
    public boolean handle(FactCheckContext context) {
        if (context.isCancelled()) {
            return false;
        }
        context.updateStage(TaskStage.CLAIM_EXTRACTING);

        ClaimExtractionService.ExtractionResult extraction =
                claimExtractionService.extractAndRoute(context.getCleanText());

        // 若模型步骤 1 调用或解析异常（重试 1 次后仍失败）
        if (extraction.modelFailed()) {
            context.markFailed("拆解健康说法时服务暂时繁忙，请稍后重试");
            return false;
        }

        List<UnverifiedItemDto> unverifiedItems = new ArrayList<>(extraction.unverifiedItems());
        List<ClaimItemDto> verifiableClaims = extraction.verifiableClaims();

        // 需求第 5 节：未提取到可核验论断 -> “没有发现可核验的健康说法，请检查输入内容”，仅展示未核验清单与免责声明
        if (verifiableClaims == null || verifiableClaims.isEmpty()) {
            if (unverifiedItems.isEmpty()) {
                unverifiedItems.add(UnverifiedItemDto.of(
                        context.getNormalizedRawText(),
                        UnverifiedReason.UNVERIFIABLE_OR_VAGUE
                ));
            }
            context.getResponse().setUnverifiedItems(unverifiedItems);
            context.getResponse().setClaims(List.of());
            context.getResponse().setArticleSummary(NO_VERIFIABLE_CLAIMS_MSG);
            context.markBlocked(NO_VERIFIABLE_CLAIMS_MSG);
            return false;
        }

        context.getResponse().setClaims(verifiableClaims);
        context.getResponse().setUnverifiedItems(unverifiedItems);
        context.getResponse().setTotalClaimCount(verifiableClaims.size());
        context.getResponse().setCompletedClaimCount(0);
        return true;
    }
}

