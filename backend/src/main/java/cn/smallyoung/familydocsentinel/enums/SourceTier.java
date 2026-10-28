package cn.smallyoung.familydocsentinel.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 证据来源分级枚举（对应需求文档 4.5 节）
 */
@Getter
@RequiredArgsConstructor
public enum SourceTier {

    /**
     * 一级来源（准入，排序等级系数 1.5）：
     * 国家与省市卫生健康部门、疾控机构、药品监管部门、世界卫生组织、白名单内学会指南与学术机构
     */
    TIER_1(1, "一级来源", 1.5, true),

    /**
     * 二级来源（准入，排序等级系数 1.2）：
     * 三甲医院与重点医科大学官网、主流媒体健康频道（标注“媒体报道”）、白名单内的专业科普机构
     */
    TIER_2(2, "二级来源", 1.2, true),

    /**
     * 零级来源（不准入，系数 0.0）：
     * 自媒体、商业健康资讯、广告类页面、其余未命中白名单的站点
     */
    TIER_0(0, "零级来源", 0.0, false);

    private final int level;
    private final String label;
    private final double weightFactor;
    private final boolean admitted;
}

