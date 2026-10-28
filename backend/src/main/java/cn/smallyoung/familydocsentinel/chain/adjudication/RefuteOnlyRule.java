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
 * 裁决规则顺序 4：仅有“否定”立场规则（对应需求文档 4.7 节第 4 条）
 * <p>
 * 条件：仅有“否定（REFUTE）”（可含“缺乏证据 INSUFFICIENT”）。
 * 结论：
 * - 达到定论强度（至少 1 条一级来源，或来自至少 2 个不同域名） -> 有误导（MISLEADING）；
 * - 未达到定论强度 -> 没查实·证据偏弱（WEAK_EVIDENCE）。
 * </p>
 */
@Order(4)
@Component
public class RefuteOnlyRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        List<EvidenceItemDto> active = context.getActiveEvidences();
        if (active == null || active.isEmpty()) {
            return false;
        }

        List<EvidenceItemDto> refuteList = active.stream()
                .filter(ev -> StanceType.REFUTE.getCode().equals(ev.getStanceCode()))
                .toList();
        boolean hasPositiveOrCond = active.stream()
                .anyMatch(ev -> StanceType.SUPPORT.getCode().equals(ev.getStanceCode())
                        || StanceType.CONDITIONAL.getCode().equals(ev.getStanceCode()));

        if (refuteList.isEmpty() || hasPositiveOrCond) {
            return false;
        }

        // 检查同向（否定）有效证据是否达到定论强度
        if (AdjudicationSupport.hasConclusivenessStrength(refuteList)) {
            ConfidenceLevel confidence = AdjudicationSupport.computeConfidence(refuteList, false);
            AdjudicationSupport.applyDefiniteVerdict(
                    context,
                    VerdictType.MISLEADING,
                    confidence,
                    active,
                    "查证可信来源指出，该说法与现有医学证据矛盾。"
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

