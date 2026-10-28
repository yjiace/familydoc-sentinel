package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 单条论断实时检索与核验进度状态枚举
 * <p>
 * 前端轮询任务状态时，可根据此枚举实时展示每条健康说法当前正在执行的检索或核验动作。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum ClaimVerifyStage {

    /**
     * 排队等待进入核验线程
     */
    PENDING("PENDING", "等待检索核对..."),

    /**
     * 正在调用元宝搜索执行混合模式主检索（4.4 主检索）
     */
    PRIMARY_SEARCHING("PRIMARY_SEARCHING", "正在检索权威医学与科普来源..."),

    /**
     * 主检索证据不充分，正在限定政府类（gov）行业执行升级检索（4.4 升级检索）
     */
    UPGRADE_SEARCHING("UPGRADE_SEARCHING", "正在升级检索卫健与疾控部门来源..."),

    /**
     * 正在对检索结果执行去重、白名单分级准入与排序截取（4.5 证据整理）
     */
    EVIDENCE_ADMITTING("EVIDENCE_ADMITTING", "正在筛选一级/二级可信出处..."),

    /**
     * 正在调用大模型对前 4 条准入证据进行立场标注与引文提取，并执行四项硬校验（4.6 立场标注与校验）
     */
    STANCE_ANNOTATING("STANCE_ANNOTATING", "正在比对来源摘要立场并核验引文..."),

    /**
     * 正在按固定顺序规则产出最终核验结论（4.7 规则裁决）
     */
    ADJUDICATING("ADJUDICATING", "正在根据规则生成核验结论..."),

    /**
     * 该条论断已核验完成并生成印章结论（4.9 有结论）
     */
    COMPLETED("COMPLETED", "核验完成"),

    /**
     * 因检索超时、被限流、服务或模型异常未能形成结论（4.9 待重试）
     */
    RETRY_NEEDED("RETRY_NEEDED", "这条暂时没查成");

    private final String code;
    private final String statusText;
}

