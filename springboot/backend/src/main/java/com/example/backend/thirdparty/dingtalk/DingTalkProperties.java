package com.example.backend.thirdparty.dingtalk;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 钉钉开放平台相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "third-party.dingtalk")
public class DingTalkProperties {

    /** 是否启用钉钉登录 */
    private boolean enabled = false;

    /** 应用的 AppKey / SuiteKey，钉钉 OAuth2 中叫 clientId */
    private String clientId;

    /** 应用的 AppSecret，钉钉 OAuth2 中叫 clientSecret */
    private String clientSecret;

    /** 授权回调地址：http://localhost:8080/api/third-party/dingtalk/callback */
    private String redirectUri;

    /** 登录成功后回跳前端的地址：http://localhost:5173/#/oauth/callback */
    private String frontendCallbackUrl;

    /** 绑定成功后回跳前端的地址：http://localhost:5173/#/settings/bindings */
    private String bindResultUrl;

    /** 授权 scope，扫码登录/手机号登录均为 openid；如需读取手机号需申请 Contact.User.mobile */
    private String scope = "openid";

    /** 钉钉统一登录授权地址（页面同时提供扫码登录与手机号登录） */
    private String authUrl = "https://login.dingtalk.com/oauth2/auth";

    /** 用户 accessToken 交换/刷新地址 */
    private String tokenUrl = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";

    /** 用户信息地址 */
    private String userInfoUrl = "https://api.dingtalk.com/v1.0/contact/users/me";

    /** 钉钉文档（含表格）OpenAPI 基础地址 */
    private String docBaseUrl = "https://api.dingtalk.com";

    /** 获取工作表接口路径，可按钉钉开放平台文档调整 */
    private String listSheetsPath = "/v1.0/doc/workbooks/{workbookId}/sheets";

    /** 单元格区域读写接口路径，可按钉钉开放平台文档调整 */
    private String rangePath = "/v1.0/doc/workbooks/{workbookId}/ranges/{range}";

    /** 连接超时（毫秒） */
    private int connectTimeout = 5000;

    /** 读取超时（毫秒） */
    private int readTimeout = 10000;

    public boolean isConfigured() {
        return enabled
                && clientId != null && !clientId.trim().isEmpty()
                && clientSecret != null && !clientSecret.trim().isEmpty()
                && redirectUri != null && !redirectUri.trim().isEmpty();
    }
}
