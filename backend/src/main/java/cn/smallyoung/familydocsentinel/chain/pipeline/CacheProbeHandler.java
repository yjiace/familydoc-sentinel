package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.TaskStatus;
import cn.smallyoung.familydocsentinel.service.TaskStateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 主流程责任链节点 3（@Order 30）：规范化全文哈希缓存探测（对应需求文档 US-09、3.2、4.9 节）
 * <p>
 * 职责：
 * 1. 当未指定 skipCache（重新核验）时，以 queryHash 探测已完成且无“待重试”条目的缓存结果；
 * 2. 若命中缓存，直接将核验结论、未核验清单与文章级摘要回填至当前任务并短路后续外部调用。
 * </p>
 */
@Slf4j
@Order(30)
@Component
@RequiredArgsConstructor
public class CacheProbeHandler implements FactCheckPipelineHandler {

    private final TaskStateManager taskStateManager;

    @Override
    public boolean isSynchronousPreCheck() {
        return true;
    }

    @Override
    public boolean handle(FactCheckContext context) {
        context.updateStage(TaskStage.CACHE_PROBING);

        if (context.isSkipCache()) {
            return true;
        }

        Optional<FactCheckTaskResponse> cachedOpt = taskStateManager.getCachedResultByHash(context.getQueryHash());
        if (cachedOpt.isEmpty()) {
            return true;
        }

        FactCheckTaskResponse cached = cachedOpt.get();
        FactCheckTaskResponse current = context.getResponse();

        current.setCacheHit(true);
        current.setAllowClientCache(true);
        current.setArticleSummary(cached.getArticleSummary());
        current.setClaims(cached.getClaims());
        current.setUnverifiedItems(cached.getUnverifiedItems());
        current.setTotalClaimCount(cached.getTotalClaimCount());
        current.setCompletedClaimCount(cached.getCompletedClaimCount());
        current.setStatus(TaskStatus.COMPLETED.getCode());
        current.setStatusDescription(TaskStatus.COMPLETED.getDescription());
        current.setStage(TaskStage.FINISHED.getCode());
        current.setStageDescription("命中缓存核验结果");
        current.setProgressPercent(100);
        current.setUpdatedAt(LocalDateTime.now());

        log.info("FactCheck cache hit: taskId={}, queryHash={}", context.getTaskId(), context.getQueryHash());
        // 命中缓存，短路后续责任链节点
        return false;
    }
}

