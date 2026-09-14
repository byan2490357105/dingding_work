package com.example.backend.thirdparty.dingtalk.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 钉钉 contact/users/me 响应。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DingTalkUserResponse {

    private String nick;

    private String avatarUrl;

    /** 手机号，需要应用具备 Contact.User.mobile 权限 */
    private String mobile;

    private String openId;

    private String unionId;

    private String email;

    private String stateCode;
}
