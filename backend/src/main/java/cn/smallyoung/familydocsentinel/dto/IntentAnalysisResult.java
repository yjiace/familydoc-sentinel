package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户输入意图分析结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentAnalysisResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 是否允许进入事实核验流程
     */
    private boolean valid;

    /**
     * 意图类别:
     * FACT_CHECKABLE: 可核验的健康/医学说法
     * GREETING: 打招呼/闲聊寒暄
     * NON_HEALTH: 非医疗健康领域内容
     * VAGUE: 过于宽泛笼统无具体论断
     * MALICIOUS: 恶意指令覆盖/Prompt越狱
     */
    private String category;

    /**
     * 判定原因简述
     */
    private String reason;

    /**
     * 面向用户的针对性引导或友好提示文案
     */
    private String guideMessage;
}
