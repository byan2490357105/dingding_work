package com.example.backend.dto;

import lombok.Data;

/**
 * 第三方登录回跳前端时使用的一次性票据请求体。
 */
@Data
public class ThirdPartyTicketDTO {

    private String ticket;
}
