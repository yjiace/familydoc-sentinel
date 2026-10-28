package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import cn.smallyoung.familydocsentinel.enums.ConfidenceLevel;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 裁决规则顺序 1：检索或模型调用异常待重试规则（对应需求文档 US-07、4.7 节第 1 条、4.9 节）
 * <p>
 * 条件：检索或模型调用因超时、限流、服务异常、格式异常而无法完成。
 * 结论：标记为“待重试（RETRY_NEEDED）”（显示“这条暂时没查成”和重试按钮，不是结论，不出印章，不入缓存）。
 * </p>
 */
@Order(1)
@Component
public class RuntimeExceptionRetryRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        if (!context.isRuntimeErrorOrTimeout()) {
            return false;
        }

        ClaimItemDto claim = context.getClaim();
        VerdictType verdict = VerdictType.RETRY_NEEDED;
        claim.setVerifyStageCode(ClaimVerifyStage.RETRY_NEEDED.getCode());
        claim.setVerifyStageText(ClaimVerifyStage.RETRY_NEEDED.getStatusText());
        claim.setVerdictCode(verdict.getCode());
        claim.setColloquialVerdict(verdict.getColloquialText());
        claim.setRigorousVerdict(verdict.getRigorousText());
        // 待重试不出印章
        claim.setStamp(null);
        claim.setConfidenceCode(ConfidenceLevel.NONE.getCode());
        claim.setConfidenceLabel(ConfidenceLevel.NONE.getLabel());
        claim.setExplanation("这条说法在检索或比对过程中暂时没查成，您可以点击重试按钮单独重新核验。");
        claim.setEvidences(List.of());
        claim.setOpposingEvidences(List.of());
        claim.setRetryable(true);
        AdjudicationSupport.ensureDrugSafetyNotice(claim);
        return true;
    }
}

