package cn.smallyoung.familydocsentinel.chain.adjudication;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.dto.StampDto;
import cn.smallyoung.familydocsentinel.enums.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 裁决规则通用计算与结论填充辅助工具类（对应需求文档 4.7 节）
 */
public final class AdjudicationSupport {

    private AdjudicationSupport() {
    }

    /**
     * 判断同向有效来源是否达到“定论强度”（对应需求 4.7 节）
     * <p>
     * 定论强度标准：同向有效来源中，至少 1 条一级来源（TIER_1），或来自至少 2 个不同域名。
     * </p>
     */
    public static boolean hasConclusivenessStrength(List<EvidenceItemDto> sameDirectionEvidences) {
        if (sameDirectionEvidences == null || sameDirectionEvidences.isEmpty()) {
            return false;
        }
        if (hasTier1Source(sameDirectionEvidences)) {
            return true;
        }
        return countDistinctDomains(sameDirectionEvidences) >= 2;
    }

    /**
     * 判断证据列表中是否包含至少 1 条一级来源（TIER_1）
     */
    public static boolean hasTier1Source(List<EvidenceItemDto> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return false;
        }
        for (EvidenceItemDto ev : evidences) {
            if (ev != null && SourceTier.TIER_1.name().equals(ev.getTierCode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 统计证据列表中不同主域名的数量
     */
    public static int countDistinctDomains(List<EvidenceItemDto> evidences) {
        if (evidences == null || evidences.isEmpty()) {
            return 0;
        }
        Set<String> domains = evidences.stream()
                .filter(Objects::nonNull)
                .map(ev -> ev.getDomain() != null && !ev.getDomain().isBlank() ? ev.getDomain() : ev.getUrl())
                .filter(d -> d != null && !d.isBlank())
                .collect(Collectors.toSet());
        return domains.size();
    }

    /**
     * 计算“有依据（SUPPORTED）”或“有误导（MISLEADING）”的置信度（对应需求 4.7 节）
     * <p>
     * 规则：同向来源来自 >=2 个不同域名 且 含 >=1 条一级来源为“高（HIGH）”，否则为“中（MEDIUM）”。
     * 若 forceMedium 为 true（例如“支持”与“缺乏证据”并存），则固定返回“中（MEDIUM）”。
     * </p>
     */
    public static ConfidenceLevel computeConfidence(List<EvidenceItemDto> sameDirectionEvidences, boolean forceMedium) {
        if (forceMedium) {
            return ConfidenceLevel.MEDIUM;
        }
        boolean hasTier1 = hasTier1Source(sameDirectionEvidences);
        int distinctDomains = countDistinctDomains(sameDirectionEvidences);
        return (hasTier1 && distinctDomains >= 2) ? ConfidenceLevel.HIGH : ConfidenceLevel.MEDIUM;
    }

    /**
     * 填充確定性有效结论（有依据 / 有误导 / 看情况）到 ClaimItemDto
     */
    public static void applyDefiniteVerdict(
            ClaimAdjudicationContext context,
            VerdictType verdict,
            ConfidenceLevel confidence,
            List<EvidenceItemDto> primaryEvidences,
            String defaultExplanation) {

        ClaimItemDto claim = context.getClaim();
        claim.setVerifyStageCode(ClaimVerifyStage.COMPLETED.getCode());
        claim.setVerifyStageText(ClaimVerifyStage.COMPLETED.getStatusText());
        claim.setVerdictCode(verdict.getCode());
        claim.setColloquialVerdict(verdict.getColloquialText());
        claim.setRigorousVerdict(verdict.getRigorousText());
        claim.setStamp(StampDto.fromVerdict(verdict));
        claim.setConfidenceCode(confidence.getCode());
        claim.setConfidenceLabel(confidence.getLabel());

        String explanation = context.getValidatedExplanation();
        claim.setExplanation((explanation != null && !explanation.isBlank()) ? explanation : defaultExplanation);
        claim.setEvidences(primaryEvidences != null ? primaryEvidences : List.of());
        claim.setOpposingEvidences(context.getOpposingEvidences() != null ? context.getOpposingEvidences() : List.of());
        claim.setExtraNote(context.getExtraNote());
        claim.setRetryable(false);
        ensureDrugSafetyNotice(claim);
    }

    /**
     * 填充“没查实（UNVERIFIED）”结论及五类细分原因到 ClaimItemDto
     * <p>
     * 遵循需求 US-04 与 4.6 节：
     * - 明示“没查实不代表是真的，也不代表是假的”；
     * - 不输出任何医学建议。
     * </p>
     */
    public static void applyUnverifiedVerdict(
            ClaimAdjudicationContext context,
            UnverifiedCause cause,
            List<EvidenceItemDto> primaryEvidences,
            List<EvidenceItemDto> opposingEvidences) {

        ClaimItemDto claim = context.getClaim();
        VerdictType verdict = VerdictType.UNVERIFIED;
        claim.setVerifyStageCode(ClaimVerifyStage.COMPLETED.getCode());
        claim.setVerifyStageText(ClaimVerifyStage.COMPLETED.getStatusText());
        claim.setVerdictCode(verdict.getCode());
        claim.setColloquialVerdict(verdict.getColloquialText());
        claim.setRigorousVerdict(verdict.getRigorousText());
        claim.setStamp(StampDto.fromVerdict(verdict));
        claim.setConfidenceCode(ConfidenceLevel.NONE.getCode());
        claim.setConfidenceLabel(ConfidenceLevel.NONE.getLabel());

        claim.setUnverifiedCauseCode(cause.getCode());
        claim.setUnverifiedCauseLabel(cause.getLabel());
        claim.setUnverifiedCauseMessage(cause.getUserMessage());
        claim.setUnverifiedDisclaimer(UnverifiedCause.UNVERIFIED_DISCLAIMER);

        // 没查实时不输出任何医学建议，说明文字使用客观原因说明或已核实的纯引文转述
        String explanation = cause.getUserMessage() + "。" + UnverifiedCause.UNVERIFIED_DISCLAIMER + "。";
        if ((cause == UnverifiedCause.WEAK_EVIDENCE || cause == UnverifiedCause.CURRENTLY_LACK_EVIDENCE)
                && context.getValidatedExplanation() != null && !context.getValidatedExplanation().isBlank()) {
            explanation = cause.getUserMessage() + "（" + context.getValidatedExplanation() + "）";
        }
        claim.setExplanation(explanation);
        claim.setEvidences(primaryEvidences != null ? primaryEvidences : List.of());
        claim.setOpposingEvidences(opposingEvidences != null ? opposingEvidences : List.of());
        claim.setExtraNote(context.getExtraNote());
        claim.setRetryable(false);
        ensureDrugSafetyNotice(claim);
    }

    /**
     * 填充药物类论断固定提示（对应需求 4.7 节：药物类论断无论结论为何，卡片均附药物相关提示）
     */
    public static void ensureDrugSafetyNotice(ClaimItemDto claim) {
        if (ClaimCategory.DRUG.getCode().equals(claim.getCategoryCode())) {
            claim.setDrugSafetyNotice(ClaimItemDto.DRUG_SAFETY_NOTICE);
        }
    }

    /**
     * 追加附注信息
     */
    public static void appendExtraNote(ClaimAdjudicationContext context, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        String existing = context.getExtraNote();
        if (existing == null || existing.isBlank()) {
            context.setExtraNote(note);
        } else if (!existing.contains(note)) {
            context.setExtraNote(existing + "；" + note);
        }
    }
}

