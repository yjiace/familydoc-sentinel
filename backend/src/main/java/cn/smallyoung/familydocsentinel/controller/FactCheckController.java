package cn.smallyoung.familydocsentinel.controller;

import cn.hutool.v7.core.data.id.IdUtil;
import cn.smallyoung.familydocsentinel.dto.FactCheckRequest;
import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import cn.smallyoung.familydocsentinel.dto.RedFlagResult;
import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.interfaces.ResponseSysResult;
import cn.smallyoung.familydocsentinel.service.FactCheckService;
import cn.smallyoung.familydocsentinel.service.RateLimiterService;
import cn.smallyoung.familydocsentinel.service.RedFlagService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 家医智核·流言粉碎 核心 REST 接口控制器
 * <p>
 * 提供：
 * 1. 创建流言核验任务（支持同步红线预检、2000字校验、缓存探测与异步责任链启动）；
 * 2. 实时查询核验任务检索进度与逐条论断状态（支撑前端实时进度展示与逐条卡片渲染）；
 * 3. 核验中取消任务（US-01）；
 * 4. 单条失败论断安全重试（US-07 & 4.10）；
 * 5. 独立危急红线扫描接口（US-02 & 4.1）。
 * </p>
 */
@Slf4j
@RestController
@ResponseSysResult
@RequestMapping("/api/v1/factcheck")
public class FactCheckController {

    private static final String RATE_LIMIT_MSG = "操作太频繁，请稍后再试";

    @Resource
    private FactCheckService factCheckService;

    @Resource
    private RateLimiterService rateLimiterService;

    @Resource
    private RedFlagService redFlagService;

    /**
     * 创建并启动核验任务（同时兼容 JSON 请求体 {@link FactCheckRequest} 与纯文本请求体）
     *
     * @param rawBody 请求体（可为 JSON 字符串或直接粘贴的纯文本）
     * @param request HTTP 请求对象（用于获取客户端 IP 执行滑动窗口限流）
     * @return 核验任务初始状态响应（含 taskId、实时阶段描述、红线结果或缓存命中结果）
     */
    @PostMapping({ "createTask", "tasks" })
    public FactCheckTaskResponse createTask(@RequestBody(required = false) String rawBody,
            HttpServletRequest request) {
        // 1. 单 IP 滑动窗口限流检查（每 10 分钟最多 10 次，对应需求 4.10 与第 5 节）
        if (!rateLimiterService.tryAcquire(request)) {
            throw BizException.of(RATE_LIMIT_MSG);
        }

        // 2. 兼容解析 FactCheckRequest JSON 或纯文本输入
        FactCheckRequest factCheckRequest = parseFactCheckRequest(rawBody);

        // 3. 生成唯一任务 ID 并触发责任链（同步前置节点 + 异步核验节点）
        String taskId = "t-" + IdUtil.getSeataSnowflake();
        return factCheckService.submitTask(taskId, factCheckRequest);
    }

    /**
     * 查询核验任务实时检索状态、阶段进度与已完成的论断卡片（供前端轮询刷新进度）
     *
     * @param taskId 任务唯一 ID
     * @return 任务全量实时状态快照
     */
    @GetMapping({ "tasks/{taskId}", "status/{taskId}" })
    public FactCheckTaskResponse getTaskStatus(@PathVariable("taskId") String taskId) {
        return factCheckService.getTaskStatus(taskId);
    }

    /**
     * 取消正在进行中的核验任务（对应需求文档 US-01“核验中可取消”）
     *
     * @param taskId 任务唯一 ID
     * @return 取消后的任务状态
     */
    @PostMapping("tasks/{taskId}/cancel")
    public FactCheckTaskResponse cancelTask(@PathVariable("taskId") String taskId) {
        return factCheckService.cancelTask(taskId);
    }

    /**
     * 单条失败论断单独重试（对应需求文档 US-07“某条失败可单独重试”与 4.10“单论断重试请求须经服务端校验”）
     *
     * @param taskId  任务唯一 ID
     * @param claimId 论断唯一 ID（如 c-1）
     * @param request HTTP 请求对象
     * @return 触发重试后的任务状态
     */
    @PostMapping("tasks/{taskId}/claims/{claimId}/retry")
    public FactCheckTaskResponse retryClaim(@PathVariable("taskId") String taskId,
            @PathVariable("claimId") String claimId,
            HttpServletRequest request) {
        if (!rateLimiterService.tryAcquire(request)) {
            throw BizException.of(RATE_LIMIT_MSG);
        }
        return factCheckService.retrySingleClaim(taskId, claimId);
    }

    /**
     * 独立危急红线扫描接口（对应需求文档 US-02 与 4.1 节）
     *
     * @param rawBody 待扫描文本或 FactCheckRequest JSON
     * @return 危急红线扫描结果（NONE / WARN / HARD）
     */
    @PostMapping("redflag/scan")
    public RedFlagResult scanRedFlag(@RequestBody(required = false) String rawBody) {
        FactCheckRequest req = parseFactCheckRequest(rawBody);
        return redFlagService.scan(req.getText(), req.isIgnoreHardRedFlag());
    }

    /**
     * 兼容解析 JSON 对象与纯文本请求体
     */
    private FactCheckRequest parseFactCheckRequest(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return FactCheckRequest.builder().text("").build();
        }
        String trimmed = rawBody.strip();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                JSONObject obj = JSON.parseObject(trimmed);
                if (obj != null && obj.containsKey("text")) {
                    return FactCheckRequest.builder()
                            .text(obj.getString("text"))
                            .ignoreHardRedFlag(Boolean.TRUE.equals(obj.getBoolean("ignoreHardRedFlag")))
                            .skipCache(Boolean.TRUE.equals(obj.getBoolean("skipCache")))
                            .build();
                }
            } catch (Exception ignored) {
                // 解析失败时按普通纯文本处理
            }
        }
        return FactCheckRequest.builder().text(rawBody).build();
    }
}
