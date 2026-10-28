package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 论断风险等级枚举（对应需求文档 4.2 节）
 * <p>
 * 当提取出的可核验论断超过 5 条时，按风险等级降序（HIGH -> MEDIUM -> LOW）、同级按原文顺序保留前 5 条。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum RiskLevel {

    /**
     * 高风险：涉及用药、停药、替代治疗、急症处理方法（排序优先级 3）
     */
    HIGH("HIGH", "高", 3),

    /**
     * 中风险：疾病预防或治疗效果（排序优先级 2）
     */
    MEDIUM("MEDIUM", "中", 2),

    /**
     * 低风险：日常饮食与生活方式（排序优先级 1）
     */
    LOW("LOW", "低", 1);

    private final String code;
    private final String label;
    private final int priority;

    /**
     * 从字符串安全解析风险等级，默认回退为 MEDIUM
     */
    public static RiskLevel from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        String normalized = raw.trim();
        for (RiskLevel level : values()) {
            if (level.code.equalsIgnoreCase(normalized) || level.label.equals(normalized)) {
                return level;
            }
        }
        if (normalized.contains("高")) {
            return HIGH;
        }
        if (normalized.contains("低")) {
            return LOW;
        }
        return MEDIUM;
    }
}

