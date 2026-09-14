package com.example.backend.dto;

import lombok.Data;

/**
 * 修改密码入参。
 */
@Data
public class ChangePasswordDTO {

    /** 旧密码（用于身份校验） */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
