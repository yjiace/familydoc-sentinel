package cn.smallyoung.familydocsentinel.chain.pipeline;

/**
 * 核验任务主流程责任链处理器接口（第一层责任链）
 * <p>
 * 各处理器通过 Spring {@code @Order} 注解编排顺序，围绕 {@link FactCheckContext} 协同工作：
 * - 返回 {@code true} 表示当前节点正常处理完毕，继续向责任链下一个节点传递；
 * - 返回 {@code false} 表示当前节点已触发短路终止（如红线拦截、缓存直接命中、非健康意图拦截、无有效论断等），
 *   责任链调度器将立即停止调用后续节点。
 * </p>
 */
public interface FactCheckPipelineHandler {

    /**
     * 是否属于任务创建时的同步前置节点
     * <p>
     * 同步前置节点（如输入校验与清洗、危急红线扫描、缓存探测）在 HTTP 请求线程中执行，
     * 耗时节点（意图分析、论断拆解、并行检索核验、文章级摘要生成）在异步线程池中执行并实时更新进度状态。
     * </p>
     */
    default boolean isSynchronousPreCheck() {
        return false;
    }

    /**
     * 执行当前责任链节点的业务逻辑
     *
     * @param context 核验任务上下文
     * @return {@code true} 继续执行后续节点；{@code false} 短路终止责任链
     */
    boolean handle(FactCheckContext context);
}

