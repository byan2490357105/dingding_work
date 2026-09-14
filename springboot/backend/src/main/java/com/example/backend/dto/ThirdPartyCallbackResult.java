package com.example.backend.dto;

import lombok.Data;

/**
 * 第三方回调处理结果，供 Controller 决定跳转地址。
 */
@Data
public class ThirdPartyCallbackResult {

    private String provider;

    /** login 登录 / bind 绑定 */
    private String mode;

    /** 登录成功时返回给前端的一次性票据 */
    private String ticket;

    private boolean success;

    private String message;
}
