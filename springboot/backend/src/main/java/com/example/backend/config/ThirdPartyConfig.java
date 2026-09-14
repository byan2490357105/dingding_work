package com.example.backend.config;

import com.example.backend.thirdparty.dingtalk.DingTalkProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 第三方平台 HTTP 客户端配置。
 */
@Configuration
public class ThirdPartyConfig {

    @Bean
    public RestTemplate thirdPartyRestTemplate(RestTemplateBuilder builder, DingTalkProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeout()))
                .build();
    }
}
