package cn.smallyoung.familydocsentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 流言粉碎核验任务全量状态与结果响应 DTO
 * <p>
 * 同时服务于：
 * 1. 创建任务接口的初始状态返回；
 * 2. 前端轮询接口的实时检索进度、阶段描述、逐条论断状态同步及最终核验结果展示。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactCheckTaskResponse implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页面底部常驻免责声明（对应需求 4.10 节）
     */
    public static final String CONSTANT_DISCLAIMER =
            "比对信息仅供健康参考，不可作为处方或诊疗依据；内容由 AI 辅助生成，非官方结论。";

    /**
     * 统一隐私表述（对应需求 4.10 节，不使用“零留存”等绝对说法）
     */
    public static final String PRIVACY_STATEMENT =
            "本服务端不保存您的输入与核验结果；第三方服务遵循其各自条款";

    /**
     * 任务唯一 ID
     */
    private String taskId;

    /**
     * 任务整体状态编码：PROCESSING / COMPLETED / BLOCKED / CANCELLED / FAILED
     */
    private String status;

    /**
     * 任务整体状态说明
     */
    private String statusDescription;

    /**
     * 当前所处主流程阶段编码：SANITIZING / RED_FLAG_SCANNING / CACHE_PROBING /
     * INTENT_ANALYZING / CLAIM_EXTRACTING / PARALLEL_VERIFYING / SUMMARY_GENERATING / FINISHED
     */
    private String stage;

    /**
     * 当前所处主流程阶段实时提示文案（供前端实时展示进度，避免页面一直处于盲等加载中）
     */
    private String stageDescription;

    /**
     * 整体进度百分比（0 ~ 100）
     */
    private int progressPercent;

    /**
     * 已完成核验（含已出结论或待重试）的论断数量
     */
    private int completedClaimCount;

    /**
     * 纳入核验的论断总数量（上限 5 条）
     */
    private int totalClaimCount;

    /**
     * 规范化全文 SHA-256 指纹哈希（供前端作 24 小时本地缓存 Key）
     */
    private String queryHash;

    /**
     * 本次结果是否命中缓存
     */
    private boolean cacheHit;

    /**
     * 是否允许前端写入 24 小时浏览器本地缓存
     * （对应需求 4.9 节：存在“待重试”条目时为 false，整次结果不写入缓存）
     */
    private boolean allowClientCache;

    /**
     * 危急红线扫描结果（NONE / WARN 黄色提示条 / HARD 红色拦截警报）
     */
    private RedFlagResult redFlag;

    /**
     * 用户意图识别结果（当非健康或问候被拦截时提供友好引导语）
     */
    private IntentAnalysisResult intentResult;

    /**
     * 文章级一句话总结（对应需求 US-06 与 4.8 节：由纯规则模板生成，不使用“靠谱/可信度高”等词）
     */
    private String articleSummary;

    /**
     * 业务提示或异常提示文案（如“没有发现可核验的健康说法，请检查输入内容”、“今日核验额度已用完，请明天再试”）
     */
    private String noticeMessage;

    /**
     * 逐条可核验论断结果列表（每条完成即更新状态与印章，前端轮询时可渐进式渲染已完成卡片）
     */
    @Builder.Default
    private List<ClaimItemDto> claims = new ArrayList<>();

    /**
     * 未纳入核验的内容折叠清单（对应需求 US-08 与 4.2 节）
     */
    @Builder.Default
    private List<UnverifiedItemDto> unverifiedItems = new ArrayList<>();

    /**
     * 页面底部常驻免责声明
     */
    @Builder.Default
    private String disclaimer = CONSTANT_DISCLAIMER;

    /**
     * 隐私与数据说明
     */
    @Builder.Default
    private String privacyStatement = PRIVACY_STATEMENT;

    /**
     * 任务最后更新时间
     */
    private LocalDateTime updatedAt;
}

