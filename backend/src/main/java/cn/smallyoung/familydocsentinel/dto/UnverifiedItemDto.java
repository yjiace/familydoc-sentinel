package cn.smallyoung.familydocsentinel.dto;

import cn.smallyoung.familydocsentinel.enums.UnverifiedReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 未纳入核验的内容清单条目 DTO（对应需求文档 US-08 与 4.2 节）
 * <p>
 * 用于前端折叠清单展示：逐项显示原文片段与未纳入核验的原因。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnverifiedItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 未被核验的原文片段
     */
    private String segmentText;

    /**
     * 未核验原因编码：
     * IRRELEVANT_OR_SUBJECTIVE / NAMED_PRODUCT_PROMOTION / UNVERIFIABLE_OR_VAGUE / EXCEED_LIMIT
     */
    private String reasonCode;

    /**
     * 未核验原因面向用户的中文说明
     */
    private String reasonDescription;

    public static UnverifiedItemDto of(String segmentText, UnverifiedReason reason) {
        return UnverifiedItemDto.builder()
                .segmentText(segmentText)
                .reasonCode(reason.getCode())
                .reasonDescription(reason.getDescription())
                .build();
    }
}

