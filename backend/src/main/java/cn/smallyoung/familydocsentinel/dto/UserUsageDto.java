package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户资源用量概览 DTO
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

    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
}

