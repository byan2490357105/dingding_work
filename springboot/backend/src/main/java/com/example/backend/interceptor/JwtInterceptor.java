package com.example.backend.interceptor;

import com.example.backend.common.Result;
import com.example.backend.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器：校验请求头中的 Token，并放入 request 供 Controller 使用。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public JwtInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 只拦截 Controller 方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = resolveToken(request.getHeader("Authorization"));
        if (!JwtUtil.checkToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    Result.error(401, "未登录或 Token 已失效")));
            return false;
        }

        request.setAttribute("username", JwtUtil.getUsername(token));
        request.setAttribute("token", token);
        return true;
    }

    /**
     * 支持 Authorization: Bearer xxx 或 Authorization: token xxx 两种写法。
     */
    private String resolveToken(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String value = authorization.trim();
        if (value.startsWith("Bearer ")) {
            return value.substring("Bearer ".length()).trim();
        }
        if (value.startsWith("token ")) {
            return value.substring("token ".length()).trim();
        }
        return value;
    }
}
