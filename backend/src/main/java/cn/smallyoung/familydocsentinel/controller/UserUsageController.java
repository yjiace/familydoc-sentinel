package cn.smallyoung.familydocsentinel.controller;

import cn.smallyoung.familydocsentinel.dto.UserUsageDto;
import cn.smallyoung.familydocsentinel.interfaces.ResponseSysResult;
import cn.smallyoung.familydocsentinel.security.RequireAuth;
import cn.smallyoung.familydocsentinel.security.UserContext;
import cn.smallyoung.familydocsentinel.service.UsageTrackingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资源用量控制器（Token 与元宝搜索用量统计）
 */
@Slf4j
@RestController
@ResponseSysResult
@RequireAuth
@RequestMapping("/api/v1/user")
public class UserUsageController {

    @Resource
    private UsageTrackingService usageTrackingService;

    /**
     * 查询当前登录用户的 Token 消耗与元宝搜索使用量
     */
    @GetMapping("usage")
    public UserUsageDto getUserUsage() {
        Long userId = UserContext.getUserId();
        return usageTrackingService.getUserUsage(userId);
    }
}

