package com.example.backend.dto;

import lombok.Data;

/**
 * 前后端交互的用户 DTO，用于传递用户名、密码和 Token。
 */
@Data
public class UserDTO {

    /** 用户名 */
    private String username;

    /** 密码（仅登录/注册时由前端传入，响应中不返回） */
    private String password;

    /** JWT 令牌（登录/注册成功后由后端生成返回） */
    private String token;
}
