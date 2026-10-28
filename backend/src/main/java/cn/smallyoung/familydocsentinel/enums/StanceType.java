package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 证据立场标注枚举（对应需求文档 4.6 节）
 */
@Getter
@RequiredArgsConstructor
public enum StanceType {

    /**
     * 支持：来源明确支持该论断
     */
    SUPPORT("SUPPORT", "支持"),

    /**
     * 否定：来源直接否定，明确表述该说法不成立或无效
     */
    REFUTE("REFUTE", "否定"),

    /**
     * 条件成立：仅在特定剂量、体质或前提下成立
     */
    CONDITIONAL("CONDITIONAL", "条件成立"),

    /**
     * 缺乏证据：来源称目前尚无证据或研究不足，但未否定
     */
    INSUFFICIENT("INSUFFICIENT", "缺乏证据"),

    /**
     * 未涉及：来源未涉及该论断本身
     */
    IRRELEVANT("IRRELEVANT", "未涉及");

    private final String code;
    private final String label;

    public static StanceType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return IRRELEVANT;
        }
        String normalized = raw.trim();
        for (StanceType type : values()) {
            if (type.code.equalsIgnoreCase(normalized) || type.label.equals(normalized)) {
                return type;
            }
        }
        if (normalized.contains("否定") || normalized.contains("反驳") || normalized.contains("不成立")) {
            return REFUTE;
        }
        if (normalized.contains("条件")) {
            return CONDITIONAL;
        }
        if (normalized.contains("支持") || normalized.contains("赞同")) {
            return SUPPORT;
        }
        if (normalized.contains("缺乏") || normalized.contains("不足")) {
            return INSUFFICIENT;
        }
        return IRRELEVANT;
    }
}

