package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.dto.IntentAnalysisResult;
import cn.smallyoung.familydocsentinel.dto.UnverifiedItemDto;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.UnverifiedReason;
import cn.smallyoung.familydocsentinel.service.UserIntentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 主流程责任链节点 4（@Order 40）：用户提问意图分析与防注入过滤（对应需求文档 4.2、5 节）
 * <p>
 * 职责：
 * 1. 执行防 Prompt 注入粗筛、日常寒暄识别与大模型宽进严查意图分类；
 * 2. 若判定为非健康内容（NON_HEALTH）、过于笼统（VAGUE）、打招呼（GREETING）或恶意注入（MALICIOUS），
 *    生成对应的未核验清单条目与提示语，并短路终止后续检索链路。
 * </p>
 */
@Slf4j
@Order(40)
@Component
@RequiredArgsConstructor
public class UserIntentHandler implements FactCheckPipelineHandler {

    private static final String NO_VERIFIABLE_CLAIMS_MSG = "没有发现可核验的健康说法，请检查输入内容";

    private final UserIntentService userIntentService;

    @Override
    public boolean handle(FactCheckContext context) {
        if (context.isCancelled()) {
            return false;
        }
        context.updateStage(TaskStage.INTENT_ANALYZING);

        IntentAnalysisResult intentResult = userIntentService.analyzeIntent(context.getCleanText());
        context.getResponse().setIntentResult(intentResult);

        if (!intentResult.isValid()) {
            String category = intentResult.getCategory();
            if ("NON_HEALTH".equalsIgnoreCase(category)) {
                context.getResponse().setUnverifiedItems(List.of(
                        UnverifiedItemDto.of(context.getNormalizedRawText(), UnverifiedReason.IRRELEVANT_OR_SUBJECTIVE)
                ));
            } else if ("VAGUE".equalsIgnoreCase(category)) {
                context.getResponse().setUnverifiedItems(List.of(
                        UnverifiedItemDto.of(context.getNormalizedRawText(), UnverifiedReason.UNVERIFIABLE_OR_VAGUE)
                ));
            }

            String notice = (intentResult.getGuideMessage() != null && !intentResult.getGuideMessage().isBlank())
                    ? intentResult.getGuideMessage()
                    : NO_VERIFIABLE_CLAIMS_MSG;
            context.getResponse().setArticleSummary(NO_VERIFIABLE_CLAIMS_MSG);
            context.markBlocked(notice);
            return false;
        }
        return true;
    }
}

