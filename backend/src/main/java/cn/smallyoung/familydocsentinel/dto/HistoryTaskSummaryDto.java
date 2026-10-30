package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史核验任务列表项摘要
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryTaskSummaryDto {

    private Long id;

    private String taskId;

    private String originalText;

    private String status;

    private String summary;

    private Integer claimCount;

    private String redFlagLevel;

    private Integer tokensUsed;

    private Integer searchUsed;

    private LocalDateTime createdAt;
}

