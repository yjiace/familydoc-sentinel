package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 核验任务主流程执行阶段枚举
 * <p>
 * 用于向前端实时同步当前所处的业务环节与进度百分比，避免页面一直处于无反馈的加载状态。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum TaskStage {

    /**
     * 阶段 1：输入清洗与规范化
     */
    SANITIZING("SANITIZING", "正在清洗与规范化输入文本...", 5),

    /**
     * 阶段 2：危急红线安全扫描
     */
    RED_FLAG_SCANNING("RED_FLAG_SCANNING", "正在执行危急重症安全扫描...", 10),

    /**
     * 阶段 3：高频缓存探测
     */
    CACHE_PROBING("CACHE_PROBING", "正在检查历史核验缓存...", 15),

    /**
     * 阶段 4：用户意图识别与过滤
     */
    INTENT_ANALYZING("INTENT_ANALYZING", "正在识别健康核验意图...", 25),

    /**
     * 阶段 5：模型步骤 1 —— 拆解可核验论断与生成中性检索词
     */
    CLAIM_EXTRACTING("CLAIM_EXTRACTING", "正在拆解文中健康说法并生成检索词...", 40),

    /**
     * 阶段 6：并行检索权威来源、立场标注与规则裁决（进度在 45% ~ 90% 之间随论断完成数动态更新）
     */
    PARALLEL_VERIFYING("PARALLEL_VERIFYING", "正在检索可信医学来源并逐条核对证据...", 60),

    /**
     * 阶段 7：生成文章级摘要与收尾
     */
    SUMMARY_GENERATING("SUMMARY_GENERATING", "正在汇总整篇核验摘要...", 95),

    /**
     * 阶段 8：流程结束
     */
    FINISHED("FINISHED", "核验结束", 100);

    private final String code;
    private final String description;
    private final int baseProgress;
}

