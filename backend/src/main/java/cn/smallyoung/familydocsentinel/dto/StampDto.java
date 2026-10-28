package cn.smallyoung.familydocsentinel.dto;

import cn.smallyoung.familydocsentinel.enums.VerdictType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 核验结论印章视觉元数据 DTO（对应需求文档 US-05 与 4.8 节）
 * <p>
 * 提供口语大字、严谨小字、颜色、边框形态三重标识及旁注“AI 辅助核验·非官方结论”。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StampDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 结论编码：SUPPORTED / MISLEADING / CONDITIONAL / UNVERIFIED / ASK_DOCTOR
     */
    private String verdictCode;

    /**
     * 印章口语大字：有依据 / 有误导 / 看情况 / 没查实 / 问医生
     */
    private String colloquialText;

    /**
     * 印章严谨小字：证据相符 / 与依据矛盾 / 条件受限 / 无法核实 / 请咨询医生
     */
    private String rigorousText;

    /**
     * 建议色值（Hex，满足 WCAG AA 对比度）
     */
    private String colorHex;

    /**
     * 边框形态编码
     */
    private String borderShapeCode;

    /**
     * 边框形态中文说明
     */
    private String borderShapeDesc;

    /**
     * 印章旁固定标注：“AI 辅助核验·非官方结论”
     */
    private String sideNote;

    /**
     * 根据裁决结论枚举构建印章对象（若为“待重试”则返回 null，不出印章）
     */
    public static StampDto fromVerdict(VerdictType verdict) {
        if (verdict == null || !verdict.isShowStamp()) {
            return null;
        }
        return StampDto.builder()
                .verdictCode(verdict.getCode())
                .colloquialText(verdict.getColloquialText())
                .rigorousText(verdict.getRigorousText())
                .colorHex(verdict.getColorHex())
                .borderShapeCode(verdict.getBorderShapeCode())
                .borderShapeDesc(verdict.getBorderShapeDesc())
                .sideNote(VerdictType.STAMP_SIDE_NOTE)
                .build();
    }
}

