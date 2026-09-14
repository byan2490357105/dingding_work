package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 第三方账号绑定关系，一个用户可以绑定多个第三方平台。
 */
@Data
@TableName("user_oauth")
public class UserOAuth {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 本站用户 id，外键关联 user.id */
    private Long userId;

    /** 第三方平台标识：dingtalk / wechat / feishu ... */
    private String provider;

    /** 第三方平台内唯一标识（钉钉为 openId） */
    private String openId;

    /** 第三方平台跨应用唯一标识（钉钉为 unionId） */
    private String unionId;

    /** 第三方昵称 */
    private String nickname;

    /** 第三方头像 */
    private String avatar;

    /** 第三方返回的手机号 */
    private String mobile;

    /** 访问令牌（调用第三方接口用，生产环境建议加密存储） */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 访问令牌过期时间 */
    private LocalDateTime tokenExpireAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 修改时间 */
    private LocalDateTime updatedAt;
}
