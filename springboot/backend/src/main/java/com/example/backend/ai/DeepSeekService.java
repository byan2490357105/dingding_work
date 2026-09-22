package com.example.backend.ai;

import com.example.backend.config.DeepSeekProperties;
import com.example.backend.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * DeepSeek AI 大模型调用封装。
 * 提供统一的 chat 接口，所有 AI 分析（月报、成功率预测）都走这里。
 */
@Service
public class DeepSeekService {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private DeepSeekProperties properties;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 调用大模型，返回纯文本回答。
     *
     * @param systemPrompt 系统提示词（设定角色与任务）
     * @param userPrompt   用户输入（待分析的数据）
     * @return 模型生成的文本内容
     */
    public String chat(String systemPrompt, String userPrompt) {
        if (!properties.isConfigured()) {
            throw new BusinessException(503, "AI 服务未配置，请先在 application.properties 中填写 deepseek.api-key");
        }

        String url = properties.getBaseUrl() + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getModel());
        body.put("temperature", 0.3);

        ArrayNode messages = body.putArray("messages");
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", systemPrompt);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", userPrompt);

        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText("");
        } catch (HttpStatusCodeException e) {
            throw new BusinessException(502, "AI 服务调用失败：" + e.getStatusText());
        } catch (Exception e) {
            throw new BusinessException(502, "AI 服务调用异常：" + e.getMessage());
        }
    }
}
