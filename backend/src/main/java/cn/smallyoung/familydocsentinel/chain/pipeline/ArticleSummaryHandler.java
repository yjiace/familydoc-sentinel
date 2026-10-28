package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.service.ArticleSummaryGenerator;
import cn.smallyoung.familydocsentinel.service.TaskStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 主流程责任链节点 7（@Order 70）：文章级摘要生成与缓存回写处理器（对应需求文档 US-06、3.10、4.8、4.9 节）
 * <p>
 * 职责：
 * 1. 基于纯规则模板生成文章级一句话总结（不调用大模型，不使用“靠谱/可信度高”等判断性词汇）；
 * 2. 标记任务完成；
 * 3. 若不存在“待重试”条目，将完整核验结果写入服务端缓存。
 * </p>
 */
@Slf4j
@Order(70)
@Component
@RequiredArgsConstructor
public class ArticleSummaryHandler implements FactCheckPipelineHandler {

    private final ArticleSummaryGenerator articleSummaryGenerator;
    private final TaskStateManager taskStateManager;

    @Override
    public boolean handle(FactCheckContext context) {
        if (context.isCancelled()) {
            return false;
        }
        context.updateStage(TaskStage.SUMMARY_GENERATING);

        String summary = articleSummaryGenerator.generateSummary(
                context.getResponse().getClaims(),
                context.getResponse().getUnverifiedItems()
        );
        context.getResponse().setArticleSummary(summary);
        context.markCompleted();

        // 需求 4.9 节：仅当无“待重试”条目（allowClientCache == true）时才写入缓存
        taskStateManager.cacheCompletedResultIfEligible(context.getResponse());
        return true;
    }
}

