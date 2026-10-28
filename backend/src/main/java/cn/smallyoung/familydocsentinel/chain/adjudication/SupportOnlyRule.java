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
 * 裁决规则顺序 6：仅有“支持”立场规则（对应需求文档 4.7 节第 6 条）
 * <p>
 * 条件：仅有“支持（SUPPORT）”（可含“缺乏证据 INSUFFICIENT”）。
 * 结论：
 * - 达到定论强度 -> 有依据（SUPPORTED）；
 * - 未达到定论强度 -> 没查实·证据偏弱（WEAK_EVIDENCE）。
 * 特殊规则：“支持”与“缺乏证据”并存不算冲突，仍按第 6 条处理，并附注“另有来源称研究不足”，置信度取“中（MEDIUM）”。
 * </p>
 */
@Order(6)
@Component
public class SupportOnlyRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        List<EvidenceItemDto> active = context.getActiveEvidences();
        if (active == null || active.isEmpty()) {
            return false;
        }

        List<EvidenceItemDto> supportList = active.stream()
                .filter(ev -> StanceType.SUPPORT.getCode().equals(ev.getStanceCode()))
                .toList();
        boolean hasInsufficient = active.stream()
                .anyMatch(ev -> StanceType.INSUFFICIENT.getCode().equals(ev.getStanceCode()));

        if (supportList.isEmpty()) {
            return false;
        }

        if (AdjudicationSupport.hasConclusivenessStrength(supportList)) {
            if (hasInsufficient) {
                AdjudicationSupport.appendExtraNote(context, "另有来源称研究不足");
            }
            // 当“支持”与“缺乏证据”并存时，置信度强制取“中”
            ConfidenceLevel confidence = AdjudicationSupport.computeConfidence(supportList, hasInsufficient);
            AdjudicationSupport.applyDefiniteVerdict(
                    context,
                    VerdictType.SUPPORTED,
                    confidence,
                    active,
                    "查证可信来源显示，该说法与现有医学证据相符。"
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

