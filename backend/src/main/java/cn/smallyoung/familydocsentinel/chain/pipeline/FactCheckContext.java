package cn.smallyoung.familydocsentinel.chain.pipeline;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import cn.smallyoung.familydocsentinel.enums.ClaimVerifyStage;
import cn.smallyoung.familydocsentinel.enums.TaskStage;
import cn.smallyoung.familydocsentinel.enums.TaskStatus;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 核验任务主流程责任链上下文（贯穿第一层主流程 7 个处理器节点）
 * <p>
 * 负责维护任务全生命周期的输入、中间清洗产物、检索预算计数器、取消令牌，
 * 并提供线程安全的实时阶段进度与单论断检索状态同步方法，供前端轮询接口实时读取。
 * </p>
 */
@Data
public class FactCheckContext {

    /**
     * 任务唯一标识
     */
    private final String taskId;

    /**
     * 原始输入文本
     */
    private final String rawInput;

    /**
     * 用户是否确认“这只是文章里的内容（继续核验）”（降级 HARD 红线为 WARN）
     */
    private final boolean ignoreHardRedFlag;

    /**
     * 是否跳过缓存强制重新核验
     */
    private final boolean skipCache;

    /**
     * 去除首尾空白并合并连续空行后的规范化原文
     */
    private String normalizedRawText;

    /**
     * 去除转发噪音后的净文本
     */
    private String cleanText;

    /**
     * 规范化全文 SHA-256 指纹哈希
     */
    private String queryHash;

    /**
     * 本次请求已消耗的外部检索次数计数器（硬上限 12 次）
     */
    private final AtomicInteger requestSearchBudgetUsed = new AtomicInteger(0);

    /**
     * 本次任务累计消耗的大模型 Token 总数
     */
    private final AtomicInteger tokensUsed = new AtomicInteger(0);

    /**
     * 任务取消令牌（对应需求 US-01“核验中可取消”）
     */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 当前提交任务的用户 ID（未登录为 null）
     */
    private Long userId;

    /**
     * 面向前端实时查询的任务状态与结果响应对象
     */
    private final FactCheckTaskResponse response;

    public FactCheckContext(String taskId, String rawInput, boolean ignoreHardRedFlag, boolean skipCache) {
        this(taskId, rawInput, ignoreHardRedFlag, skipCache, null);
    }

    public FactCheckContext(String taskId, String rawInput, boolean ignoreHardRedFlag, boolean skipCache, Long userId) {
        this.taskId = taskId;
        this.rawInput = rawInput;
        this.ignoreHardRedFlag = ignoreHardRedFlag;
        this.skipCache = skipCache;
        this.userId = userId;
        this.response = FactCheckTaskResponse.builder()
                .taskId(taskId)
                .status(TaskStatus.PROCESSING.getCode())
                .statusDescription(TaskStatus.PROCESSING.getDescription())
                .stage(TaskStage.SANITIZING.getCode())
                .stageDescription(TaskStage.SANITIZING.getDescription())
                .progressPercent(TaskStage.SANITIZING.getBaseProgress())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 检查当前任务是否已被用户取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 标记任务为已取消状态
     */
    public synchronized void markCancelled() {
        if (TaskStatus.COMPLETED.getCode().equals(response.getStatus())
                || TaskStatus.BLOCKED.getCode().equals(response.getStatus())) {
            return;
        }
        cancelled.set(true);
        response.setStatus(TaskStatus.CANCELLED.getCode());
        response.setStatusDescription(TaskStatus.CANCELLED.getDescription());
        response.setStage(TaskStage.FINISHED.getCode());
        response.setStageDescription("用户已取消本次核验");
        response.setAllowClientCache(false);
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 更新主流程当前执行阶段与基础进度
     */
    public synchronized void updateStage(TaskStage stage) {
        if (isCancelled()) {
            return;
        }
        response.setStage(stage.getCode());
        response.setStageDescription(stage.getDescription());
        response.setProgressPercent(stage.getBaseProgress());
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 更新主流程阶段及自定义描述与进度
     */
    public synchronized void updateStage(TaskStage stage, String customDescription, int progressPercent) {
        if (isCancelled()) {
            return;
        }
        response.setStage(stage.getCode());
        response.setStageDescription(customDescription);
        response.setProgressPercent(Math.clamp(progressPercent, 0, 100));
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 当单条论断的检索/核验子阶段发生变化时，实时同步单条状态并重新计算整体检索进度与提示文案
     */
    public synchronized void updateClaimStage(ClaimItemDto claim, ClaimVerifyStage verifyStage) {
        if (claim == null || verifyStage == null) {
            return;
        }
        claim.setVerifyStageCode(verifyStage.getCode());
        claim.setVerifyStageText(verifyStage.getStatusText());
        refreshParallelVerifyProgress();
    }

    /**
     * 根据所有论断的当前子阶段，动态计算并同步任务整体进度百分比（45% ~ 90%）与实时检索状态文案
     */
    public synchronized void refreshParallelVerifyProgress() {
        if (isCancelled()) {
            return;
        }
        List<ClaimItemDto> claims = response.getClaims();
        if (claims == null || claims.isEmpty()) {
            return;
        }
        int total = claims.size();
        int finishedCount = 0;
        double stageScoreSum = 0.0;
        String activeClaimDetail = null;

        for (ClaimItemDto c : claims) {
            String stageCode = c.getVerifyStageCode();
            if (ClaimVerifyStage.COMPLETED.getCode().equals(stageCode)
                    || ClaimVerifyStage.RETRY_NEEDED.getCode().equals(stageCode)) {
                finishedCount++;
                stageScoreSum += 1.0;
            } else {
                if (ClaimVerifyStage.PRIMARY_SEARCHING.getCode().equals(stageCode)) {
                    stageScoreSum += 0.25;
                } else if (ClaimVerifyStage.UPGRADE_SEARCHING.getCode().equals(stageCode)) {
                    stageScoreSum += 0.45;
                } else if (ClaimVerifyStage.EVIDENCE_ADMITTING.getCode().equals(stageCode)) {
                    stageScoreSum += 0.60;
                } else if (ClaimVerifyStage.STANCE_ANNOTATING.getCode().equals(stageCode)) {
                    stageScoreSum += 0.80;
                } else if (ClaimVerifyStage.ADJUDICATING.getCode().equals(stageCode)) {
                    stageScoreSum += 0.90;
                }
                if (activeClaimDetail == null && c.getVerifyStageText() != null) {
                    activeClaimDetail = c.getVerifyStageText();
                }
            }
        }

        response.setTotalClaimCount(total);
        response.setCompletedClaimCount(finishedCount);

        if (TaskStage.PARALLEL_VERIFYING.getCode().equals(response.getStage())) {
            int dynamicProgress = 45 + (int) Math.round((stageScoreSum / total) * 45.0);
            response.setProgressPercent(Math.clamp(dynamicProgress, 45, 92));
            String desc = "正在核验第 " + Math.min(finishedCount + 1, total) + "/" + total + " 条说法（已完成 "
                    + finishedCount + "/" + total + "）"
                    + (activeClaimDetail != null ? "：" + activeClaimDetail : "");
            response.setStageDescription(desc);
        }
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 标记任务因前置规则（如危急红线 HARD 拦截、非健康意图、无有效论断）被拦截终止
     */
    public synchronized void markBlocked(String noticeMessage) {
        response.setStatus(TaskStatus.BLOCKED.getCode());
        response.setStatusDescription(TaskStatus.BLOCKED.getDescription());
        response.setStage(TaskStage.FINISHED.getCode());
        response.setStageDescription(noticeMessage != null ? noticeMessage : "已拦截提示");
        response.setProgressPercent(100);
        response.setNoticeMessage(noticeMessage);
        response.setAllowClientCache(false);
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 标记任务因系统或额度异常失败
     */
    public synchronized void markFailed(String errorMessage) {
        response.setStatus(TaskStatus.FAILED.getCode());
        response.setStatusDescription(TaskStatus.FAILED.getDescription());
        response.setStage(TaskStage.FINISHED.getCode());
        response.setStageDescription(errorMessage);
        response.setProgressPercent(100);
        response.setNoticeMessage(errorMessage);
        response.setAllowClientCache(false);
        response.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * 标记任务全部核验完成，并根据是否存在“待重试”条目计算 allowClientCache
     */
    public synchronized void markCompleted() {
        List<ClaimItemDto> claims = response.getClaims();
        int total = claims != null ? claims.size() : 0;
        boolean hasRetryNeeded = claims != null && claims.stream()
                .anyMatch(c -> VerdictType.RETRY_NEEDED.getCode().equals(c.getVerdictCode()) || c.isRetryable());

        response.setTotalClaimCount(total);
        response.setCompletedClaimCount(total);
        response.setStatus(TaskStatus.COMPLETED.getCode());
        response.setStatusDescription(TaskStatus.COMPLETED.getDescription());
        response.setStage(TaskStage.FINISHED.getCode());
        response.setStageDescription("核验已完成");
        response.setProgressPercent(100);
        // 需求 4.9 节：存在待重试条目时，整次结果不写入缓存
        response.setAllowClientCache(total > 0 && !hasRetryNeeded);
        response.setUpdatedAt(LocalDateTime.now());
    }
}

