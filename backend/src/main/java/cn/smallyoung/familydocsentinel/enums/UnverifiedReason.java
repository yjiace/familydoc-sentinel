package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 未纳入核验清单的原因分类枚举（对应需求文档 4.2 节与 US-08）
 */
@Getter
@RequiredArgsConstructor
public enum UnverifiedReason {

    /**
     * 与健康无关，或纯属主观感受（如投资理财、“我觉得这个方子挺神”）
     */
    IRRELEVANT_OR_SUBJECTIVE("IRRELEVANT_OR_SUBJECTIVE", "与健康无关，或纯属主观感受"),

    /**
     * 具名商品或品牌的功效宣传（如“某某牌胶囊三天见效”，规避名誉与广告风险）
     */
    NAMED_PRODUCT_PROMOTION("NAMED_PRODUCT_PROMOTION", "具名商品或品牌的功效宣传"),

    /**
     * 无法验证或过于笼统（如“吃了就能延年益寿”、“多喝水对身体好”，或检索词压缩重写2次后仍超长）
     */
    UNVERIFIABLE_OR_VAGUE("UNVERIFIABLE_OR_VAGUE", "无法验证或过于笼统"),

    /**
     * 超出单次核验 5 条论断上限
     */
    EXCEED_LIMIT("EXCEED_LIMIT", "超出 5 条上限");

    private final String code;
    private final String description;

    public static UnverifiedReason from(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNVERIFIABLE_OR_VAGUE;
        }
        String normalized = raw.trim();
        for (UnverifiedReason reason : values()) {
            if (reason.code.equalsIgnoreCase(normalized) || reason.description.equals(normalized)) {
                return reason;
            }
        }
        if (normalized.contains("品牌") || normalized.contains("商品") || normalized.contains("广告")) {
            return NAMED_PRODUCT_PROMOTION;
        }
        if (normalized.contains("无关") || normalized.contains("主观")) {
            return IRRELEVANT_OR_SUBJECTIVE;
        }
        if (normalized.contains("上限") || normalized.contains("5")) {
            return EXCEED_LIMIT;
        }
        return UNVERIFIABLE_OR_VAGUE;
    }
}

