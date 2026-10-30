package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.chain.pipeline.FactCheckContext;
import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import com.alibaba.fastjson2.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 核验任务实时状态与缓存管理服务（对应需求文档 US-01、US-07、US-09、3.2、4.9、4.10 节）
 * <p>
 * 负责：
 * 1. 维护活跃任务的 {@link FactCheckContext}，支撑前端实时轮询检索进度、核验中取消（US-01）与单论断重试校验（4.10）；
 * 2. 维护基于规范化全文哈希（queryHash）的完整核验结果缓存（仅当全部论断有结论、无“待重试”条目时才允许写入）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStateManager {

    private final FactCheckProperties properties;

    /**
     * 活跃任务上下文内存表：taskId -> FactCheckContext
     */
    private Cache<String, FactCheckContext> activeTaskCache;

    /**
     * 完整核验结果高频缓存：queryHash -> FactCheckTaskResponse JSON 快照
     */
    private Cache<String, String> completedResultCache;

    @PostConstruct
    public void init() {
        int activeTtlMinutes = Math.max(10, properties.getCache().getActiveTaskTtlMinutes());
        int resultTtlHours = Math.max(1, properties.getCache().getResultCacheTtlHours());

        this.activeTaskCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofMinutes(activeTtlMinutes))
                .maximumSize(10_000)
                .build();

        this.completedResultCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(resultTtlHours))
                .maximumSize(20_000)
                .build();
    }

    /**
     * 注册新创建的任务上下文
     */
    public void registerTask(FactCheckContext context) {
        activeTaskCache.put(context.getTaskId(), context);
    }

    /**
     * 根据 taskId 获取任务上下文
     */
    public Optional<FactCheckContext> getTaskContext(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(activeTaskCache.getIfPresent(taskId.strip()));
    }

    /**
     * 累加当前核验任务的模型 Token 消耗量
     */
    public void recordTaskTokens(String taskId, int tokens) {
        if (taskId != null && tokens > 0) {
            getTaskContext(taskId).ifPresent(ctx -> ctx.getTokensUsed().addAndGet(tokens));
        }
    }

    /**
     * 根据规范化全文指纹哈希（queryHash）探测已完成的核验缓存
     */
    public Optional<FactCheckTaskResponse> getCachedResultByHash(String queryHash) {
        if (queryHash == null || queryHash.isBlank()) {
            return Optional.empty();
        }
        String json = completedResultCache.getIfPresent(queryHash);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            FactCheckTaskResponse copy = JSON.parseObject(json, FactCheckTaskResponse.class);
            return Optional.ofNullable(copy);
        } catch (Exception e) {
            log.warn("Failed to deserialize cached FactCheckTaskResponse for hash={}", queryHash, e);
            completedResultCache.invalidate(queryHash);
            return Optional.empty();
        }
    }

    /**
     * 将完整核验结果写入缓存（严格遵循需求 4.9 节：存在待重试条目时绝不写入缓存）
     */
    public void cacheCompletedResultIfEligible(FactCheckTaskResponse response) {
        if (response == null || !response.isAllowClientCache()) {
            return;
        }
        String queryHash = response.getQueryHash();
        if (queryHash == null || queryHash.isBlank()) {
            return;
        }
        try {
            completedResultCache.put(queryHash, JSON.toJSONString(response));
        } catch (Exception e) {
            log.warn("Failed to cache FactCheckTaskResponse for hash={}", queryHash, e);
        }
    }
}

