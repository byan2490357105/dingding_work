package com.example.backend.service;

import com.example.backend.entity.User;

import java.util.List;

public interface UserService {
    /** 判断用户名是否已被注册 */
    boolean isUsernameExists(String username);

    /** 注册：保存用户，返回带主键信息的用户 */
    User register(String username, String password);

    /** 根据用户名和密码登录，用户名或密码错误时返回 null */
    User loginByUsernameAndPassword(String username, String password);

    /** 根据用户名查询用户 */
    User getByUsername(String username);

    /** 根据主键查询用户 */
    User getById(Long id);

    /** 根据手机号查询用户 */
    User getByPhone(String phone);

    /** 第三方登录首次进入时创建用户 */
    User registerThirdPartyUser(String username, String password, String phone);

    /** 同步第三方返回的手机号到用户表 */
    void updatePhone(Long userId, String phone);

    /**
     * 修改用户名。
     * 新用户名与原名相同则直接返回；与他人重名时抛出业务异常。
     * 返回更新后的用户（Controller 据此重新签发 Token）。
     */
    User updateUsername(String currentUsername, String newUsername);

    /**
     * 修改密码：校验旧密码后更新为新密码。
     */
    void changePassword(String username, String oldPassword, String newPassword);

    /**
     * 修改邮箱（用于定时提醒推送）。传 null 或空串表示清除邮箱。
     */
    void updateEmail(String username, String email);

    /**
     * 查询所有已设置有效邮箱的用户（email IS NOT NULL AND email != ''），
     * 供定时任务批量推送邮件使用。
     */
    List<User> listUsersWithEmail();

    /**
     * 判断用户名是否可被当前用户使用：
     * 与自己当前用户名相同，或没有任何其他用户占用时返回 true。
     */
    boolean isUsernameAvailable(String currentUsername, String candidateUsername);
}
