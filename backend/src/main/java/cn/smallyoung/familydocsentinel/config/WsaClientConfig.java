package cn.smallyoung.familydocsentinel.config;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.wsa.v20250508.WsaClient;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云元宝搜索（WSA）客户端配置类
 * <p>
 * 遵循需求 4.4 节规定，将单次 HTTP 检索的连接与读取超时时间严格控制在配置的秒数（默认 3 秒）以内。
 * </p>
 */
@Configuration
public class WsaClientConfig {

    @Value("${tencentcloud.wsa.secretId}")
    private String secretId;

    @Value("${tencentcloud.wsa.secretKey}")
    private String secretKey;

    @Resource
    private FactCheckProperties factCheckProperties;

    @Bean
    public WsaClient wsaClient() {
        int timeoutSeconds = Math.max(1, factCheckProperties.getSearch().getTimeoutSeconds());

        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setConnTimeout(timeoutSeconds);
        httpProfile.setReadTimeout(timeoutSeconds);
        httpProfile.setWriteTimeout(timeoutSeconds);

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        return new WsaClient(new Credential(secretId, secretKey), null, clientProfile);
    }
}

