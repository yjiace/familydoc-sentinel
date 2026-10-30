package cn.smallyoung.familydocsentinel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户资源用量累计统计实体（大模型 Token 与元宝搜索用量）
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

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

