package com.example.backend.service;

import com.example.backend.dto.ThirdPartyBindingDTO;
import com.example.backend.dto.ThirdPartyCallbackResult;
import com.example.backend.dto.ThirdPartyProviderDTO;
import com.example.backend.dto.UserDTO;

import java.util.List;

/**
 * 第三方登录统一服务：登录、绑定、解绑、票据换取本站 JWT。
 */
public interface ThirdPartyAuthService {

    /** 已启用的第三方平台 */
    List<ThirdPartyProviderDTO> enabledProviders();

    /** 生成第三方登录授权地址 */
    String createLoginUrl(String provider);

    /** 生成第三方绑定授权地址（需要当前登录用户） */
    String createBindUrl(String provider, String username);

    /** 处理第三方回调 */
    ThirdPartyCallbackResult handleCallback(String provider, String code, String state);

    /** 一次性票据换取本站 JWT */
    UserDTO exchangeTicket(String ticket);

    /** 当前用户已绑定的第三方平台 */
    List<ThirdPartyBindingDTO> listBindings(String username);

    /** 解绑第三方平台 */
    void unbind(String username, String provider);
}
