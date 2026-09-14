package com.example.backend.dto;

import lombok.Data;

/**
 * 前端可展示的第三方登录平台。
 */
@Data
public class ThirdPartyProviderDTO {

    private String provider;

    private String displayName;

    public ThirdPartyProviderDTO() {
    }

    public ThirdPartyProviderDTO(String provider, String displayName) {
        this.provider = provider;
        this.displayName = displayName;
    }
}
