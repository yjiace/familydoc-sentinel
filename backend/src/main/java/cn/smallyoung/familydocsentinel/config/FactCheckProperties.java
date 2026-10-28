package cn.smallyoung.familydocsentinel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 家医智核·流言粉碎 核心业务配置属性绑定类
 * <p>
 * 对应 {@code application.yaml} 中的 {@code factcheck} 配置前缀，
 * 支持检索策略预算、证据准入白名单、危急红线阈值与合规禁用词的外部化配置。
 * </p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "factcheck")
public class FactCheckProperties {

    /**
     * 检索策略、并发与调用预算配置（对应需求文档 4.3、4.4、4.10）
     */
    private Search search = new Search();

    /**
     * 缓存与活跃任务上下文保留配置（对应需求文档 3.2、4.9、4.10）
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 危急红线生理指标阈值配置（对应需求文档 4.1）
     */
    private RedFlag redFlag = new RedFlag();

    /**
     * 一级、二级证据准入白名单配置（对应需求文档 4.5）
     */
    private Whitelist whitelist = new Whitelist();

    /**
     * 合规与禁用词控制配置（对应需求文档 4.3、4.6、4.10）
     */
    private Compliance compliance = new Compliance();

    @Data
    public static class Search {
        /**
         * 单论断最大检索次数硬上限（含重试），默认 3 次
         */
        private int maxPerClaim = 3;

        /**
         * 单次核验请求总检索次数硬上限，默认 12 次
         */
        private int maxPerRequest = 12;

        /**
         * 每日全局检索调用额度上限，默认 5000 次
         */
        private int dailyQuota = 5000;

        /**
         * 每日额度告警比例，达到该比例（默认 80%）时输出运维告警
         */
        private double quotaWarnRatio = 0.8;

        /**
         * 证据准入与充分性判定的相关度阈值初值，默认 0.5
         */
        private double relevanceThreshold = 0.5;

        /**
         * 中性检索词最大字符长度上限，超出时调用大模型语义压缩重写（最多 2 次）
         */
        private int queryMaxLength = 50;

        /**
         * 单次检索接口调用超时时间（秒），默认 3 秒
         */
        private int timeoutSeconds = 3;

        /**
         * 单条论断核验总时限（秒），默认 12 秒
         */
        private int claimTimeoutSeconds = 12;

        /**
         * 整次核验请求总时限（秒），默认 30 秒
         */
        private int taskTimeoutSeconds = 30;

        /**
         * 单次请求内论断并行核验的最大并发数，默认 3
         */
        private int concurrency = 3;

        /**
         * 是否开启学术类（acad）行业升级检索，默认关闭
         */
        private boolean enableAcademic = false;
    }

    @Data
    public static class CacheConfig {
        /**
         * 活跃任务状态与重试上下文在内存中的保留时长（分钟），默认 60 分钟
         */
        private int activeTaskTtlMinutes = 60;

        /**
         * 完整核验结果缓存保留时长（小时），默认 24 小时
         */
        private int resultCacheTtlHours = 24;
    }

    @Data
    public static class RedFlag {
        /**
         * 收缩压危急阈值（mmHg），默认 180
         */
        private int sbpThreshold = 180;

        /**
         * 舒张压危急阈值（mmHg），默认 110
         */
        private int dbpThreshold = 110;

        /**
         * 血糖高值危急阈值（mmol/L），默认 16.7
         */
        private double glucoseHighThreshold = 16.7;

        /**
         * 血糖低值危急阈值（mmol/L），默认 2.8
         */
        private double glucoseLowThreshold = 2.8;
    }

    @Data
    public static class Whitelist {
        /**
         * 一级来源域名白名单：国家与省市卫健部门、疾控、药监、世卫组织、白名单学会与学术机构
         */
        private List<String> tier1Domains = new ArrayList<>();

        /**
         * 一级来源站点名称关键词
         */
        private List<String> tier1SiteKeywords = new ArrayList<>();

        /**
         * 二级来源 - 三甲医院与重点医科大学官网域名
         */
        private List<String> tier2HospitalDomains = new ArrayList<>();

        /**
         * 二级来源 - 三甲医院与重点医科大学站点名称关键词
         */
        private List<String> tier2HospitalSiteKeywords = new ArrayList<>();

        /**
         * 二级来源 - 主流媒体健康频道域名（界面标注“媒体报道”）
         */
        private List<String> tier2MediaDomains = new ArrayList<>();

        /**
         * 二级来源 - 主流媒体站点名称关键词（界面标注“媒体报道”）
         */
        private List<String> tier2MediaSiteKeywords = new ArrayList<>();

        /**
         * 二级来源 - 白名单内专业科普机构域名
         */
        private List<String> tier2ScienceDomains = new ArrayList<>();

        /**
         * 二级来源 - 白名单内专业科普机构站点名称关键词
         */
        private List<String> tier2ScienceSiteKeywords = new ArrayList<>();
    }

    @Data
    public static class Compliance {
        /**
         * 中性检索词禁止包含的词汇列表（如“辟谣”“谣言”“骗局”及特定机构名）
         */
        private List<String> queryForbiddenWords = new ArrayList<>();

        /**
         * 用户可见说明文字禁用词及對應中性替换词映射
         */
        private Map<String, String> explanationForbiddenReplacements = new LinkedHashMap<>();
    }
}

