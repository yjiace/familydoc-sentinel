package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.UnverifiedItemDto;
import cn.smallyoung.familydocsentinel.enums.VerdictType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文章级一句话摘要纯规则生成服务（对应需求文档 US-06 与 4.8 节）
 * <p>
 * 核心规则（纯代码规则模板产出，严禁调用大模型，严禁使用“靠谱”、“可信度高”等判断性词汇）：
 * 1. 基础句：“共核验 n 条说法：x 条有误导，y 条有依据，z 条看情况，w 条没查实”，数量为 0 的项省略；
 * 2. 有未核验内容（k > 0）：追加“另有 k 条未纳入核验”；
 * 3. 有待重试条（r > 0）：追加“r 条暂时没查成，可点击重试”；
 * 4. 有误导条（x >= 1）：追加“文中含与依据矛盾的说法，转发前请留意”；
 * 5. 有问医生条（askDoctor > 0）：追加“文中有涉及个人用药的问题，请咨询医生或药师”。
 * </p>
 */
@Service
public class ArticleSummaryGenerator {

    /**
     * 根据已完成核验的论断列表与未核验清单生成文章级摘要
     *
     * @param claims          论断列表
     * @param unverifiedItems 未纳入核验清单
     * @return 符合 4.8 节模板规范的文章级一句话总结
     */
    public String generateSummary(List<ClaimItemDto> claims, List<UnverifiedItemDto> unverifiedItems) {
        List<ClaimItemDto> safeClaims = claims != null ? claims : List.of();
        int unverifiedCount = unverifiedItems != null ? unverifiedItems.size() : 0;

        if (safeClaims.isEmpty()) {
            return "没有发现可核验的健康说法，请检查输入内容。";
        }

        int misleadingCount = 0;  // x: 有误导
        int supportedCount = 0;   // y: 有依据
        int conditionalCount = 0; // z: 看情况
        int unconfirmedCount = 0; // w: 没查实
        int askDoctorCount = 0;   // 问医生
        int retryNeededCount = 0; // r: 待重试

        for (ClaimItemDto claim : safeClaims) {
            if (claim == null || claim.getVerdictCode() == null) {
                continue;
            }
            String v = claim.getVerdictCode();
            if (VerdictType.MISLEADING.getCode().equals(v)) {
                misleadingCount++;
            } else if (VerdictType.SUPPORTED.getCode().equals(v)) {
                supportedCount++;
            } else if (VerdictType.CONDITIONAL.getCode().equals(v)) {
                conditionalCount++;
            } else if (VerdictType.UNVERIFIED.getCode().equals(v)) {
                unconfirmedCount++;
            } else if (VerdictType.ASK_DOCTOR.getCode().equals(v)) {
                askDoctorCount++;
            } else if (VerdictType.RETRY_NEEDED.getCode().equals(v)) {
                retryNeededCount++;
            }
        }

        int totalCount = safeClaims.size();
        List<String> verdictParts = new ArrayList<>();
        if (misleadingCount > 0) {
            verdictParts.add(misleadingCount + " 条有误导");
        }
        if (supportedCount > 0) {
            verdictParts.add(supportedCount + " 条有依据");
        }
        if (conditionalCount > 0) {
            verdictParts.add(conditionalCount + " 条看情况");
        }
        if (unconfirmedCount > 0) {
            verdictParts.add(unconfirmedCount + " 条没查实");
        }
        if (askDoctorCount > 0 && verdictParts.isEmpty()) {
            verdictParts.add(askDoctorCount + " 条需问医生");
        }

        List<String> clauses = new ArrayList<>();
        if (!verdictParts.isEmpty()) {
            clauses.add("共核验 " + totalCount + " 条说法：" + String.join("，", verdictParts));
        } else {
            clauses.add("共提取 " + totalCount + " 条说法");
        }

        // 有未核验内容：追加“另有 k 条未纳入核验”
        if (unverifiedCount > 0) {
            clauses.add("另有 " + unverifiedCount + " 条未纳入核验");
        }

        // 有待重试条：追加“r 条暂时没查成，可点击重试”
        if (retryNeededCount > 0) {
            clauses.add(retryNeededCount + " 条暂时没查成，可点击重试");
        }

        // x >= 1：追加“文中含与依据矛盾的说法，转发前请留意”
        if (misleadingCount >= 1) {
            clauses.add("文中含与依据矛盾的说法，转发前请留意");
        }

        // 有问医生条：追加“文中有涉及个人用药的问题，请咨询医生或药师”
        if (askDoctorCount > 0) {
            clauses.add("文中有涉及个人用药的问题，请咨询医生或药师");
        }

        return String.join("；", clauses) + "。";
    }
}

