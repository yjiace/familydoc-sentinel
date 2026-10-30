package cn.smallyoung.familydocsentinel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 模型与搜索服务调用审计流水日志实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_usage_log", indexes = {
        @Index(name = "idx_usage_log_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_usage_log_task", columnList = "task_id")
})
public class UsageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "task_id", length = 64)
    private String taskId;

    @Column(name = "service_type", nullable = false, length = 30)
    private String serviceType;

    @Column(name = "call_action", nullable = false, length = 50)
    private String callAction;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Integer searchCount = 0;

    @Column(name = "cost_time_ms", nullable = false)
    @Builder.Default
    private Long costTimeMs = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

