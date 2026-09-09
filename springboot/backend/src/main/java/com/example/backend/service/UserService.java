package com.example.backend.service;

import com.example.backend.entity.User;

public interface UserService {
    /** 判断用户名是否已被注册 */
    boolean isUsernameExists(String username);

    /** 注册：保存用户，返回带主键信息的用户 */
    User register(String username, String password);

    /** 根据用户名和密码登录，用户名或密码错误时返回 null */
    User loginByUsernameAndPassword(String username, String password);

    /** 根据用户名查询用户 */
    User getByUsername(String username);
}
