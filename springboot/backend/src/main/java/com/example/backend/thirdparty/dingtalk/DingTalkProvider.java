package com.example.backend.thirdparty.dingtalk;

import com.example.backend.dto.ThirdPartyUserInfo;
import com.example.backend.thirdparty.ThirdPartyProvider;
import com.example.backend.thirdparty.dingtalk.dto.DingTalkTokenResponse;
import com.example.backend.thirdparty.dingtalk.dto.DingTalkUserResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * 钉钉第三方登录实现。
 * 钉钉扫码登录与手机号登录都使用同一个 OAuth2 授权码流程：
 * 登录页自行提供“扫码”和“手机号”两种入口，回调结果一致。
 */
@Component
public class DingTalkProvider implements ThirdPartyProvider {

    public static final String PROVIDER = "dingtalk";

    private final DingTalkProperties properties;
    private final DingTalkApiClient apiClient;

    public DingTalkProvider(DingTalkProperties properties, DingTalkApiClient apiClient) {
        this.properties = properties;
        this.apiClient = apiClient;
    }

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public String getDisplayName() {
        return "钉钉";
    }

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }

    @Override
    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(properties.getAuthUrl())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .queryParam("prompt", "consent")
                .build()
                .encode()
                .toUriString();
    }

    @Override
    public ThirdPartyUserInfo getUserInfo(String code) {
        DingTalkTokenResponse token = apiClient.getUserAccessToken(code);
        return buildUserInfo(token);
    }

    @Override
    public ThirdPartyUserInfo refreshAccessToken(String refreshToken) {
        DingTalkTokenResponse token = apiClient.refreshUserAccessToken(refreshToken);
        ThirdPartyUserInfo info = new ThirdPartyUserInfo();
        info.setProvider(PROVIDER);
        info.setAccessToken(token.getAccessToken());
        info.setRefreshToken(token.getRefreshToken() != null ? token.getRefreshToken() : refreshToken);
        info.setTokenExpireAt(expireAt(token.getExpireIn()));
        return info;
    }

    @Override
    public String getFrontendCallbackUrl() {
        return properties.getFrontendCallbackUrl();
    }

    @Override
    public String getBindResultUrl() {
        return properties.getBindResultUrl();
    }

    private ThirdPartyUserInfo buildUserInfo(DingTalkTokenResponse token) {
        DingTalkUserResponse user = apiClient.getUserInfo(token.getAccessToken());
        ThirdPartyUserInfo info = new ThirdPartyUserInfo();
        info.setProvider(PROVIDER);
        if (user != null) {
            info.setOpenId(user.getOpenId());
            info.setUnionId(user.getUnionId());
            info.setNickname(user.getNick());
            info.setAvatar(user.getAvatarUrl());
            info.setMobile(user.getMobile());
            info.setEmail(user.getEmail());
        }
        info.setAccessToken(token.getAccessToken());
        info.setRefreshToken(token.getRefreshToken());
        info.setTokenExpireAt(expireAt(token.getExpireIn()));
        return info;
    }

    private LocalDateTime expireAt(Long expireIn) {
        long seconds = expireIn == null ? 7200L : expireIn;
        return LocalDateTime.now().plusSeconds(seconds);
    }
}
