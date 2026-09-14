package com.example.backend.dto;

import lombok.Data;

/**
 * 当前登录用户资料（不含密码等敏感字段）。
 */
@Data
public class ProfileVO {

    private String username;

    private String phone;
}
