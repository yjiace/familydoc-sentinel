package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.dto.RedFlagResult;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.service.RedFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 主流程责任链节点 2（@Order 20）：服务端危急红线独立扫描（对应需求文档 US-02、3.3、4.1 节）
 * <p>
 * 职责：
 * 1. 严格在任何检索与大模型调用（含意图分析）之前执行；
 * 2. 若命中拦截级（HARD）且用户未点击“这只是文章里的内容（继续核验）”，则立即阻断责任链，
 *    不做任何外部调用，返回红色警报与一键拨打 120 提示；
 * 3. 若命中提示级（WARN）或用户确认仅为转述内容降级，则保留顶部黄色提醒条并继续后续核验。
 * </p>
 */
@Slf4j
@Order(20)
@Component
@RequiredArgsConstructor
public class RedFlagScanHandler implements FactCheckPipelineHandler {

    private final RedFlagService redFlagService;

    @Override
    public boolean isSynchronousPreCheck() {
        return true;
    }

    @Override
    public boolean handle(FactCheckContext context) {
        context.updateStage(TaskStage.RED_FLAG_SCANNING);

        RedFlagResult redFlag = redFlagService.scan(
                context.getNormalizedRawText(),
                context.isIgnoreHardRedFlag()
        );
        context.getResponse().setRedFlag(redFlag);

        // 命中 HARD 拦截级：立即短路终止责任链，不发起任何外部检索与模型调用
        if (redFlag.isBlocked()) {
            context.markBlocked(redFlag.getMessage());
            return false;
        }
        return true;
    }
}

