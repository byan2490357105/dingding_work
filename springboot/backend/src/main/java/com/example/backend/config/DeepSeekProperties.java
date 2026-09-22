package com.example.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * DeepSeek AI 大模型配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {

    /** 是否启用 AI 功能 */
    private boolean enabled = true;

    /** API Key，在 https://platform.deepseek.com/ 创建 */
    private String apiKey;

    /** API 基地址 */
    private String baseUrl = "https://api.deepseek.com";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 单次请求超时时间（秒） */
    private int timeoutSeconds = 120;

    /** 是否配置完成（启用 且 有 apiKey） */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty();
    }
}
