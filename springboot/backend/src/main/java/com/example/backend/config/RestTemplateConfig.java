package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * RestTemplate 配置：用于调用 DeepSeek 等外部 HTTP 接口。
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 大模型生成时间较长，超时设置为 120 秒
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(120000);
        RestTemplate restTemplate = new RestTemplate(factory);
        // 全局拦截器：对所有请求附带 UTF-8 相关处理（DeepSeek 默认 UTF-8，无需额外处理）
        restTemplate.setInterceptors(Collections.emptyList());
        return restTemplate;
    }
}
