package cn.smallyoung.familydocsentinel;

import cn.smallyoung.familydocsentinel.exception.BizException;
import cn.smallyoung.familydocsentinel.service.OcrService;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRRequest;
import com.tencentcloudapi.ocr.v20181119.models.GeneralBasicOCRResponse;
import com.tencentcloudapi.ocr.v20181119.models.TextDetection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 腾讯云 OCR 图片识别服务单元测试
 */
class OcrServiceTest {

    @Mock
    private OcrClient ocrClient;

    @InjectMocks
    private OcrService ocrService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("空文件或不支持格式校验")
    void testFileValidation() {
        // 空文件
        BizException ex1 = assertThrows(BizException.class, () -> ocrService.extractTextFromImage(null));
        assertTrue(ex1.getMessage().contains("请上传待识别的图片文件"));

        // 不支持的格式
        MockMultipartFile txtFile = new MockMultipartFile("file", "rumor.txt", "text/plain", "测试文字".getBytes());
        BizException ex2 = assertThrows(BizException.class, () -> ocrService.extractTextFromImage(txtFile));
        assertTrue(ex2.getMessage().contains("仅支持 JPG、PNG、WEBP 或 BMP"));
    }

    @Test
    @DisplayName("正常识别图片文本拼接")
    void testOcrSuccess() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "poster.png", "image/png", new byte[]{1, 2, 3, 4}
        );

        GeneralBasicOCRResponse mockResponse = new GeneralBasicOCRResponse();
        TextDetection d1 = new TextDetection();
        d1.setDetectedText("每天喝老陈醋一个月软化血管");
        TextDetection d2 = new TextDetection();
        d2.setDetectedText("高血压患者千万别吃降压药");
        mockResponse.setTextDetections(new TextDetection[]{d1, d2});

        when(ocrClient.GeneralBasicOCR(any(GeneralBasicOCRRequest.class))).thenReturn(mockResponse);

        String text = ocrService.extractTextFromImage(imageFile);
        assertNotNull(text);
        assertTrue(text.contains("每天喝老陈醋一个月软化血管"));
        assertTrue(text.contains("高血压患者千万别吃降压药"));
    }

    @Test
    @DisplayName("腾讯云异常安全转换为友好提示")
    void testOcrSdkExceptionMapping() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        when(ocrClient.GeneralBasicOCR(any(GeneralBasicOCRRequest.class)))
                .thenThrow(new TencentCloudSDKException("ResourceUnavailable.BalanceInsufficient", "Resource is insufficient"));

        BizException ex = assertThrows(BizException.class, () -> ocrService.extractTextFromImage(imageFile));
        assertTrue(ex.getMessage().contains("免费额度耗尽或服务未开通"));
    }
}

