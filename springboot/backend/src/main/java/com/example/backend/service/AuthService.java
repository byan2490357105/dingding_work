package com.example.backend.service;

import com.example.backend.dto.UserDTO;

public interface AuthService {

    /** 注册并签发 JWT Token */
    UserDTO register(UserDTO userDTO);

    /** 登录并签发 JWT Token */
    UserDTO login(UserDTO userDTO);

    /** 根据已校验通过的 Token 获取当前用户信息 */
    UserDTO getUserInfoByToken(String token);
}
