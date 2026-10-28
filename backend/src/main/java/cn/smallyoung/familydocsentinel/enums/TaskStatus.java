package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 核验任务整体状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum TaskStatus {

    /**
     * 任务正在后台执行中（前端可根据 stage 与 claims 状态实时刷新进度）
     */
    PROCESSING("PROCESSING", "正在核验中"),

    /**
     * 核验任务已执行完毕（所有论断已产出结论或标记为待重试）
     */
    COMPLETED("COMPLETED", "核验已完成"),

    /**
     * 任务被前置规则阻断（如命中危急红线 HARD 拦截级、非健康意图拦截或未提取到可核验论断）
     */
    BLOCKED("BLOCKED", "已拦截提示"),

    /**
     * 用户主动取消核验任务（对应需求 US-01“核验中可取消”）
     */
    CANCELLED("CANCELLED", "核验已取消"),

    /**
     * 核验任务因系统或额度异常终止
     */
    FAILED("FAILED", "核验服务暂时异常");

    private final String code;
    private final String description;
}

