package com.example.backend.service.impl;

import com.example.backend.dto.ThirdPartyUserInfo;
import com.example.backend.entity.User;
import com.example.backend.entity.UserOAuth;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.DingTalkSheetService;
import com.example.backend.service.UserOAuthService;
import com.example.backend.service.UserService;
import com.example.backend.thirdparty.dingtalk.DingTalkProvider;
import com.example.backend.thirdparty.dingtalk.DingTalkSheetClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class DingTalkSheetServiceImpl implements DingTalkSheetService {

    private final UserService userService;
    private final UserOAuthService userOAuthService;
    private final DingTalkProvider dingTalkProvider;
    private final DingTalkSheetClient sheetClient;

    public DingTalkSheetServiceImpl(UserService userService,
                                    UserOAuthService userOAuthService,
                                    DingTalkProvider dingTalkProvider,
                                    DingTalkSheetClient sheetClient) {
        this.userService = userService;
        this.userOAuthService = userOAuthService;
        this.dingTalkProvider = dingTalkProvider;
        this.sheetClient = sheetClient;
    }

    @Override
    public Map<String, Object> listSheets(String username, String workbookId) {
        return sheetClient.listSheets(requireAccessToken(username), workbookId);
    }

    @Override
    public Map<String, Object> getRange(String username, String workbookId, String range) {
        return sheetClient.getRange(requireAccessToken(username), workbookId, range);
    }

    @Override
    public Map<String, Object> updateRange(String username, String workbookId,
                                           String range, List<List<Object>> values) {
        return sheetClient.updateRange(requireAccessToken(username), workbookId, range, values);
    }

    /** 获取钉钉访问令牌，临近过期时自动刷新 */
    private String requireAccessToken(String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        UserOAuth binding = userOAuthService.getByUserIdAndProvider(user.getId(), DingTalkProvider.PROVIDER);
        if (binding == null) {
            throw new BusinessException("请先绑定钉钉账号，才能操作钉钉表格");
        }
        if (binding.getAccessToken() == null) {
            throw new BusinessException("钉钉授权已失效，请重新绑定钉钉账号");
        }
        if (needRefresh(binding) && binding.getRefreshToken() != null) {
            ThirdPartyUserInfo token = dingTalkProvider.refreshAccessToken(binding.getRefreshToken());
            userOAuthService.updateToken(binding, token);
            return binding.getAccessToken();
        }
        return binding.getAccessToken();
    }

    private boolean needRefresh(UserOAuth binding) {
        LocalDateTime expireAt = binding.getTokenExpireAt();
        if (expireAt == null) {
            return false;
        }
        return expireAt.isBefore(LocalDateTime.now().plusMinutes(2));
    }
}
