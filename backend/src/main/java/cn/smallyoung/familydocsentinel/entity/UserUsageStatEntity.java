package cn.smallyoung.familydocsentinel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户资源用量累计统计实体（大模型 Token 与元宝搜索用量，含日/周/月周期统计与额度控制）
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_user_usage_stat")
public class UserUsageStatEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_prompt_tokens", nullable = false)
    @Builder.Default
    private Long totalPromptTokens = 0L;

    @Column(name = "total_completion_tokens", nullable = false)
    @Builder.Default
    private Long totalCompletionTokens = 0L;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Long totalTokens = 0L;

    @Column(name = "total_search_count", nullable = false)
    @Builder.Default
    private Long totalSearchCount = 0L;

    @Column(name = "task_count", nullable = false)
    @Builder.Default
    private Integer taskCount = 0;

    @Column(name = "daily_tokens", nullable = false)
    @Builder.Default
    private Long dailyTokens = 0L;

    @Column(name = "weekly_tokens", nullable = false)
    @Builder.Default
    private Long weeklyTokens = 0L;

    @Column(name = "monthly_tokens", nullable = false)
    @Builder.Default
    private Long monthlyTokens = 0L;

    @Column(name = "daily_search_count", nullable = false)
    @Builder.Default
    private Long dailySearchCount = 0L;

    @Column(name = "weekly_search_count", nullable = false)
    @Builder.Default
    private Long weeklySearchCount = 0L;

    @Column(name = "monthly_search_count", nullable = false)
    @Builder.Default
    private Long monthlySearchCount = 0L;

    @Column(name = "daily_period_date")
    private LocalDate dailyPeriodDate;

    @Column(name = "weekly_period_date")
    private LocalDate weeklyPeriodDate;

    @Column(name = "monthly_period_date")
    private LocalDate monthlyPeriodDate;

    @Column(name = "daily_token_limit")
    private Long dailyTokenLimit;

    @Column(name = "weekly_token_limit")
    private Long weeklyTokenLimit;

    @Column(name = "monthly_token_limit")
    private Long monthlyTokenLimit;

    @Column(name = "daily_search_limit")
    private Long dailySearchLimit;

    @Column(name = "weekly_search_limit")
    private Long weeklySearchLimit;

    @Column(name = "monthly_search_limit")
    private Long monthlySearchLimit;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

