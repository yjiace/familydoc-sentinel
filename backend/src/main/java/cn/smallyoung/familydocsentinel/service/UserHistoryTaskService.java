package cn.smallyoung.familydocsentinel.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import cn.smallyoung.familydocsentinel.dto.HistoryTaskDetailDto;
import cn.smallyoung.familydocsentinel.dto.HistoryTaskSummaryDto;
import cn.smallyoung.familydocsentinel.entity.UserHistoryTaskEntity;
import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.repository.UserHistoryTaskRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 用户核验任务历史持久化与查询服务
 */
@Slf4j
@Service
public class UserHistoryTaskService {

    @Resource
    private UserHistoryTaskRepository userHistoryTaskRepository;

    /**
     * 保存或更新任务持久化快照
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveOrUpdateTask(Long userId, String taskId, String originalText,
                                 String status, String summary, Integer claimCount,
                                 String claimsJson, String redFlagLevel,
                                 Integer tokensUsed, Integer searchUsed) {
        if (userId == null || taskId == null) {
            return;
        }
        try {
            Optional<UserHistoryTaskEntity> existingOpt = userHistoryTaskRepository.findByTaskId(taskId);
            UserHistoryTaskEntity entity = existingOpt.orElseGet(() -> UserHistoryTaskEntity.builder()
                    .userId(userId)
                    .taskId(taskId)
                    .originalText(originalText != null ? originalText : "")
                    .build());

            entity.setStatus(status != null ? status : "PROCESSING");
            if (summary != null) {
                entity.setSummary(summary);
            }
            if (claimCount != null) {
                entity.setClaimCount(claimCount);
            }
            if (claimsJson != null) {
                entity.setClaimsJson(claimsJson);
            }
            if (redFlagLevel != null) {
                entity.setRedFlagLevel(redFlagLevel);
            }
            if (tokensUsed != null) {
                entity.setTokensUsed(tokensUsed);
            }
            if (searchUsed != null) {
                entity.setSearchUsed(searchUsed);
            }
            userHistoryTaskRepository.save(entity);
            log.debug("成功持久化用户核验任务历史: taskId={}, userId={}", taskId, userId);
        } catch (Exception e) {
            log.error("持久化用户核验任务历史异常: {}", e.getMessage());
        }
    }

    /**
     * 分页查询用户核验历史列表（摘要版，不加载大体积 claimsJson）
     */
    public Page<HistoryTaskSummaryDto> getUserTaskList(Long userId, int page, int size) {
        if (userId == null) {
            return Page.empty();
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<UserHistoryTaskEntity> entityPage = userHistoryTaskRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return entityPage.map(e -> HistoryTaskSummaryDto.builder()
                .id(e.getId())
                .taskId(e.getTaskId())
                .originalText(e.getOriginalText())
                .status(e.getStatus())
                .summary(e.getSummary())
                .claimCount(e.getClaimCount())
                .redFlagLevel(e.getRedFlagLevel())
                .tokensUsed(e.getTokensUsed())
                .searchUsed(e.getSearchUsed())
                .createdAt(e.getCreatedAt())
                .build());
    }

    /**
     * 查询单次历史核验详情（含完整 claims 快照）
     */
    public HistoryTaskDetailDto getTaskDetail(Long userId, String taskId) {
        if (userId == null || taskId == null) {
            throw BizException.of("参数错误");
        }
        UserHistoryTaskEntity entity = userHistoryTaskRepository.findByTaskIdAndUserId(taskId, userId)
                .orElseThrow(() -> BizException.of("历史记录不存在或无权查看"));

        JSONArray claimsArray = null;
        if (entity.getClaimsJson() != null && !entity.getClaimsJson().isBlank()) {
            try {
                claimsArray = JSON.parseArray(entity.getClaimsJson());
            } catch (Exception ignored) {
            }
        }

        return HistoryTaskDetailDto.builder()
                .id(entity.getId())
                .taskId(entity.getTaskId())
                .originalText(entity.getOriginalText())
                .status(entity.getStatus())
                .summary(entity.getSummary())
                .claimCount(entity.getClaimCount())
                .redFlagLevel(entity.getRedFlagLevel())
                .tokensUsed(entity.getTokensUsed())
                .searchUsed(entity.getSearchUsed())
                .claims(claimsArray)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 删除单条历史记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long userId, String taskId) {
        if (userId == null || taskId == null) {
            return;
        }
        userHistoryTaskRepository.deleteByTaskIdAndUserId(taskId, userId);
    }

    /**
     * 清空当前用户所有历史记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void clearAllTasks(Long userId) {
        if (userId == null) {
            return;
        }
        userHistoryTaskRepository.deleteAllByUserId(userId);
    }
}

