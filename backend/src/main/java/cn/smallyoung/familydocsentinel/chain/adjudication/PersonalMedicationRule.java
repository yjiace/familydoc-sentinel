package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.StampDto;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import cn.smallyoung.familydocsentinel.enums.ConfidenceLevel;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 裁决规则顺序 0：个体用药咨询分流规则（对应需求文档 4.2、4.7 节第 0 条）
 * <p>
 * 条件：前置分流识别为个体用药咨询（用户在为本人或家人做用药决定，如“我该吃几片降压药”“我爸能不能停药”）。
 * 结论：直接生成“问医生（ASK_DOCTOR）”印章卡片，不检索、不进入后续裁决。
 * </p>
 */
@Order(0)
@Component
public class PersonalMedicationRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        ClaimItemDto claim = context.getClaim();
        if (claim == null || !claim.isPersonalMedication()) {
            return false;
        }

        VerdictType verdict = VerdictType.ASK_DOCTOR;
        claim.setVerifyStageCode(ClaimVerifyStage.COMPLETED.getCode());
        claim.setVerifyStageText(ClaimVerifyStage.COMPLETED.getStatusText());
        claim.setVerdictCode(verdict.getCode());
        claim.setColloquialVerdict(verdict.getColloquialText());
        claim.setRigorousVerdict(verdict.getRigorousText());
        claim.setStamp(StampDto.fromVerdict(verdict));
        claim.setConfidenceCode(ConfidenceLevel.NONE.getCode());
        claim.setConfidenceLabel(ConfidenceLevel.NONE.getLabel());
        claim.setExplanation(ClaimItemDto.PERSONAL_MEDICATION_EXPLANATION);
        claim.setEvidences(List.of());
        claim.setOpposingEvidences(List.of());
        claim.setRetryable(false);
        AdjudicationSupport.ensureDrugSafetyNotice(claim);
        return true;
    }
}

