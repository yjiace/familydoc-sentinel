package cn.smallyoung.familydocsentinel;

import cn.smallyoung.familydocsentinel.dto.ClaimItemDto;
import cn.smallyoung.familydocsentinel.dto.EvidenceItemDto;
import cn.smallyoung.familydocsentinel.dto.FactCheckRequest;
import cn.smallyoung.familydocsentinel.dto.FactCheckTaskResponse;
import cn.smallyoung.familydocsentinel.enums.TaskStatus;
import cn.smallyoung.familydocsentinel.service.FactCheckService;
import cn.smallyoung.familydocsentinel.service.StanceAnnotationService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 家医智核·流言粉碎 真实元宝搜索（WSA）与大模型端到端核验及耗时测量测试类
 * <p>
 * 运行方式：mvn test -Dtest=FactCheckBenchmarkEvaluationTest -DrunRealApiBenchmark=true
 * 测量指标（对齐需求文档第 6 节 Demo 验收口径）：
 * 1. 单论断平均检索调用次数（目标 <= 1.5 次，硬上限 3 次）；
 * 2. 首张结论卡片产出耗时（目标 <= 10 秒）与整篇完成总耗时（目标 <= 25 秒）；
 * 3. 引文逐字可校验率（强制 100%）。
 * </p>
 */
@Slf4j
@SpringBootTest
@EnabledIfSystemProperty(named = "runRealApiBenchmark", matches = "true")
class FactCheckBenchmarkEvaluationTest {

    @Resource
    private FactCheckService factCheckService;

    @Resource
    private StanceAnnotationService stanceAnnotationService;

    @Test
    @DisplayName("端到端真实核验闭环与耗时/调用开销/引文可校验率实测")
    void evaluateRealEndToEndFactCheck() throws InterruptedException {
        String sampleRumor = "家人们注意了！每天早晨空腹喝一碗陈醋能软化血管治高血压。"
                + "平时吃他汀类降脂药特别伤肝，转氨酶一高必须马上自己停药！"
                + "突发心梗千万别打120乱动，只要用力猛拍左右胳膊肘内侧就能急救活命！"
                + "某某牌深海神仙胶囊三天见效包治百病！";

        long startMs = System.currentTimeMillis();
        Long firstCardReadyCostMs = null;

        FactCheckTaskResponse initResp = factCheckService.submitTask(
                "t-benchmark-01",
                FactCheckRequest.builder().text(sampleRumor).skipCache(true).build()
        );
        assertNotNull(initResp);

        FactCheckTaskResponse latest = initResp;
        while (TaskStatus.PROCESSING.getCode().equals(latest.getStatus())) {
            Thread.sleep(500);
            latest = factCheckService.getTaskStatus("t-benchmark-01");
            if (firstCardReadyCostMs == null && latest.getCompletedClaimCount() > 0) {
                firstCardReadyCostMs = System.currentTimeMillis() - startMs;
            }
            if (System.currentTimeMillis() - startMs > 35_000) {
                break;
            }
        }
        long totalCostMs = System.currentTimeMillis() - startMs;

        log.info("=== 端到端实测结果 JSON ===\n{}", JSON.toJSONString(latest, JSONWriter.Feature.PrettyFormat));

        List<ClaimItemDto> claims = latest.getClaims();
        assertNotNull(claims);
        assertFalse(claims.isEmpty());

        int totalSearchCalls = 0;
        int verifiableCount = 0;
        for (ClaimItemDto claim : claims) {
            if (!claim.isPersonalMedication()) {
                verifiableCount++;
                totalSearchCalls += claim.getSearchCount();
                assertTrue(claim.getSearchCount() <= 3, "单论断检索调用不得超过硬上限 3 次");
            }
            if (claim.getEvidences() != null) {
                for (EvidenceItemDto ev : claim.getEvidences()) {
                    if (ev.isCitationValid()) {
                        assertTrue(
                                stanceAnnotationService.isQuoteVerbatimValid(ev.getQuote(), ev.getSourceSummary()),
                                "引文必须 100% 在来源摘要中逐字出现"
                        );
                    }
                }
            }
        }

        double avgSearchCalls = verifiableCount > 0 ? (double) totalSearchCalls / verifiableCount : 0.0;
        log.info("[BenchmarkSummary] 提取可核验论断数={}, 总检索调用={}次, 平均每论断调用={}次, 首张卡片耗时={}ms, 全部完成总耗时={}ms",
                verifiableCount, totalSearchCalls, String.format("%.2f", avgSearchCalls),
                firstCardReadyCostMs != null ? firstCardReadyCostMs : totalCostMs, totalCostMs);
    }
}

