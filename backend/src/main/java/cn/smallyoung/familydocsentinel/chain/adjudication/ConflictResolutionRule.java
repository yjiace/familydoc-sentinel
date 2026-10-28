package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.enums.StanceType;
import cn.smallyoung.familydocsentinel.enums.UnverifiedCause;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 裁决规则顺序 3：来源立场冲突判定与一级来源消歧规则（对应需求文档 4.7 节第 3 条）
 * <p>
 * 条件：“否定（REFUTE）”与“支持（SUPPORT）或条件成立（CONDITIONAL）”同时存在。
 * 规则：
 * - 若仅一方含一级来源（TIER_1），则以该方进入后续规则（顺序 4/5/6），另一方作为“口径不同”附注展示；
 * - 否则（双方均含一级来源，或双方均只有二级来源），直接裁决为“没查实·来源冲突（SOURCE_CONFLICT）”，并列展示双方证据。
 * </p>
 */
@Order(3)
@Component
public class ConflictResolutionRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        List<EvidenceItemDto> active = context.getActiveEvidences();
        if (active == null || active.isEmpty()) {
            return false;
        }

        List<EvidenceItemDto> refuteSide = new ArrayList<>();
        List<EvidenceItemDto> supportOrConditionalSide = new ArrayList<>();
        List<EvidenceItemDto> insufficientSide = new ArrayList<>();

        for (EvidenceItemDto ev : active) {
            String code = ev.getStanceCode();
            if (StanceType.REFUTE.getCode().equals(code)) {
                refuteSide.add(ev);
            } else if (StanceType.SUPPORT.getCode().equals(code)
                    || StanceType.CONDITIONAL.getCode().equals(code)) {
                supportOrConditionalSide.add(ev);
            } else if (StanceType.INSUFFICIENT.getCode().equals(code)) {
                insufficientSide.add(ev);
            }
        }

        // 未同时出现“否定”与“支持或条件成立”，无冲突，放行至后续规则
        if (refuteSide.isEmpty() || supportOrConditionalSide.isEmpty()) {
            return false;
        }

        boolean refuteHasTier1 = AdjudicationSupport.hasTier1Source(refuteSide);
        boolean positiveHasTier1 = AdjudicationSupport.hasTier1Source(supportOrConditionalSide);

        // 若仅一方含一级来源：以含一级来源的一方进入后续规则 4~6，另一方作为“口径不同”附注展示
        if (refuteHasTier1 ^ positiveHasTier1) {
            List<EvidenceItemDto> winningSide = new ArrayList<>(refuteHasTier1 ? refuteSide : supportOrConditionalSide);
            winningSide.addAll(insufficientSide);

            List<EvidenceItemDto> losingSide = refuteHasTier1 ? supportOrConditionalSide : refuteSide;
            for (EvidenceItemDto opp : losingSide) {
                opp.setNote("口径不同");
            }

            context.setActiveEvidences(winningSide);
            context.setOpposingEvidences(losingSide);
            AdjudicationSupport.appendExtraNote(context, "口径不同：另有部分来源持不同说法，已优先采信一级来源");
            // 返回 false，让责任链继续流转至顺序 4 / 5 / 6
            return false;
        }

        // 双方均含一级来源或双方均仅为二级来源 -> 裁决为“没查实·来源冲突”，并列展示双方
        AdjudicationSupport.applyUnverifiedVerdict(
                context,
                UnverifiedCause.SOURCE_CONFLICT,
                refuteSide,
                supportOrConditionalSide
        );
        return true;
    }
}

