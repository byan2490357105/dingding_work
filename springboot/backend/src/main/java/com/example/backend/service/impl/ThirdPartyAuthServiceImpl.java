package com.example.backend.service.impl;

import com.example.backend.dto.ThirdPartyBindingDTO;
import com.example.backend.dto.ThirdPartyCallbackResult;
import com.example.backend.dto.ThirdPartyProviderDTO;
import com.example.backend.dto.ThirdPartyUserInfo;
import com.example.backend.dto.UserDTO;
import com.example.backend.entity.User;
import com.example.backend.entity.UserOAuth;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.ThirdPartyAuthService;
import com.example.backend.service.UserOAuthService;
import com.example.backend.service.UserService;
import com.example.backend.thirdparty.ThirdPartyProvider;
import com.example.backend.thirdparty.ThirdPartyProviderRegistry;
import com.example.backend.thirdparty.state.ThirdPartyState;
import com.example.backend.thirdparty.state.ThirdPartyStateStore;
import com.example.backend.thirdparty.state.ThirdPartyTicketStore;
import com.example.backend.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ThirdPartyAuthServiceImpl implements ThirdPartyAuthService {

    private static final String MODE_LOGIN = "login";
    private static final String MODE_BIND = "bind";

    private final ThirdPartyProviderRegistry providerRegistry;
    private final ThirdPartyStateStore stateStore;
    private final ThirdPartyTicketStore ticketStore;
    private final UserOAuthService userOAuthService;
    private final UserService userService;

    public ThirdPartyAuthServiceImpl(ThirdPartyProviderRegistry providerRegistry,
                                     ThirdPartyStateStore stateStore,
                                     ThirdPartyTicketStore ticketStore,
                                     UserOAuthService userOAuthService,
                                     UserService userService) {
        this.providerRegistry = providerRegistry;
        this.stateStore = stateStore;
        this.ticketStore = ticketStore;
        this.userOAuthService = userOAuthService;
        this.userService = userService;
    }

    @Override
    public List<ThirdPartyProviderDTO> enabledProviders() {
        List<ThirdPartyProviderDTO> result = new ArrayList<>();
        for (ThirdPartyProvider provider : providerRegistry.enabledProviders()) {
            result.add(new ThirdPartyProviderDTO(provider.getProvider(), provider.getDisplayName()));
        }
        return result;
    }

    @Override
    public String createLoginUrl(String provider) {
        ThirdPartyProvider target = providerRegistry.require(provider);
        String state = stateStore.create(provider, MODE_LOGIN, null);
        return target.buildAuthorizeUrl(state);
    }

    @Override
    public String createBindUrl(String provider, String username) {
        User user = requireUser(username);
        ThirdPartyProvider target = providerRegistry.require(provider);
        String state = stateStore.create(provider, MODE_BIND, user.getId());
        return target.buildAuthorizeUrl(state);
    }

    @Override
    public ThirdPartyCallbackResult handleCallback(String provider, String code, String state) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(400, "第三方授权码不能为空，请重新发起授权");
        }
        ThirdPartyState stateValue = stateStore.consume(state);
        if (stateValue == null) {
            throw new BusinessException(400, "授权状态已失效，请重新发起登录");
        }
        if (!provider.equals(stateValue.getProvider())) {
            throw new BusinessException(400, "授权平台不匹配");
        }

        ThirdPartyProvider target = providerRegistry.require(provider);
        ThirdPartyUserInfo userInfo = target.getUserInfo(code);
        userInfo.setProvider(provider);

        ThirdPartyCallbackResult result = new ThirdPartyCallbackResult();
        result.setProvider(provider);
        result.setMode(stateValue.getMode());
        result.setSuccess(true);

        if (MODE_BIND.equals(stateValue.getMode())) {
            userOAuthService.saveBinding(stateValue.getUserId(), userInfo);
            syncPhone(stateValue.getUserId(), userInfo.getMobile());
            result.setMessage("绑定成功");
            return result;
        }

        User user = loginOrRegister(userInfo);
        result.setTicket(ticketStore.create(user.getId()));
        result.setMessage("登录成功");
        return result;
    }

    @Override
    public UserDTO exchangeTicket(String ticket) {
        Long userId = ticketStore.consume(ticket);
        if (userId == null) {
            throw new BusinessException(400, "登录票据已失效，请重新登录");
        }
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername(user.getUsername());
        userDTO.setToken(JwtUtil.createToken(user));
        return userDTO;
    }

    @Override
    public List<ThirdPartyBindingDTO> listBindings(String username) {
        User user = requireUser(username);
        List<ThirdPartyBindingDTO> result = new ArrayList<>();
        for (UserOAuth binding : userOAuthService.listByUserId(user.getId())) {
            ThirdPartyBindingDTO dto = new ThirdPartyBindingDTO();
            dto.setProvider(binding.getProvider());
            dto.setNickname(binding.getNickname());
            dto.setAvatar(binding.getAvatar());
            dto.setMobile(maskMobile(binding.getMobile()));
            dto.setBindTime(binding.getCreatedAt());
            result.add(dto);
        }
        return result;
    }

    @Override
    public void unbind(String username, String provider) {
        User user = requireUser(username);
        List<UserOAuth> bindings = userOAuthService.listByUserId(user.getId());
        boolean thirdPartyOnly = isRandomPassword(user.getPassword());
        if (thirdPartyOnly && bindings.size() <= 1) {
            throw new BusinessException("至少保留一种登录方式，请先设置密码后再解绑");
        }
        userOAuthService.unbind(user.getId(), provider);
    }

    /**
     * 登录或自动注册：
     * 1. openId / unionId 已绑定 -> 直接登录；
     * 2. 手机号已存在于本站 -> 自动绑定并登录（即“钉钉手机号绑定登录”）；
     * 3. 全新用户 -> 自动创建账号后登录。
     */
    private User loginOrRegister(ThirdPartyUserInfo userInfo) {
        User user = findOrCreateUser(userInfo);
        syncPhone(user.getId(), userInfo.getMobile());
        return user;
    }

    private User findOrCreateUser(ThirdPartyUserInfo userInfo) {
        if (userInfo.getOpenId() == null || userInfo.getOpenId().trim().isEmpty()) {
            throw new BusinessException(502, "钉钉未返回用户唯一标识 openId");
        }

        UserOAuth byOpenId = userOAuthService.getByProviderAndOpenId(userInfo.getProvider(), userInfo.getOpenId());
        if (byOpenId != null) {
            userOAuthService.updateToken(byOpenId, userInfo);
            return requireUserById(byOpenId.getUserId());
        }

        UserOAuth byUnionId = userOAuthService.getByProviderAndUnionId(userInfo.getProvider(), userInfo.getUnionId());
        if (byUnionId != null) {
            userOAuthService.saveBinding(byUnionId.getUserId(), userInfo);
            return requireUserById(byUnionId.getUserId());
        }

        if (userInfo.getMobile() != null && !userInfo.getMobile().trim().isEmpty()) {
            User byMobile = userService.getByPhone(userInfo.getMobile());
            if (byMobile != null) {
                userOAuthService.saveBinding(byMobile.getId(), userInfo);
                return byMobile;
            }
        }

        User user = userService.registerThirdPartyUser(
                generateUsername(userInfo), UUID.randomUUID().toString().replace("-", ""), userInfo.getMobile());
        userOAuthService.saveBinding(user.getId(), userInfo);
        return user;
    }

    /** 第三方返回手机号时同步到 user.phone，手机号已被其他用户占用时跳过 */
    private void syncPhone(Long userId, String mobile) {
        if (mobile == null || mobile.trim().isEmpty()) {
            return;
        }
        User existing = userService.getByPhone(mobile);
        if (existing != null && !existing.getId().equals(userId)) {
            return;
        }
        userService.updatePhone(userId, mobile);
    }

    private User requireUser(String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        return user;
    }

    private User requireUserById(Long userId) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(401, "绑定的本站用户不存在");
        }
        return user;
    }

    private String generateUsername(ThirdPartyUserInfo userInfo) {
        String source = userInfo.getUnionId() != null ? userInfo.getUnionId() : userInfo.getOpenId();
        String suffix = source.replaceAll("[^0-9a-zA-Z]", "");
        if (suffix.length() > 16) {
            suffix = suffix.substring(0, 16);
        }
        String base = userInfo.getProvider() + "_" + suffix;
        String username = base;
        int index = 1;
        while (userService.isUsernameExists(username)) {
            username = base + "_" + index;
            index++;
        }
        return username;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 7) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
    }

    private boolean isRandomPassword(String password) {
        return password != null && password.matches("[0-9a-f]{32}");
    }
}
