package com.example.backend.controller;

import com.example.backend.ai.AiReportService;
import com.example.backend.common.Result;
import com.example.backend.dto.PredictDTO;
import com.example.backend.dto.PredictVO;
import com.example.backend.dto.ReportVO;
import com.example.backend.entity.User;
import com.example.backend.service.MailService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * AI 月报与成功率预测接口。
 * 生成报告时同时写入站内消息。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiReportService aiReportService;

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    /** 生成近一个月笔记月报 */
    @GetMapping("/report/notes")
    public Result<ReportVO> noteReport(@RequestAttribute("username") String username) {
        ReportVO vo = aiReportService.noteMonthlyReport(username);
        saveReportNotification(username, "AI 笔记月报", vo.getContent());
        return Result.success(vo);
    }

    /** 生成近一个月待办月报（含未完成原因分析） */
    @GetMapping("/report/todo")
    public Result<ReportVO> todoReport(@RequestAttribute("username") String username) {
        ReportVO vo = aiReportService.todoMonthlyReport(username);
        saveReportNotification(username, "AI 待办月报", vo.getContent());
        return Result.success(vo);
    }

    /** 生成近一周笔记周报 */
    @GetMapping("/report/notes/weekly")
    public Result<ReportVO> noteWeeklyReport(@RequestAttribute("username") String username) {
        ReportVO vo = aiReportService.noteWeeklyReport(username);
        saveReportNotification(username, "AI 笔记周报", vo.getContent());
        return Result.success(vo);
    }

    /** 生成近一周待办周报 */
    @GetMapping("/report/todo/weekly")
    public Result<ReportVO> todoWeeklyReport(@RequestAttribute("username") String username) {
        ReportVO vo = aiReportService.todoWeeklyReport(username);
        saveReportNotification(username, "AI 待办周报", vo.getContent());
        return Result.success(vo);
    }

    /** 预测新待办任务的完成成功率 */
    @PostMapping("/predict")
    public Result<PredictVO> predict(@RequestAttribute("username") String username,
                                     @RequestBody(required = false) PredictDTO dto) {
        return Result.success(aiReportService.predictSuccessRate(username, dto));
    }

    /** 将 AI 报告内容写入站内消息 */
    private void saveReportNotification(String username, String title, String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                return;
            }
            User user = userService.getByUsername(username);
            if (user == null) {
                return;
            }
            String html = mailService.buildWeeklyReportHtml(username, content);
            notificationService.create(user.getId(), "AI_REPORT", title, html, null);
        } catch (Exception e) {
            // 消息写入失败不影响报告返回
            System.err.println("[AiController] saveReportNotification err=" + e.getMessage());
        }
    }
}
