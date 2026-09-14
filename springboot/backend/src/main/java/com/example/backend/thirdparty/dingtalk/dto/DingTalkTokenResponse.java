package com.example.backend.thirdparty.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 钉钉用户 accessToken 响应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DingTalkTokenResponse {

    private String accessToken;

    private String refreshToken;

    private Long expireIn;

    private String corpId;
}
