package com.example.backend.thirdparty;

import com.example.backend.dto.ThirdPartyUserInfo;

/**
 * 第三方登录平台抽象。
 * 新增微信、飞书登录时只需实现本接口并交给 Spring 管理，业务层无需改动。
 */
public interface ThirdPartyProvider {

    /** 平台标识：dingtalk / wechat / feishu */
    String getProvider();

    /** 平台展示名 */
    String getDisplayName();

    /** 是否启用（未配置密钥时为 false） */
    boolean isEnabled();

    /** 生成授权地址（登录、绑定共用，具体意图由 state 携带） */
    String buildAuthorizeUrl(String state);

    /** 用授权码换取第三方用户信息 */
    ThirdPartyUserInfo getUserInfo(String code);

    /** 用 refreshToken 刷新第三方令牌 */
    ThirdPartyUserInfo refreshAccessToken(String refreshToken);

    /** 登录成功后回跳前端的一次性票据页面地址 */
    String getFrontendCallbackUrl();

    /** 绑定完成后回跳前端的地址 */
    String getBindResultUrl();
}
