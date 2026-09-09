package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.UserDTO;
import com.example.backend.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 登录、注册与 Token 校验接口。
 */
@RestController
@RequestMapping("/api/token")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册成功后直接签发 Token，便于前端自动登录 */
    @PostMapping("/register")
    public Result<UserDTO> register(@RequestBody(required = false) UserDTO userDTO) {
        return Result.success("注册成功", authService.register(userDTO));
    }

    /** 登录成功后签发 Token */
    @PostMapping("/login")
    public Result<UserDTO> login(@RequestBody(required = false) UserDTO userDTO) {
        return Result.success("登录成功", authService.login(userDTO));
    }

    /** 校验 Token，返回当前用户信息（由 JwtInterceptor 保护） */
    @GetMapping
    public Result<UserDTO> checkToken(HttpServletRequest request) {
        String token = (String) request.getAttribute("token");
        return Result.success("Token 有效", authService.getUserInfoByToken(token));
    }
}
