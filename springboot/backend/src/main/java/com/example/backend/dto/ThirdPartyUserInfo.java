package com.example.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方平台返回并归一化后的用户信息。
 * 各平台 Provider 负责把差异化字段转换成本类，业务层只依赖本类。
 */
@Data
public class ThirdPartyUserInfo {

    /** 平台标识：dingtalk / wechat / feishu ... */
    private String provider;

    /** 平台内用户唯一标识 */
    private String openId;

    /** 平台跨应用唯一标识（钉钉 unionId） */
    private String unionId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String mobile;

    /** 邮箱 */
    private String email;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 访问令牌过期时间 */
    private LocalDateTime tokenExpireAt;
}
