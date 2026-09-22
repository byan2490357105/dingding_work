package com.example.backend.controller;

import com.example.backend.ai.AiReportService;
import com.example.backend.common.Result;
import com.example.backend.dto.PredictDTO;
import com.example.backend.dto.PredictVO;
import com.example.backend.dto.ReportVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * AI 月报与成功率预测接口。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiReportService aiReportService;

    /** 生成近一个月笔记月报 */
    @GetMapping("/report/notes")
    public Result<ReportVO> noteReport(@RequestAttribute("username") String username) {
        return Result.success(aiReportService.noteMonthlyReport(username));
    }

    /** 生成近一个月待办月报（含未完成原因分析） */
    @GetMapping("/report/todo")
    public Result<ReportVO> todoReport(@RequestAttribute("username") String username) {
        return Result.success(aiReportService.todoMonthlyReport(username));
    }

    /** 生成近一周笔记周报 */
    @GetMapping("/report/notes/weekly")
    public Result<ReportVO> noteWeeklyReport(@RequestAttribute("username") String username) {
        return Result.success(aiReportService.noteWeeklyReport(username));
    }

    /** 生成近一周待办周报 */
    @GetMapping("/report/todo/weekly")
    public Result<ReportVO> todoWeeklyReport(@RequestAttribute("username") String username) {
        return Result.success(aiReportService.todoWeeklyReport(username));
    }

    /** 预测新待办任务的完成成功率 */
    @PostMapping("/predict")
    public Result<PredictVO> predict(@RequestAttribute("username") String username,
                                     @RequestBody(required = false) PredictDTO dto) {
        return Result.success(aiReportService.predictSuccessRate(username, dto));
    }
}
