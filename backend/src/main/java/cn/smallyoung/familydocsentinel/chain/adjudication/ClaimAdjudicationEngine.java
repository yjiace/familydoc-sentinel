package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.enums.UnverifiedCause;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 单条论断规则裁决责任链执行引擎（对应需求文档 4.7 节）
 * <p>
 * 自动注入按 {@code @Order(0)} ~ {@code @Order(6)} 排序的全部 {@link ClaimAdjudicationRule}，
 * 依次执行评估，首个返回 {@code true} 的规则即产出最终结论并终止后续链路。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimAdjudicationEngine {

    private final List<ClaimAdjudicationRule> adjudicationRules;

    /**
     * 执行裁决责任链
     *
     * @param context 单论断裁决上下文
     */
    public void adjudicate(ClaimAdjudicationContext context) {
        for (ClaimAdjudicationRule rule : adjudicationRules) {
            if (rule.evaluate(context)) {
                log.debug("Claim [{}] adjudicated by rule [{}] -> verdict={}",
                        context.getClaim().getClaimId(),
                        rule.getClass().getSimpleName(),
                        context.getClaim().getVerdictCode());
                return;
            }
        }
        // 兜底保护：若全部规则均未命中，默认按“没查实·证据不足”归档
        AdjudicationSupport.applyUnverifiedVerdict(
                context,
                UnverifiedCause.INSUFFICIENT_EVIDENCE,
                context.getActiveEvidences(),
                context.getOpposingEvidences()
        );
    }
}

