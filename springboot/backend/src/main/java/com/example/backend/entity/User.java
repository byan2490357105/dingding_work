package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class User {
    /** 主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;

    /** 手机号（钉钉等第三方登录返回，用于手机号绑定登录） */
    private String phone;

    /** 邮箱（用于定时提醒推送） */
    private String email;
}
