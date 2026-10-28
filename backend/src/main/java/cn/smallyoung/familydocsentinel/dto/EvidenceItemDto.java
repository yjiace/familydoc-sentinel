package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 核验出处与证据条目 DTO（对应需求文档 US-03、4.5、4.6 节）
 * <p>
 * 界面统一称摘要为“来源摘要”。包含来源机构、标题、发布日期（缺失显示“日期未标注”）、
 * 来源摘要（800 字以内按句截取）、可点击链接、来源分级、媒体报道标识以及立场与逐字引文。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 证据在当前论断 Top 4 候选列表中的序号（1~4）
     */
    private Integer index;

    /**
     * 来源机构或站点名称（如“国家卫生健康委员会”、“人民网-健康频道”）
     */
    private String sourceOrg;

    /**
     * 规范化主域名（用于展示与不同域名定论强度计算）
     */
    private String domain;

    /**
     * 来源文章标题
     */
    private String title;

    /**
     * 发布日期展示文本（格式 yyyy-MM-dd，缺失时固定为“日期未标注”）
     */
    private String publishDate;

    /**
     * 来源摘要（来自检索结果摘要，按句截取至 800 字以内；界面统一称“来源摘要”）
     */
    private String sourceSummary;

    /**
     * 可点击原文安全链接（仅限 http/https 协议）
     */
    private String url;

    /**
     * 来源分级编码：TIER_1（一级来源）/ TIER_2（二级来源）
     */
    private String tierCode;

    /**
     * 来源分级中文名称：“一级来源” / “二级来源”
     */
    private String tierLabel;

    /**
     * 是否为主流媒体健康频道（对应 4.5 节：主流媒体健康频道需标注“媒体报道”）
     */
    private boolean mediaReport;

    /**
     * 来源类型标签（如“媒体报道”，非媒体报道时为空或机构性质）
     */
    private String sourceTag;

    /**
     * 检索相关性得分（0~1）
     */
    private Double relevanceScore;

    /**
     * 综合排序分（相关度 × 等级系数：一级 1.5，二级 1.2）
     */
    private Double rankScore;

    /**
     * 模型标注立场编码：SUPPORT / REFUTE / CONDITIONAL / INSUFFICIENT / IRRELEVANT
     */
    private String stanceCode;

    /**
     * 模型标注立场中文名：支持 / 否定 / 条件成立 / 缺乏证据 / 未涉及
     */
    private String stanceLabel;

    /**
     * 摘要逐字引文（除“未涉及”外，≤60 字，且必须通过逐字包含校验）
     */
    private String quote;

    /**
     * 该条立场引用是否通过四项硬校验（链接、逐字引文、来源等级、说明文字校验）
     */
    private boolean citationValid;

    /**
     * 附加标注（例如当所有立场条目作废时，按 4.6 节要求附已检索材料并标注“未据此下结论”；
     * 或当一级来源优先裁决时，另一方标注“口径不同”）
     */
    private String note;
}

