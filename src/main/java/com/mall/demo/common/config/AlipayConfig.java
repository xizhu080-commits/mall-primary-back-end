package com.mall.demo.common.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AlipayConfig {

    @Value("${alipay.sandbox.appId}")
    private String appId;

    @Value("${alipay.sandbox.privateKey}")
    private String privateKey;

    @Value("${alipay.sandbox.alipayPublicKey}")
    private String alipayPublicKey;

    @Value("${alipay.sandbox.gatewayUrl}")
    private String gatewayUrl;

    @Bean
    public AlipayClient alipayClient() {
        // 初始化支付宝 SDK 的核心客户端对象
        return new DefaultAlipayClient(
                gatewayUrl,
                appId,
                privateKey,
                "json",
                "UTF-8",
                alipayPublicKey,
                "RSA2"
                // 沙箱环境和现代接口统一使用 RSA2 签名
        );
    }
}
