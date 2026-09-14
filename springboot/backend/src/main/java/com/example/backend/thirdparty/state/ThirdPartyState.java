package com.example.backend.thirdparty.state;

import lombok.Data;

/**
 * 第三方授权 state 对应的上下文。
 */
@Data
public class ThirdPartyState {

    /** 平台标识 */
    private String provider;

    /** login 登录 / bind 绑定 */
    private String mode;

    /** 绑定模式下当前站内用户 id，登录模式为空 */
    private Long userId;

    /** 过期时间戳（毫秒） */
    private long expireAt;
}
