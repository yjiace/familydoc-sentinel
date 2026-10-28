package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 论断分类枚举（对应需求文档 4.2 节）
 */
@Getter
@RequiredArgsConstructor
public enum ClaimCategory {

    /**
     * 药物类：涉及药物作用、副作用、停药换药
     * （注：4.7 节要求药物类论断无论结论为何，卡片均附药物安全提示）
     */
    DRUG("DRUG", "药物类"),

    /**
     * 疾病类：涉及疾病预防、治疗、诊断
     */
    DISEASE("DISEASE", "疾病类"),

    /**
     * 食物与生活方式类：日常饮食、运动、习惯
     */
    LIFESTYLE("LIFESTYLE", "食物与生活方式类");

    private final String code;
    private final String label;

    /**
     * 从字符串安全解析分类，默认回退为 LIFESTYLE
     */
    public static ClaimCategory from(String raw) {
        if (raw == null || raw.isBlank()) {
            return LIFESTYLE;
        }
        String normalized = raw.trim();
        for (ClaimCategory category : values()) {
            if (category.code.equalsIgnoreCase(normalized) || category.label.equals(normalized)) {
                return category;
            }
        }
        if (normalized.contains("药")) {
            return DRUG;
        }
        if (normalized.contains("病") || normalized.contains("症") || normalized.contains("治")) {
            return DISEASE;
        }
        return LIFESTYLE;
    }
}

