package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前用户已绑定的第三方平台信息。
 */
@Data
public class ThirdPartyBindingDTO {

    private String provider;

    private String nickname;

    private String avatar;

    /** 手机号脱敏后返回，避免前端泄露完整手机号 */
    private String mobile;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime bindTime;
}
