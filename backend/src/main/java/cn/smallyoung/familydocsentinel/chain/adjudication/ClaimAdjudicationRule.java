package cn.smallyoung.familydocsentinel.chain.adjudication;

/**
 * 单条论断裁决规则责任链节点接口（对应需求文档 4.7 节表顺序 0 ~ 6）
 * <p>
 * 各实现类通过 Spring {@code @Order} 注解严格按 0 ~ 6 顺序编排，
 * 由 {@link ClaimAdjudicationEngine} 依次调用，首个返回 {@code true} 的规则即产出最终结论并终止后续链路。
 * </p>
 */
public interface ClaimAdjudicationRule {

    /**
     * 评估当前论断上下文并尝试产出裁决结论
     *
     * @param context 单论断裁决上下文
     * @return {@code true} 表示当前规则已命中并完成结论填充，责任链立即终止；
     *         {@code false} 表示未命中（或已在规则 3 完成一级来源消歧过滤），继续向下传递
     */
    boolean evaluate(ClaimAdjudicationContext context);
}

