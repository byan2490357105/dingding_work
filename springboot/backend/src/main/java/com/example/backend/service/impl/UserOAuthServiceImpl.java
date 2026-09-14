package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.ThirdPartyUserInfo;
import com.example.backend.entity.UserOAuth;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.UserOAuthMapper;
import com.example.backend.service.UserOAuthService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class UserOAuthServiceImpl implements UserOAuthService {

    private final UserOAuthMapper userOAuthMapper;

    public UserOAuthServiceImpl(UserOAuthMapper userOAuthMapper) {
        this.userOAuthMapper = userOAuthMapper;
    }

    @Override
    public UserOAuth getByProviderAndOpenId(String provider, String openId) {
        if (openId == null) {
            return null;
        }
        LambdaQueryWrapper<UserOAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOAuth::getProvider, provider)
                .eq(UserOAuth::getOpenId, openId);
        return userOAuthMapper.selectOne(queryWrapper);
    }

    @Override
    public UserOAuth getByProviderAndUnionId(String provider, String unionId) {
        if (unionId == null) {
            return null;
        }
        LambdaQueryWrapper<UserOAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOAuth::getProvider, provider)
                .eq(UserOAuth::getUnionId, unionId);
        return userOAuthMapper.selectOne(queryWrapper);
    }

    @Override
    public UserOAuth getByUserIdAndProvider(Long userId, String provider) {
        LambdaQueryWrapper<UserOAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOAuth::getUserId, userId)
                .eq(UserOAuth::getProvider, provider);
        return userOAuthMapper.selectOne(queryWrapper);
    }

    @Override
    public List<UserOAuth> listByUserId(Long userId) {
        LambdaQueryWrapper<UserOAuth> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserOAuth::getUserId, userId);
        return userOAuthMapper.selectList(queryWrapper);
    }

    @Override
    public UserOAuth saveBinding(Long userId, ThirdPartyUserInfo userInfo) {
        if (userId == null || userInfo == null
                || userInfo.getProvider() == null || userInfo.getOpenId() == null) {
            throw new BusinessException("第三方绑定信息不完整");
        }

        UserOAuth byOpenId = getByProviderAndOpenId(userInfo.getProvider(), userInfo.getOpenId());
        if (byOpenId != null && !userId.equals(byOpenId.getUserId())) {
            throw new BusinessException(409, "该第三方账号已绑定其他用户");
        }

        UserOAuth binding = byOpenId != null ? byOpenId : getByUserIdAndProvider(userId, userInfo.getProvider());
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (binding == null) {
            binding = new UserOAuth();
            binding.setUserId(userId);
            binding.setProvider(userInfo.getProvider());
            binding.setCreatedAt(now);
            binding.setUpdatedAt(now);
            copyFields(binding, userInfo);
            userOAuthMapper.insert(binding);
        } else {
            if (!userId.equals(binding.getUserId())) {
                throw new BusinessException(409, "该第三方账号已绑定其他用户");
            }
            copyFields(binding, userInfo);
            binding.setUpdatedAt(now);
            userOAuthMapper.updateById(binding);
        }
        return binding;
    }

    @Override
    public void updateToken(UserOAuth binding, ThirdPartyUserInfo userInfo) {
        // 刷新令牌时只更新令牌字段，避免把 openId 等已有信息覆盖为空
        if (userInfo.getAccessToken() != null) {
            binding.setAccessToken(userInfo.getAccessToken());
        }
        if (userInfo.getRefreshToken() != null) {
            binding.setRefreshToken(userInfo.getRefreshToken());
        }
        if (userInfo.getTokenExpireAt() != null) {
            binding.setTokenExpireAt(userInfo.getTokenExpireAt());
        }
        binding.setUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        userOAuthMapper.updateById(binding);
    }

    @Override
    public void unbind(Long userId, String provider) {
        UserOAuth binding = getByUserIdAndProvider(userId, provider);
        if (binding == null) {
            throw new BusinessException(404, "尚未绑定该第三方平台");
        }
        userOAuthMapper.deleteById(binding.getId());
    }

    private void copyFields(UserOAuth binding, ThirdPartyUserInfo userInfo) {
        binding.setOpenId(userInfo.getOpenId());
        binding.setUnionId(userInfo.getUnionId());
        binding.setNickname(userInfo.getNickname());
        binding.setAvatar(userInfo.getAvatar());
        binding.setMobile(userInfo.getMobile());
        binding.setAccessToken(userInfo.getAccessToken());
        binding.setRefreshToken(userInfo.getRefreshToken());
        binding.setTokenExpireAt(userInfo.getTokenExpireAt());
    }
}
