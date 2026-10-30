package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户资源用量与双链额度（Token & 元宝搜索 · 日/周/月）概览 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUsageDto {

    private Long userId;

    /**
     * 大模型 Token 总消耗
     */
    private Long totalTokens;

    /**
     * 输入提示词 Token
     */
    private Long totalPromptTokens;

    /**
     * 生成回复 Token
     */
    private Long totalCompletionTokens;

    /**
     * 腾讯元宝搜索调用总次数
     */
    private Long totalSearchCount;

    /**
     * 累计发起的核验任务数
     */
    private Integer taskCount;

    // ================== 每日额度与已用量 ==================

    /**
     * 今日已用 Token
     */
    private Long dailyTokensUsed;

    /**
     * 每日 Token 额度上限
     */
    private Long dailyTokenLimit;

    /**
     * 今日已用元宝搜索次数
     */
    private Long dailySearchUsed;

    /**
     * 每日元宝搜索次数额度上限
     */
    private Long dailySearchLimit;

    // ================== 每周额度与已用量 ==================

    /**
     * 本周已用 Token
     */
    private Long weeklyTokensUsed;

    /**
     * 每周 Token 额度上限
     */
    private Long weeklyTokenLimit;

    /**
     * 本周已用元宝搜索次数
     */
    private Long weeklySearchUsed;

    /**
     * 每周元宝搜索次数额度上限
     */
    private Long weeklySearchLimit;

    // ================== 每月额度与已用量 ==================

    /**
     * 本月已用 Token
     */
    private Long monthlyTokensUsed;

    /**
     * 每月 Token 额度上限
     */
    private Long monthlyTokenLimit;

    /**
     * 本月已用元宝搜索次数
     */
    private Long monthlySearchUsed;

    /**
     * 每月元宝搜索次数额度上限
     */
    private Long monthlySearchLimit;

    // ================== 额度熔断状态 ==================

    /**
     * 是否已触发任一额度上限而停止服务
     */
    private boolean quotaExceeded;

    /**
     * 额度耗尽提示文案（未超限时为 null）
     */
    private String quotaExceededReason;

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}

