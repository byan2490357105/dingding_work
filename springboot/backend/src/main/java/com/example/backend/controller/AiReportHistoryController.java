package com.example.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.common.Result;
import com.example.backend.entity.AiReport;
import com.example.backend.entity.User;
import com.example.backend.mapper.AiReportMapper;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;

/**
 * AI 报告历史回看接口。
 * 由 JwtInterceptor 保护，当前用户名从请求属性中取得。
 */
@RestController
@RequestMapping("/api/ai/report")
public class AiReportHistoryController {

    private final AiReportMapper aiReportMapper;
    private final UserService userService;

    public AiReportHistoryController(AiReportMapper aiReportMapper, UserService userService) {
        this.aiReportMapper = aiReportMapper;
        this.userService = userService;
    }

    /**
     * 查询当前用户指定类型的 AI 报告历史列表。
     *
     * @param type 报告类型：note-weekly | todo-weekly | note-monthly | todo-monthly
     * @param limit 最多返回条数，默认 12
     */
    @GetMapping("/history")
    public Result<List<AiReport>> history(@RequestParam(value = "type", required = false) String type,
                                          @RequestParam(value = "limit", defaultValue = "12") Integer limit,
                                          HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        User user = userService.getByUsername(username);
        if (user == null || user.getId() == null) {
            return Result.error(401, "用户不存在");
        }
        QueryWrapper<AiReport> qw = new QueryWrapper<AiReport>()
                .eq("user_id", user.getId())
                .orderByDesc("generated_at");
        if (type != null && !type.trim().isEmpty()) {
            qw.eq("report_type", type);
        }
        int safeLimit = limit == null || limit <= 0 ? 12 : limit;
        qw.last("LIMIT " + safeLimit);
        List<AiReport> list = aiReportMapper.selectList(qw);
        if (list == null) {
            list = Collections.emptyList();
        }
        return Result.success(list);
    }
}
