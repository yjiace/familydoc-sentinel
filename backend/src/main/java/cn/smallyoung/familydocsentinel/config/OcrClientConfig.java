package cn.smallyoung.familydocsentinel.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.ocr.v20181119.OcrClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云文字识别（OCR）客户端配置类
 * <p>
 * 复用统一的腾讯云 API 凭证，用于微信群养生截图与多模态流言海报的文字提取。
 * </p>
 */
@Configuration
public class OcrClientConfig {

    @Value("${tencentcloud.ocr.secretId:${tencentcloud.wsa.secretId}}")
    private String secretId;

    @Value("${tencentcloud.ocr.secretKey:${tencentcloud.wsa.secretKey}}")
    private String secretKey;

    @Value("${tencentcloud.ocr.region:ap-guangzhou}")
    private String region;

    @Bean
    public OcrClient ocrClient() {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setConnTimeout(10);
        httpProfile.setReadTimeout(15);
        httpProfile.setWriteTimeout(15);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        return new OcrClient(new Credential(secretId, secretKey), region, clientProfile);
    }
}

