package cn.smallyoung.familydocsentinel.controller;

import cn.smallyoung.familydocsentinel.dto.HistoryTaskDetailDto;
import cn.smallyoung.familydocsentinel.dto.HistoryTaskSummaryDto;
import cn.smallyoung.familydocsentinel.interfaces.ResponseSysResult;
import cn.smallyoung.familydocsentinel.security.RequireAuth;
import cn.smallyoung.familydocsentinel.security.UserContext;
import cn.smallyoung.familydocsentinel.service.UserHistoryTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户云端核验历史记录控制器
 */
@Slf4j
@RestController
@ResponseSysResult
@RequireAuth
@RequestMapping("/api/v1/history")
public class HistoryController {

    @Resource
    private UserHistoryTaskService userHistoryTaskService;

    /**
     * 分页查询当前用户核验历史列表
     */
    @GetMapping("tasks")
    public Page<HistoryTaskSummaryDto> getUserTasks(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        Long userId = UserContext.getUserId();
        return userHistoryTaskService.getUserTaskList(userId, page, size);
    }

    /**
     * 获取历史核验任务详情（含完整论断、裁决结论与证据）
     */
    @GetMapping("tasks/{taskId}")
    public HistoryTaskDetailDto getTaskDetail(@PathVariable("taskId") String taskId) {
        Long userId = UserContext.getUserId();
        return userHistoryTaskService.getTaskDetail(userId, taskId);
    }

    /**
     * 删除单条历史记录
     */
    @DeleteMapping("tasks/{taskId}")
    public Map<String, Object> deleteTask(@PathVariable("taskId") String taskId) {
        Long userId = UserContext.getUserId();
        userHistoryTaskService.deleteTask(userId, taskId);
        return Map.of("success", true, "message", "历史记录已删除");
    }

    /**
     * 清空当前用户所有历史记录
     */
    @DeleteMapping("tasks")
    public Map<String, Object> clearAllTasks() {
        Long userId = UserContext.getUserId();
        userHistoryTaskService.clearAllTasks(userId);
        return Map.of("success", true, "message", "全部历史记录已清空");
    }
}

