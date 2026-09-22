package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.StatsVO;
import com.example.backend.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 数据分析仪表盘接口。
 * 由 JwtInterceptor 保护，当前用户名从请求属性中取得。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 获取当前登录用户的仪表盘数据 */
    @GetMapping("/dashboard")
    public Result<StatsVO> dashboard(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return Result.success(statsService.getDashboard(username));
    }
}
