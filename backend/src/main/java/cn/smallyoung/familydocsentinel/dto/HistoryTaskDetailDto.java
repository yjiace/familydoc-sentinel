package cn.smallyoung.familydocsentinel.dto;

import com.alibaba.fastjson2.JSONArray;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 历史核验任务完整快照详情
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoryTaskDetailDto {

    private Long id;

    private String taskId;

    private String originalText;

    private String status;

    private String summary;

    private Integer claimCount;

    private String redFlagLevel;

    private Integer tokensUsed;

    private Integer searchUsed;

    /**
     * 完整论断卡片、依据与置信度列表快照
     */
    private JSONArray claims;

    private LocalDateTime createdAt;
}

