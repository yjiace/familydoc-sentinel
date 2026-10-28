package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 单条可核验论断及其核验结果与实时检索状态 DTO（对应需求文档 US-03~US-07、4.2~4.8 节）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimItemDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 药物类论断固定附加提示文案（对应需求 4.7 节）
     */
    public static final String DRUG_SAFETY_NOTICE =
            "药物相关提示：请勿自行停药、换药或加减剂量，如有疑问请咨询医生或药师。";

    /**
     * 个体用药咨询固定说明文案（对应需求 4.2、4.7 节）
     */
    public static final String PERSONAL_MEDICATION_EXPLANATION =
            "该问题涉及个人或家人的具体用药、停药或剂量调整决定。因每个人病情、体质与合并用药不同，本工具不提供个体化用药建议，请咨询主治医生或临床药师。";

    /**
     * 论断唯一标识（如 c-1, c-2，用于前端渲染及单论断安全重试校验）
     */
    private String claimId;

    /**
     * 在原文中出现的先后顺序索引（从 1 开始）
     */
    private int originalOrder;

    /**
     * 提取出的可核验原子论断陈述文本
     */
    private String claimText;

    /**
     * 论断分类编码：DRUG（药物类）/ DISEASE（疾病类）/ LIFESTYLE（食物与生活方式类）
     */
    private String categoryCode;

    /**
     * 论断分类中文名称
     */
    private String categoryLabel;

    /**
     * 风险等级编码：HIGH（高）/ MEDIUM（中）/ LOW（低）
     */
    private String riskLevelCode;

    /**
     * 风险等级中文名称
     */
    private String riskLevelLabel;

    /**
     * 是否为个体用药咨询（若为 true，直接生成“问医生”卡片，不检索、不裁决）
     */
    private boolean personalMedication;

    /**
     * 中性检索词（不含“辟谣/谣言/骗局”或机构名）
     */
    private String neutralQuery;

    /**
     * 当前实时检索/核验子阶段编码（PENDING / PRIMARY_SEARCHING / UPGRADE_SEARCHING /
     * EVIDENCE_ADMITTING / STANCE_ANNOTATING / ADJUDICATING / COMPLETED / RETRY_NEEDED）
     */
    private String verifyStageCode;

    /**
     * 当前实时检索/核验子阶段中文描述（供前端实时展示检索进度）
     */
    private String verifyStageText;

    /**
     * 本条论断已执行的外部检索次数（含升级检索与重试，硬上限 3 次）
     */
    private int searchCount;

    /**
     * 裁决结论编码：SUPPORTED / MISLEADING / CONDITIONAL / UNVERIFIED / ASK_DOCTOR / RETRY_NEEDED
     */
    private String verdictCode;

    /**
     * 印章口语大字（如“有依据”、“有误导”、“看情况”、“没查实”、“问医生”、“这条暂时没查成”）
     */
    private String colloquialVerdict;

    /**
     * 印章严谨小字（如“证据相符”、“与依据矛盾”、“条件受限”、“无法核实”、“请咨询医生”）
     */
    private String rigorousVerdict;

    /**
     * 印章视觉元数据（当结论为 RETRY_NEEDED 待重试时为 null，不出印章）
     */
    private StampDto stamp;

    /**
     * 结论置信度编码：HIGH / MEDIUM / NONE
     */
    private String confidenceCode;

    /**
     * 结论置信度中文标签：“高” / “中” / ""
     */
    private String confidenceLabel;

    /**
     * 结论说明文字（≤120 字，仅转述已摘引文，不引入新医学结论，已通过禁用词与数字溯源校验）
     */
    private String explanation;

    /**
     * 当结论为“没查实（UNVERIFIED）”时的五类细分原因编码
     */
    private String unverifiedCauseCode;

    /**
     * 当结论为“没查实（UNVERIFIED）”时的原因标签（如“证据不足”、“来源冲突”）
     */
    private String unverifiedCauseLabel;

    /**
     * 当结论为“没查实（UNVERIFIED）”时的面向用户原因文案（如“没找到可靠来源直接讨论这一说法”）
     */
    private String unverifiedCauseMessage;

    /**
     * 当结论为“没查实（UNVERIFIED）”时的固定明示文案：“没查实不代表是真的，也不代表是假的”
     */
    private String unverifiedDisclaimer;

    /**
     * 药物类附加提示（当 categoryCode == DRUG 时固定附带）
     */
    private String drugSafetyNotice;

    /**
     * 特殊裁决附注文案（如 4.7 节“另有来源称研究不足”或“口径不同”）
     */
    private String extraNote;

    /**
     * 主展示出处证据列表（支持结论的同向有效证据，或校验全部作废时附带的“未据此下结论”参考材料）
     */
    @Builder.Default
    private List<EvidenceItemDto> evidences = new ArrayList<>();

    /**
     * 冲突或不同口径的另一方出处证据列表
     * <p>
     * 对应需求 US-04 与 4.7 节第 3 条：
     * - 当判定为“没查实·来源冲突”时，evidences 与 opposingEvidences 并列展示冲突双方；
     * - 当仅一方含一级来源并按该方得出结论时，另一方放入 opposingEvidences 作为“口径不同”附注展示。
     * </p>
     */
    @Builder.Default
    private List<EvidenceItemDto> opposingEvidences = new ArrayList<>();

    /**
     * 是否处于“待重试”状态并展示重试按钮（对应需求 US-07）
     */
    private boolean retryable;
}

