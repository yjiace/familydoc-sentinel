package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 裁决结论与印章视觉元数据枚举（对应需求文档 4.7、4.8、4.9 节及 US-05）
 * <p>
 * 包含口语大字、严谨小字、建议色值、边框形态三重标识及是否出印章/进缓存标记。
 * 印章旁统一标注“AI 辅助核验·非官方结论”。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum VerdictType {

    /**
     * 证据相符 -> 口语大字“有依据”
     */
    SUPPORTED(
            "SUPPORTED",
            "有依据",
            "证据相符",
            "#047857",
            "ROUNDED_DOUBLE_SOLID_SQUARE",
            "圆角双实线方章",
            true,
            true
    ),

    /**
     * 与依据矛盾 -> 口语大字“有误导”
     */
    MISLEADING(
            "MISLEADING",
            "有误导",
            "与依据矛盾",
            "#E11D48",
            "DOUBLE_SOLID_RECTANGLE",
            "双实线矩形章",
            true,
            true
    ),

    /**
     * 条件受限 -> 口语大字“看情况”
     */
    CONDITIONAL(
            "CONDITIONAL",
            "看情况",
            "条件受限",
            "#B45309",
            "DASHED_SOLID_RECTANGLE",
            "虚实结合矩形章",
            true,
            true
    ),

    /**
     * 无法核实 -> 口语大字“没查实”
     */
    UNVERIFIED(
            "UNVERIFIED",
            "没查实",
            "无法核实",
            "#475569",
            "SINGLE_THIN_STAMP",
            "单细线印章",
            true,
            true
    ),

    /**
     * 咨询医生 -> 口语大字“问医生”
     */
    ASK_DOCTOR(
            "ASK_DOCTOR",
            "问医生",
            "请咨询医生",
            "#1D4ED8",
            "ALERT_ROUNDED_RECTANGLE",
            "警示圆角矩形章",
            true,
            true
    ),

    /**
     * 待重试 -> 显示“这条暂时没查成”，不是结论，不出印章，不入缓存
     */
    RETRY_NEEDED(
            "RETRY_NEEDED",
            "这条暂时没查成",
            "待重试",
            "#64748B",
            "NONE",
            "无印章",
            false,
            false
    );

    /**
     * 印章旁固定合规标注（US-05）
     */
    public static final String STAMP_SIDE_NOTE = "AI 辅助核验·非官方结论";

    /**
     * 结论英文编码
     */
    private final String code;

    /**
     * 印章口语大字（面向长辈一眼看懂）
     */
    private final String colloquialText;

    /**
     * 印章严谨小字
     */
    private final String rigorousText;

    /**
     * 印章建议色值（满足 WCAG AA 对比度）
     */
    private final String colorHex;

    /**
     * 边框形态编码（供前端组件渲染对应边框样式）
     */
    private final String borderShapeCode;

    /**
     * 边框形态中文描述
     */
    private final String borderShapeDesc;

    /**
     * 是否展示印章（“待重试”不出印章）
     */
    private final boolean showStamp;

    /**
     * 是否允许写入缓存（存在“待重试”时整次结果不写入缓存）
     */
    private final boolean cacheable;
}

