package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.enums.ConfidenceLevel;
import cn.smallyoung.familydocsentinel.enums.StanceType;
import cn.smallyoung.familydocsentinel.enums.UnverifiedCause;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 裁决规则顺序 5：无“否定”、有“条件成立”规则（对应需求文档 4.7 节第 5 条）
 * <p>
 * 条件：无“否定（REFUTE）”，有“条件成立（CONDITIONAL）”。
 * 结论：
 * - 达到定论强度 -> 看情况（CONDITIONAL），置信度固定为“中（MEDIUM）”；
 * - 未达到定论强度 -> 没查实·证据偏弱（WEAK_EVIDENCE）。
 * </p>
 */
@Order(5)
@Component
public class ConditionalRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        List<EvidenceItemDto> active = context.getActiveEvidences();
        if (active == null || active.isEmpty()) {
            return false;
        }

        boolean hasRefute = active.stream()
                .anyMatch(ev -> StanceType.REFUTE.getCode().equals(ev.getStanceCode()));
        List<EvidenceItemDto> conditionalList = active.stream()
                .filter(ev -> StanceType.CONDITIONAL.getCode().equals(ev.getStanceCode()))
                .toList();

        if (hasRefute || conditionalList.isEmpty()) {
            return false;
        }

        // 同向有效来源（条件成立，或条件成立+支持共同构成的同向非否定证据）达到定论强度判定
        List<EvidenceItemDto> nonNegativeEvidences = active.stream()
                .filter(ev -> StanceType.CONDITIONAL.getCode().equals(ev.getStanceCode())
                        || StanceType.SUPPORT.getCode().equals(ev.getStanceCode()))
                .toList();

        if (AdjudicationSupport.hasConclusivenessStrength(nonNegativeEvidences)) {
            // 需求 4.7 节：看情况置信度固定为“中”
            AdjudicationSupport.applyDefiniteVerdict(
                    context,
                    VerdictType.CONDITIONAL,
                    ConfidenceLevel.MEDIUM,
                    active,
                    "查证可信来源显示，该说法仅在特定条件、特定人群或剂量前提下成立。"
            );
        } else {
            AdjudicationSupport.applyUnverifiedVerdict(
                    context,
                    UnverifiedCause.WEAK_EVIDENCE,
                    active,
                    context.getOpposingEvidences()
            );
        }
        return true;
    }
}

