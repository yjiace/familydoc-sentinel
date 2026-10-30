package cn.smallyoung.familydocsentinel.config;

import cn.smallyoung.familydocsentinel.security.TaskExecutionHolder;
import cn.smallyoung.familydocsentinel.security.UserContext;
import cn.smallyoung.familydocsentinel.service.UsageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

/**
 * 集中拦截 Spring AI 大模型调用并自动校验双链额度与统计 Token 消耗的 Advisor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTokenUsageAdvisor implements CallAdvisor {

    private final UsageTrackingService usageTrackingService;
    private final cn.smallyoung.familydocsentinel.service.TaskStateManager taskStateManager;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Long userId = TaskExecutionHolder.getUserId();
        if (userId == null) {
            userId = UserContext.getUserId();
        }
        if (userId != null) {
            usageTrackingService.checkUserQuotaAvailable(userId);
        }

        long start = System.currentTimeMillis();
        ChatClientResponse clientResponse = chain.nextCall(request);
        long cost = System.currentTimeMillis() - start;

        try {
            ChatResponse chatResponse = clientResponse.chatResponse();
            if (chatResponse != null && chatResponse.getMetadata() != null) {
                Usage usage = chatResponse.getMetadata().getUsage();
                if (usage != null) {
                    int promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens() : 0;
                    int completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0;
                    String taskId = TaskExecutionHolder.getTaskId();
                    usageTrackingService.recordModelUsage(userId, taskId, "DEEPSEEK_CHAT", promptTokens, completionTokens, cost);
                    taskStateManager.recordTaskTokens(taskId, promptTokens + completionTokens);
                }
            }
        } catch (Exception e) {
            log.warn("AiTokenUsageAdvisor 统计 Token 出现异常: {}", e.getMessage());
        }

        return clientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return "AiTokenUsageAdvisor";
    }
}

