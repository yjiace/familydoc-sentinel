package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * “没查实”的五类细分原因枚举（对应需求文档 US-04、4.6、4.7 节）
 */
@Getter
@RequiredArgsConstructor
public enum UnverifiedCause {

    /**
     * 证据不足：无有效立场
     */
    INSUFFICIENT_EVIDENCE("INSUFFICIENT_EVIDENCE", "证据不足", "没找到可靠来源直接讨论这一说法"),

    /**
     * 目前缺乏证据：仅有“缺乏证据”立场
     */
    CURRENTLY_LACK_EVIDENCE("CURRENTLY_LACK_EVIDENCE", "目前缺乏证据", "可信来源称目前研究不足"),

    /**
     * 来源冲突：“否定”与“支持或条件成立”同时存在，且并非仅一方含一级来源
     */
    SOURCE_CONFLICT("SOURCE_CONFLICT", "来源冲突", "不同来源说法不一致"),

    /**
     * 证据偏弱：同向证据达不到定论强度（既无一级来源，也不足 2 个不同域名）
     */
    WEAK_EVIDENCE("WEAK_EVIDENCE", "证据偏弱", "只找到少量来源，不足以下结论"),

    /**
     * 引用未通过校验：模型输出的立场条目全部未通过四项硬校验而作废
     */
    CITATION_VALIDATION_FAILED("CITATION_VALIDATION_FAILED", "引用未通过校验", "未能核对到可靠出处");

    /**
     * “没查实”固定明示文案（对应 US-04：明示“没查实不代表是真的，也不代表是假的”）
     */
    public static final String UNVERIFIED_DISCLAIMER = "没查实不代表是真的，也不代表是假的";

    /**
     * 引用未通过校验时附带相关材料的固定标注（对应 4.6 节）
     */
    public static final String INVALIDATED_EVIDENCE_NOTE = "未据此下结论";

    private final String code;
    private final String label;
    private final String userMessage;
}

