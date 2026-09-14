package com.example.backend.thirdparty.dingtalk;

import com.example.backend.exception.BusinessException;
import com.example.backend.thirdparty.dingtalk.dto.DingTalkTokenResponse;
import com.example.backend.thirdparty.dingtalk.dto.DingTalkUserResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 钉钉开放平台 HTTP 客户端：只负责调用接口，不包含业务逻辑。
 */
@Component
public class DingTalkApiClient {

    private final DingTalkProperties properties;
    private final RestTemplate restTemplate;

    public DingTalkApiClient(DingTalkProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /** 授权码换取用户 accessToken（扫码登录、手机号登录回调都走这里） */
    public DingTalkTokenResponse getUserAccessToken(String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("clientId", properties.getClientId());
        body.put("clientSecret", properties.getClientSecret());
        body.put("code", code);
        body.put("grantType", "authorization_code");
        return postForToken(body);
    }

    /** 刷新用户 accessToken */
    public DingTalkTokenResponse refreshUserAccessToken(String refreshToken) {
        Map<String, Object> body = new HashMap<>();
        body.put("clientId", properties.getClientId());
        body.put("clientSecret", properties.getClientSecret());
        body.put("refreshToken", refreshToken);
        body.put("grantType", "refresh_token");
        return postForToken(body);
    }

    /** 获取当前授权用户信息 */
    public DingTalkUserResponse getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<DingTalkUserResponse> response = restTemplate.exchange(
                    properties.getUserInfoUrl(),// 接口地址，从配置读取（钉钉获取用户信息API地址）
                    HttpMethod.GET,// GET请求
                    entity,// 请求（带header，无body）
                    DingTalkUserResponse.class// 把钉钉返回的JSON自动反序列化为这个Java实体类
            );
            return response.getBody();
        } catch (RestClientException e) {
            throw new BusinessException(502, "获取钉钉用户信息失败：" + e.getMessage());
        }
    }

    private DingTalkTokenResponse postForToken(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<DingTalkTokenResponse> response = restTemplate.exchange(
                    properties.getTokenUrl(), HttpMethod.POST, entity, DingTalkTokenResponse.class);
            DingTalkTokenResponse token = response.getBody();
            if (token == null || token.getAccessToken() == null) {
                throw new BusinessException(502, "钉钉未返回 accessToken");
            }
            return token;
        } catch (RestClientException e) {
            throw new BusinessException(502, "调用钉钉令牌接口失败：" + e.getMessage());
        }
    }
}
