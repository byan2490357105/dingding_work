package com.example.backend.dto;

import lombok.Data;

/**
 * 修改用户名入参。
 */
@Data
public class UpdateUsernameDTO {

    /** 想要改成的新用户名 */
    private String newUsername;
}
