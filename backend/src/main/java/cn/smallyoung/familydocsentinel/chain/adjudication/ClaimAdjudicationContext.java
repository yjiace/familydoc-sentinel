package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条论断规则裁决上下文（用于第二层裁决责任链节点间传递状态）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimAdjudicationContext {

    /**
     * 待裁决并回填结论的目标论断对象
     */
    private ClaimItemDto claim;

    /**
     * 检索或大模型调用是否因超时、限流、服务异常或格式异常而未能完成（对应规则顺序 1）
     */
    private boolean runtimeErrorOrTimeout;

    /**
     * 是否因四项硬校验不通过导致模型标注的立场条目全部作废（对应规则顺序 2 的“引用未通过校验”）
     */
    private boolean allInvalidatedByValidation;

    /**
     * 当前参与裁决的有效立场证据列表（已排除 IRRELEVANT；
     * 若规则 3 中仅一方含一级来源，规则 3 会将其过滤为含一级来源的一方并继续向下传递）
     */
    @Builder.Default
    private List<EvidenceItemDto> activeEvidences = new ArrayList<>();

    /**
     * 当存在冲突或不同口径时，需要并列展示或作为“口径不同”附注展示的另一方证据列表
     */
    @Builder.Default
    private List<EvidenceItemDto> opposingEvidences = new ArrayList<>();

    /**
     * 当立场条目全部未通过四项硬校验作废时，附带展示的原始准入材料（标注“未据此下结论”）
     */
    @Builder.Default
    private List<EvidenceItemDto> invalidatedMaterials = new ArrayList<>();

    /**
     * 通过四项校验后的说明文字（<=120字）
     */
    private String validatedExplanation;

    /**
     * 裁决过程中的附加提示注脚（如“口径不同”或“另有来源称研究不足”）
     */
    private String extraNote;
}

