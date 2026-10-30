package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.dto.UserUsageDto;
import cn.smallyoung.familydocsentinel.entity.UsageLogEntity;
import cn.smallyoung.familydocsentinel.entity.UserUsageStatEntity;
import cn.smallyoung.familydocsentinel.repository.UsageLogRepository;
import cn.smallyoung.familydocsentinel.repository.UserUsageStatRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 资源用量（大模型 Token 与元宝搜索次数）统计与审计服务
 */
@Slf4j
@Service
public class UsageTrackingService {

    @Resource
    private UserUsageStatRepository userUsageStatRepository;

    @Resource
    private UsageLogRepository usageLogRepository;

    /**
     * 记录大模型 Token 消耗
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordModelUsage(Long userId, String taskId, String action,
                                 int promptTokens, int completionTokens, long costTimeMs) {
        int totalTokens = promptTokens + completionTokens;
        try {
            // 1. 记流水日志
            UsageLogEntity logEntity = UsageLogEntity.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .serviceType("DEEPSEEK_AI")
                    .callAction(action)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .searchCount(0)
                    .costTimeMs(costTimeMs)
                    .build();
            usageLogRepository.save(logEntity);

            // 2. 如果已登录，累加用户统计
            if (userId != null && totalTokens > 0) {
                UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                        .orElseGet(() -> UserUsageStatEntity.builder().userId(userId).build());

                stat.setTotalPromptTokens(stat.getTotalPromptTokens() + promptTokens);
                stat.setTotalCompletionTokens(stat.getTotalCompletionTokens() + completionTokens);
                stat.setTotalTokens(stat.getTotalTokens() + totalTokens);
                userUsageStatRepository.save(stat);
            }
        } catch (Exception e) {
            log.error("记录大模型 Token 用量异常: {}", e.getMessage());
        }
    }

    /**
     * 记录腾讯元宝搜索调用次数
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordSearchUsage(Long userId, String taskId, int searchCount, long costTimeMs) {
        try {
            // 1. 记流水日志
            UsageLogEntity logEntity = UsageLogEntity.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .serviceType("TENCENT_WSA_SEARCH")
                    .callAction("WSA_WEB_SEARCH")
                    .promptTokens(0)
                    .completionTokens(0)
                    .totalTokens(0)
                    .searchCount(searchCount)
                    .costTimeMs(costTimeMs)
                    .build();
            usageLogRepository.save(logEntity);

            // 2. 如果已登录，累加用户统计
            if (userId != null && searchCount > 0) {
                UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                        .orElseGet(() -> UserUsageStatEntity.builder().userId(userId).build());

                stat.setTotalSearchCount(stat.getTotalSearchCount() + searchCount);
                userUsageStatRepository.save(stat);
            }
        } catch (Exception e) {
            log.error("记录元宝搜索用量异常: {}", e.getMessage());
        }
    }

    /**
     * 累加用户发起的核验任务数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementTaskCount(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                    .orElseGet(() -> UserUsageStatEntity.builder().userId(userId).build());
            stat.setTaskCount(stat.getTaskCount() + 1);
            userUsageStatRepository.save(stat);
        } catch (Exception e) {
            log.error("累加用户核验任务数异常: {}", e.getMessage());
        }
    }

    /**
     * 查询用户用量概览
     */
    public UserUsageDto getUserUsage(Long userId) {
        if (userId == null) {
            return UserUsageDto.builder().build();
        }
        UserUsageStatEntity stat = userUsageStatRepository.findByUserId(userId)
                .orElseGet(() -> UserUsageStatEntity.builder()
                        .userId(userId)
                        .totalTokens(0L)
                        .totalPromptTokens(0L)
                        .totalCompletionTokens(0L)
                        .totalSearchCount(0L)
                        .taskCount(0)
                        .updatedAt(LocalDateTime.now())
                        .build());

        return UserUsageDto.builder()
                .userId(userId)
                .totalTokens(stat.getTotalTokens())
                .totalPromptTokens(stat.getTotalPromptTokens())
                .totalCompletionTokens(stat.getTotalCompletionTokens())
                .totalSearchCount(stat.getTotalSearchCount())
                .taskCount(stat.getTaskCount())
                .updatedAt(stat.getUpdatedAt())
                .build();
    }
}

