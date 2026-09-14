package com.example.backend.service;

import com.example.backend.dto.ThirdPartyUserInfo;
import com.example.backend.entity.UserOAuth;

import java.util.List;

/**
 * 第三方账号绑定关系服务。
 */
public interface UserOAuthService {

    /** 按平台 + openId 查询绑定 */
    UserOAuth getByProviderAndOpenId(String provider, String openId);

    /** 按平台 + unionId 查询绑定 */
    UserOAuth getByProviderAndUnionId(String provider, String unionId);

    /** 查询某用户在某平台的绑定 */
    UserOAuth getByUserIdAndProvider(Long userId, String provider);

    /** 查询某用户全部绑定 */
    List<UserOAuth> listByUserId(Long userId);

    /** 新增或更新绑定关系（绑定到指定用户） */
    UserOAuth saveBinding(Long userId, ThirdPartyUserInfo userInfo);

    /** 刷新第三方令牌信息 */
    void updateToken(UserOAuth binding, ThirdPartyUserInfo userInfo);

    /** 解绑 */
    void unbind(Long userId, String provider);
}
