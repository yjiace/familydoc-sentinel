package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.enums.StanceType;
import cn.smallyoung.familydocsentinel.enums.UnverifiedCause;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 裁决规则顺序 2：无有效立场或仅有“缺乏证据”规则（对应需求文档 4.6、4.7 节第 2 条）
 * <p>
 * 条件：有效立场为空，或仅有“缺乏证据（INSUFFICIENT）”。
 * 结论：没查实（UNVERIFIED），按细分触发原因区分：
 * - 立场条目全部因未通过四项校验作废 -> 没查实·引用未通过校验（附已检索相关材料，标注“未据此下结论”）；
 * - 有效立场为空 -> 没查实·证据不足（“没找到可靠来源直接讨论这一说法”）；
 * - 仅有“缺乏证据”立场 -> 没查实·目前缺乏证据（“可信来源称目前研究不足”）。
 * </p>
 */
@Order(2)
@Component
public class EmptyOrInsufficientStanceRule implements ClaimAdjudicationRule {

    @Override
    public boolean evaluate(ClaimAdjudicationContext context) {
        List<EvidenceItemDto> active = context.getActiveEvidences();

        // 情形 A：有效立场为空
        if (active == null || active.isEmpty()) {
            if (context.isAllInvalidatedByValidation()) {
                // 全部作废时按“没查实·引用未通过校验”处理，并附已检索到的相关材料，标注“未据此下结论”
                AdjudicationSupport.applyUnverifiedVerdict(
                        context,
                        UnverifiedCause.CITATION_VALIDATION_FAILED,
                        context.getInvalidatedMaterials(),
                        List.of()
                );
            } else {
                AdjudicationSupport.applyUnverifiedVerdict(
                        context,
                        UnverifiedCause.INSUFFICIENT_EVIDENCE,
                        List.of(),
                        List.of()
                );
            }
            return true;
        }

        // 情形 B：检查是否全部有效立场均为“缺乏证据（INSUFFICIENT）”
        boolean onlyInsufficient = active.stream()
                .allMatch(ev -> StanceType.INSUFFICIENT.getCode().equals(ev.getStanceCode()));
        if (onlyInsufficient) {
            AdjudicationSupport.applyUnverifiedVerdict(
                    context,
                    UnverifiedCause.CURRENTLY_LACK_EVIDENCE,
                    active,
                    List.of()
            );
            return true;
        }

        return false;
    }
}

