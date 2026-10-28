package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 核验结论置信度枚举（对应需求文档 4.7 节）
 * <p>
 * 规则：
 * - 仅“有依据（SUPPORTED）”与“有误导（MISLEADING）”计算高/中置信度：
 *   同向来源来自 ≥2 个不同域名且含 ≥1 条一级来源为“高”，否则为“中”；
 *   当“支持”与“缺乏证据”并存时，置信度取“中”。
 * - “看情况（CONDITIONAL）”固定为“中”。
 * - 其余状态无置信度等级（NONE）。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ConfidenceLevel {

    HIGH("HIGH", "高"),
    MEDIUM("MEDIUM", "中"),
    NONE("NONE", "");

    private final String code;
    private final String label;
}

