package cn.smallyoung.familydocsentinel.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户核验任务历史快照实体
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_history_task", indexes = {
        @Index(name = "uk_user_history_task_id", columnList = "task_id", unique = true),
        @Index(name = "idx_user_history_user_created", columnList = "user_id, created_at")
})
public class UserHistoryTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "task_id", nullable = false, length = 64)
    private String taskId;

    @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
    private String originalText;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "claim_count", nullable = false)
    @Builder.Default
    private Integer claimCount = 0;

    @Column(name = "claims_json", columnDefinition = "TEXT")
    private String claimsJson;

    @Column(name = "red_flag_level", length = 20)
    private String redFlagLevel;

    @Column(name = "tokens_used", nullable = false)
    @Builder.Default
    private Integer tokensUsed = 0;

    @Column(name = "search_used", nullable = false)
    @Builder.Default
    private Integer searchUsed = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}

