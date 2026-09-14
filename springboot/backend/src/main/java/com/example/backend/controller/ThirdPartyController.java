package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.config.ThirdPartyProperties;
import com.example.backend.dto.ThirdPartyBindingDTO;
import com.example.backend.dto.ThirdPartyCallbackResult;
import com.example.backend.dto.ThirdPartyProviderDTO;
import com.example.backend.dto.ThirdPartyTicketDTO;
import com.example.backend.dto.UserDTO;
import com.example.backend.exception.BusinessException;
import com.example.backend.service.ThirdPartyAuthService;
import com.example.backend.thirdparty.ThirdPartyProvider;
import com.example.backend.thirdparty.ThirdPartyProviderRegistry;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

/**
 * 第三方登录 / 绑定接口。
 */
@RestController
@RequestMapping("/api/third-party")
public class ThirdPartyController {

    private final ThirdPartyAuthService thirdPartyAuthService;
    private final ThirdPartyProviderRegistry providerRegistry;
    private final ThirdPartyProperties thirdPartyProperties;

    public ThirdPartyController(ThirdPartyAuthService thirdPartyAuthService,
                                ThirdPartyProviderRegistry providerRegistry,
                                ThirdPartyProperties thirdPartyProperties) {
        this.thirdPartyAuthService = thirdPartyAuthService;
        this.providerRegistry = providerRegistry;
        this.thirdPartyProperties = thirdPartyProperties;
    }

    /** 已启用的第三方平台（登录页展示按钮用） */
    @GetMapping("/providers")
    public Result<List<ThirdPartyProviderDTO>> providers() {
        return Result.success(thirdPartyAuthService.enabledProviders());
    }

    /** 登录：获取第三方授权地址 */
    @GetMapping("/{provider}/authorize-url")
    public Result<String> authorizeUrl(@PathVariable("provider") String provider) {
        return Result.success(thirdPartyAuthService.createLoginUrl(provider));
    }

    /** 绑定：已登录用户获取第三方授权地址 */
    @GetMapping("/{provider}/bind-url")
    public Result<String> bindUrl(@PathVariable("provider") String provider,
                                  @RequestAttribute("username") String username) {
        return Result.success(thirdPartyAuthService.createBindUrl(provider, username));
    }

    /**
     * 第三方回调（浏览器跳转，不能携带 JWT，因此由 state 保存意图）。
     * 登录成功回跳前端并携带一次性 ticket；绑定成功回跳绑定管理页。
     */
    @GetMapping("/{provider}/callback")
    public void callback(@PathVariable("provider") String provider,
                         @RequestParam(value = "code", required = false) String code,
                         @RequestParam(value = "authCode", required = false) String authCode,
                         @RequestParam(value = "state", required = false) String state,
                         HttpServletResponse response) throws IOException {
        String authorizationCode = code != null ? code : authCode;
        try {
            ThirdPartyCallbackResult result =
                    thirdPartyAuthService.handleCallback(provider, authorizationCode, state);
            ThirdPartyProvider target = providerRegistry.require(provider);
            if ("bind".equals(result.getMode())) {
                response.sendRedirect(appendQuery(target.getBindResultUrl(), "bind", provider, "status", "success"));
            } else {
                response.sendRedirect(appendQuery(target.getFrontendCallbackUrl(), "ticket", result.getTicket()));
            }
        } catch (BusinessException e) {
            response.sendRedirect(appendQuery(resolveErrorUrl(provider), "error", e.getMessage()));
        } catch (Exception e) {
            response.sendRedirect(appendQuery(resolveErrorUrl(provider), "error", "第三方登录失败，请稍后重试"));
        }
    }

    /** 一次性票据换取本站 JWT */
    @PostMapping("/exchange")
    public Result<UserDTO> exchange(@RequestBody(required = false) ThirdPartyTicketDTO ticketDTO) {
        String ticket = ticketDTO == null ? null : ticketDTO.getTicket();
        return Result.success("登录成功", thirdPartyAuthService.exchangeTicket(ticket));
    }

    /** 当前用户已绑定的第三方平台 */
    @GetMapping("/bindings")
    public Result<List<ThirdPartyBindingDTO>> bindings(@RequestAttribute("username") String username) {
        return Result.success(thirdPartyAuthService.listBindings(username));
    }

    /** 解绑第三方平台 */
    @DeleteMapping("/bindings/{provider}")
    public Result<Void> unbind(@PathVariable("provider") String provider,
                               @RequestAttribute("username") String username) {
        thirdPartyAuthService.unbind(username, provider);
        return Result.success("解绑成功", null);
    }

    private String resolveErrorUrl(String provider) {
        try {
            return providerRegistry.require(provider).getFrontendCallbackUrl();
        } catch (Exception e) {
            return thirdPartyProperties.getFrontendErrorUrl();
        }
    }

    /** 把参数追加到带 # 的前端 hash 路由上 */
    private String appendQuery(String url, String... pairs) throws IOException {
        StringBuilder builder = new StringBuilder(url);
        builder.append(url.contains("?") ? "&" : "?");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                builder.append("&");
            }
            builder.append(pairs[i]).append("=").append(URLEncoder.encode(pairs[i + 1] == null ? "" : pairs[i + 1], "UTF-8"));
        }
        return builder.toString();
    }
}
