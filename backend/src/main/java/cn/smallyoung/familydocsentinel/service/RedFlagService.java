package cn.smallyoung.familydocsentinel.service;

import cn.smallyoung.familydocsentinel.config.FactCheckProperties;
import cn.smallyoung.familydocsentinel.dto.RedFlagResult;
import cn.smallyoung.familydocsentinel.dto.RedFlagResult.Category;
import cn.smallyoung.familydocsentinel.dto.RedFlagResult.Level;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 危急红线双重扫描服务（对应需求文档 US-02、3.3、4.1、6 节）
 * <p>
 * 核心规则：
 * 1. 只拦截“本人或家人此刻正在发生的危急情况”，不拦截“文章里提到了急症”（如“拍打手肘能急救心肌梗死”必须能进入核验）；
 * 2. 命中危急信号 + 同时命中当前状态线索 -> 拦截级（HARD）：阻断外部调用，展示红色警报与 120 提示；
 * 3. 命中危急信号 + 无当前状态线索（或用户确认“这只是文章里的内容”降级）-> 提示级（WARN）：顶部黄色提醒条，照常核验；
 * 4. 未命中 -> 照常核验（NONE）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedFlagService {

    private final FactCheckProperties properties;

    /**
     * 内部命中记录
     */
    private record Hit(Category category, String rule) {
    }

    // ===================== 固定文案（严格对齐需求文档 4.1 节） =====================
    public static final String HARD_ALERT_MESSAGE = "检测到突发危急重症高危信号，请立即就近急诊就医或拨打 120 急救电话，切勿依赖网络自查！";

    public static final String WARN_ALERT_MESSAGE = "文中提到了可能危急的症状。如果您或家人此刻正有类似情况，请立即就医或拨打 120，不要依赖网络自查。";

    // ===================== 血压正则 =====================
    /**
     * 匹配口语血压写法如 "180/110"（前后不能紧挨数字、斜杠、小数点，避免日期 2024/12/01 误判）
     */
    private static final Pattern BP_SLASH = Pattern.compile("(?<![\\d/.])(\\d{2,3})\\s*/\\s*(\\d{2,3})(?![\\d/.])");

    /**
     * 匹配收缩压口语写法如 "高压 180" / "收缩压为190" / "血压飙到185"
     */
    private static final Pattern BP_HIGH = Pattern
            .compile("(高压|收缩压|血压)\\s*(?:[:：为达到有是飙升至到]|超过|高于|大于|>=|≥)*\\s*(\\d{2,3})(?!\\d)");

    /**
     * 匹配舒张压口语写法如 "低压 110" / "舒张压 115"
     */
    private static final Pattern BP_LOW = Pattern
            .compile("(低压|舒张压)\\s*(?:[:：为达到有是升至到]|超过|高于|大于|>=|≥)*\\s*(\\d{2,3})(?!\\d)");

    // ===================== 血糖正则 =====================
    /**
     * 匹配血糖口语写法如 "空腹血糖 17.2" / "血糖 2.5"（排除 "餐后2小时" 等时间表述）
     */
    private static final Pattern GLUCOSE = Pattern
            .compile("(?:空腹|餐后|随机)?血糖\\s*(?:[:：为达到有是降到升到]|超过|高于|大于|低于|小于|>=|<=|≥|≤)*\\s*"
                    + "(\\d{1,3}+(?:\\.\\d++)?+)(?!\\s*(?:小时|分钟|h|H|min))");

    /**
     * 血糖超过该值视为 mg/dL 单位（mmol/L 极少超过 33.3），自动按 18.0 折算
     */
    private static final double MG_DL_THRESHOLD = 33.3;
    private static final double MG_DL_PER_MMOL = 18.0;

    // ===================== 危急急症症状词表（对应需求 4.1 节） =====================
    private static final List<String> CRITICAL_SYMPTOMS = List.of(
            "剧烈胸痛", "持续胸痛", "压榨样胸痛", "胸口压榨", "胸痛大汗", "大汗淋漓",
            "突发意识障碍", "意识障碍", "昏迷", "神志不清", "不省人事", "叫不醒",
            "言语不清", "口齿不清", "无法说话", "嘴歪眼斜", "口角歪斜",
            "单侧肢体无力", "一侧手脚无力", "偏瘫", "肢体瘫痪", "半身不遂",
            "严重呼吸困难", "呼吸极度困难", "喘不上气", "紫绀", "大咯血", "剧烈咯血", "大口吐血", "呕血",
            "突发爆炸样剧烈头痛", "突发剧烈爆炸样头痛", "爆炸样头痛", "剧烈头痛欲裂",
            "急性心肌梗死", "心肌梗死", "急性心梗", "脑出血", "急性脑梗",
            "全身抽搐", "抽搐不止", "想自杀", "不想活了", "轻生");

    /**
     * 症状词之前出现否定词时视为否定表述（如“没有胸痛”、“未出现昏迷”）
     */
    private static final Pattern NEGATION = Pattern.compile("(没有|没|无|不是|并非|未出现|否认|不曾|不会)[^，。；！？,.;!?]{0,4}$");
    private static final int NEGATION_LOOKBEHIND = 8;

    // ===================== 当前状态线索（对应需求 4.1 节） =====================
    /**
     * 第一人称或家人称谓（“我”已涵盖“我爸/我妈/我老伴/我爷/我奶”等组合）
     */
    private static final List<String> PERSON_CLUES = List.of(
            "我", "老伴", "爷爷", "奶奶", "外公", "外婆", "父亲", "母亲", "爸爸", "妈妈", "老公", "老婆", "孩子", "家里人");

    /**
     * 现时状态词
     */
    private static final List<String> TIME_CLUES = List.of(
            "现在", "刚刚", "刚才", "突然", "正在", "此刻", "这会儿", "今天突然", "突然出现");

    /**
     * 求助句式线索
     */
    private static final List<String> HELP_CLUES = List.of(
            "该怎么办", "怎么办", "要不要去医院", "要送医院吗", "需要去急诊吗", "求救", "救命", "救救我");

    /**
     * 极端紧急求救词（即使省略人称也视为当前危急状态）
     */
    private static final List<String> URGENT_CLUES = List.of("救命", "救救我", "快救救");

    // ===================== 扫描入口 =====================

    /**
     * 执行危急红线扫描（默认不降级 HARD 拦截）
     *
     * @param text 用户输入文本
     * @return 红线判定结果
     */
    public RedFlagResult scan(String text) {
        return scan(text, false);
    }

    /**
     * 执行危急红线扫描（支持用户点击“这只是文章里的内容（继续核验）”降级）
     *
     * @param text              用户输入文本
     * @param ignoreHardRedFlag 若为 true，则当命中 HARD 级时自动降为 WARN 提示级并继续核验
     * @return 红线判定结果
     */
    public RedFlagResult scan(String text, boolean ignoreHardRedFlag) {
        if (text == null || text.isBlank()) {
            return RedFlagResult.none();
        }

        // 依次检测血压、血糖、危急急症症状
        Optional<Hit> bp = detectBloodPressure(text);
        Optional<Hit> glucose = detectGlucose(text);
        Optional<Hit> symptom = detectSymptom(text);

        Optional<Hit> primary = bp.isPresent() ? bp : glucose.isPresent() ? glucose : symptom;
        if (primary.isEmpty()) {
            return RedFlagResult.none();
        }

        Hit hit = primary.get();
        boolean currentState = hasCurrentStateClues(text);
        Level level;
        if (currentState && !ignoreHardRedFlag) {
            level = Level.HARD;
        } else {
            level = Level.WARN;
        }

        // 遵循隐私安全要求：日志仅记录命中级别、分类与规则名，不记录用户健康原文
        log.warn("RedFlag hit: level={}, category={}, rule={}, downgraded={}",
                level, hit.category(), hit.rule(), currentState && ignoreHardRedFlag);

        String message = (level == Level.HARD) ? HARD_ALERT_MESSAGE : WARN_ALERT_MESSAGE;
        return RedFlagResult.of(level, hit.category(), message);
    }

    // ===================== 细分指标与症状检测 =====================

    private Optional<Hit> detectBloodPressure(String text) {
        int sbpThreshold = properties != null && properties.getRedFlag() != null
                ? properties.getRedFlag().getSbpThreshold()
                : 180;
        int dbpThreshold = properties != null && properties.getRedFlag() != null
                ? properties.getRedFlag().getDbpThreshold()
                : 110;

        Matcher slash = BP_SLASH.matcher(text);
        while (slash.find()) {
            int sbp = Integer.parseInt(slash.group(1));
            int dbp = Integer.parseInt(slash.group(2));
            // 生理合理性校验：收缩压 > 舒张压，且在合理范围内
            if (sbp > dbp && sbp <= 300 && dbp >= 30 && (sbp >= sbpThreshold || dbp >= dbpThreshold)) {
                return Optional.of(new Hit(Category.BP, "bp_slash: sbp>=" + sbpThreshold + " or dbp>=" + dbpThreshold));
            }
        }
        if (anyMatch(BP_HIGH, text, v -> v >= sbpThreshold && v <= 300)) {
            return Optional.of(new Hit(Category.BP, "sbp>=" + sbpThreshold));
        }
        if (anyMatch(BP_LOW, text, v -> v >= dbpThreshold && v <= 200)) {
            return Optional.of(new Hit(Category.BP, "dbp>=" + dbpThreshold));
        }
        return Optional.empty();
    }

    private Optional<Hit> detectGlucose(String text) {
        double highThreshold = properties != null && properties.getRedFlag() != null
                ? properties.getRedFlag().getGlucoseHighThreshold()
                : 16.7;
        double lowThreshold = properties != null && properties.getRedFlag() != null
                ? properties.getRedFlag().getGlucoseLowThreshold()
                : 2.8;

        Matcher m = GLUCOSE.matcher(text);
        while (m.find()) {
            double raw = Double.parseDouble(m.group(1));
            if (raw <= 0) {
                continue;
            }
            // 兼容 mg/dL：超过 33.3 时折算为 mmol/L
            double mmol = raw > MG_DL_THRESHOLD ? raw / MG_DL_PER_MMOL : raw;
            if (mmol >= highThreshold || mmol <= lowThreshold) {
                return Optional.of(new Hit(Category.GLUCOSE,
                        "glucose>=" + highThreshold + " or <=" + lowThreshold + " mmol/L"));
            }
        }
        return Optional.empty();
    }

    private Optional<Hit> detectSymptom(String text) {
        for (String symptom : CRITICAL_SYMPTOMS) {
            int idx = text.indexOf(symptom);
            while (idx >= 0) {
                String prefix = text.substring(Math.max(0, idx - NEGATION_LOOKBEHIND), idx);
                if (!NEGATION.matcher(prefix).find()) {
                    return Optional.of(new Hit(Category.SYMPTOM, "symptom:" + symptom));
                }
                idx = text.indexOf(symptom, idx + symptom.length());
            }
        }
        return Optional.empty();
    }

    /**
     * 判断是否命中“本人或家人此刻正在发生”的当前状态线索（对应需求 4.1 节）
     * <p>
     * 规则：必须在同一个分句或邻近上下文（或整段短求助）中存在：
     * 极端求救词，或（第一人称/家人称谓 + 现时词），或（第一人称/家人称谓 + 求助句式）。
     * 纯转述科普口吻（如“拍打手肘能急救心肌梗死”）返回 false。
     * </p>
     */
    boolean hasCurrentStateClues(String text) {
        if (containsAny(text, URGENT_CLUES)) {
            return true;
        }
        // 按句切分检查，防止长科普文章不同段落偶然分别出现“我”和“突然”导致误拦截（需求第6节：转述内容误拦截 <=10%）
        String[] sentences = text.split("[。！？!?\\r\\n]+");
        for (String sentence : sentences) {
            if (sentence.isBlank()) {
                continue;
            }
            if (containsAny(sentence, PERSON_CLUES)
                    && (containsAny(sentence, TIME_CLUES) || containsAny(sentence, HELP_CLUES))) {
                return true;
            }
        }
        // 若全文较短（<=80字，典型手工输入求助），做一次跨句组合判定
        if (text.length() <= 80 && containsAny(text, PERSON_CLUES)) {
            return containsAny(text, TIME_CLUES) || containsAny(text, HELP_CLUES);
        }
        return false;
    }

    // ===================== 内部工具方法 =====================

    private static boolean anyMatch(Pattern pattern, String text, IntPredicate cond) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            if (cond.test(Integer.parseInt(m.group(2)))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String text, List<String> keywords) {
        for (String k : keywords) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }
}
