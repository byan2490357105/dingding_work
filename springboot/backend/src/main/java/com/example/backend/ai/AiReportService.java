package com.example.backend.ai;

import com.example.backend.dto.PredictDTO;
import com.example.backend.dto.PredictVO;
import com.example.backend.dto.ReportVO;
import com.example.backend.entity.Note;
import com.example.backend.entity.Todo;
import com.example.backend.service.NoteService;
import com.example.backend.service.TodoService;
import com.example.backend.util.ExportUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * AI 月报 / 周报与成功率预测服务。
 * 所有 AI 调用统一通过 DeepSeekService。
 * 月报与周报统一接入 AiReportCacheService 做数据指纹缓存（基于 MySQL ai_report 表，无需 Redis）。
 */
@Service
public class AiReportService {

    @Resource
    private DeepSeekService deepSeekService;

    @Resource
    private NoteService noteService;

    @Resource
    private TodoService todoService;

    @Resource
    private AiReportCacheService cacheService;

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ==================== 笔记月报 ====================

    public ReportVO noteMonthlyReport(String username) {
        return cacheService.getOrGenerate(username, "note-monthly", () -> generateNoteMonthly(username));
    }

    private ReportVO generateNoteMonthly(String username) {
        List<Note> notes = noteService.listLastMonth(username);
        ReportVO vo = new ReportVO();
        vo.setPeriod(lastMonthPeriod());
        vo.setCount(notes.size());

        if (notes.isEmpty()) {
            vo.setContent("过去一个月你没有写任何笔记，本月报内容为空。快去记录点什么吧！");
            return vo;
        }

        StringBuilder data = new StringBuilder();
        int index = 1;
        for (Note note : notes) {
            data.append(index++).append(". 【标题】").append(safe(note.getTitle()))
                    .append(" 【标签】").append(safe(note.getTag()))
                    .append(" 【创建时间】").append(format(note.getCreateTime()))
                    .append(" 【内容】").append(ExportUtil.htmlToText(note.getContent()))
                    .append("\n");
        }

        String systemPrompt = "你是一位贴心的个人成长助手，擅长从用户笔记中提炼内容、洞察情绪与习惯。"
                + "请根据用户近一个月的笔记，生成一份温暖、有洞察力的月报。";
        String userPrompt = "以下是用户近一个月的笔记（按创建时间倒序），共 " + notes.size() + " 篇：\n\n"
                + data.toString()
                + "\n请按以下结构生成月报（使用 Markdown）：\n"
                + "1. **本月概览**：一句话总结本月笔记的整体状态。\n"
                + "2. **内容主题分布**：按标签或主题归类，列出每类大概有多少篇、主要在写什么。\n"
                + "3. **精彩回顾**：挑选 3-5 条最有代表性的笔记，简要引用并点评。\n"
                + "4. **情绪与状态分析**：结合笔记内容，分析用户本月的情绪倾向（积极/平静/焦虑等），给出温柔的反馈。\n"
                + "5. **下月建议**：基于本月记录，给 2-3 条具体、可执行的建议。\n"
                + "要求：语言温暖真诚，不要空洞套话，篇幅控制在 800 字以内。";

        vo.setContent(deepSeekService.chat(systemPrompt, userPrompt));
        return vo;
    }

    // ==================== 笔记周报 ====================

    public ReportVO noteWeeklyReport(String username) {
        return cacheService.getOrGenerate(username, "note-weekly", () -> generateNoteWeekly(username));
    }

    private ReportVO generateNoteWeekly(String username) {
        List<Note> notes = noteService.listLastWeek(username);
        ReportVO vo = new ReportVO();
        vo.setPeriod(lastWeekPeriod());
        vo.setCount(notes.size());

        if (notes.isEmpty()) {
            vo.setContent("过去一周你没有写任何笔记，本周报内容为空。快去记录点什么吧！");
            return vo;
        }

        StringBuilder data = new StringBuilder();
        int index = 1;
        for (Note note : notes) {
            data.append(index++).append(". 【标题】").append(safe(note.getTitle()))
                    .append(" 【标签】").append(safe(note.getTag()))
                    .append(" 【创建时间】").append(format(note.getCreateTime()))
                    .append(" 【内容】").append(ExportUtil.htmlToText(note.getContent()))
                    .append("\n");
        }

        String systemPrompt = "你是一位贴心的个人成长助手，擅长从用户笔记中提炼内容、洞察情绪与习惯。"
                + "请根据用户近一周的笔记，生成一份简洁、温暖的周报。";
        String userPrompt = "以下是用户近一周的笔记（按创建时间倒序），共 " + notes.size() + " 篇：\n\n"
                + data.toString()
                + "\n请按以下结构生成周报（使用 Markdown）：\n"
                + "1. **本周概览**：一句话总结本周笔记的整体状态。\n"
                + "2. **精彩回顾**：挑选 2-3 条最有代表性的笔记，简要引用并点评。\n"
                + "3. **下周建议**：基于本周记录，给 1-2 条具体、可执行的建议。\n"
                + "要求：语言温暖真诚，不要空洞套话，篇幅控制在 500 字以内。";

        vo.setContent(deepSeekService.chat(systemPrompt, userPrompt));
        return vo;
    }

    // ==================== 待办月报 ====================

    public ReportVO todoMonthlyReport(String username) {
        return cacheService.getOrGenerate(username, "todo-monthly", () -> generateTodoMonthly(username));
    }

    private ReportVO generateTodoMonthly(String username) {
        List<Todo> todos = todoService.listLastMonth(username);
        ReportVO vo = new ReportVO();
        vo.setPeriod(lastMonthPeriod());
        vo.setCount(todos.size());

        if (todos.isEmpty()) {
            vo.setContent("过去一个月你没有创建任何待办任务，本月报内容为空。");
            return vo;
        }

        StringBuilder data = new StringBuilder();
        int index = 1;
        for (Todo todo : todos) {
            boolean completed = Boolean.TRUE.equals(todo.getCompleted());
            boolean overdue = !completed && todo.getEndTime() != null
                    && todo.getEndTime().isBefore(LocalDateTime.now());
            String state = completed ? "已完成" : (overdue ? "已逾期" : "进行中");
            data.append(index++).append(". 【标题】").append(safe(todo.getTitle()))
                    .append(" 【标签】").append(safe(todo.getLabel()))
                    .append(" 【状态】").append(state)
                    .append(" 【开始】").append(format(todo.getStartTime()))
                    .append(" 【截止】").append(format(todo.getEndTime()))
                    .append(" 【内容】").append(ExportUtil.htmlToText(todo.getContent()))
                    .append("\n");
        }

        long completedCount = todos.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        long overdueCount = todos.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())
                && t.getEndTime() != null && t.getEndTime().isBefore(LocalDateTime.now())).count();
        long ongoingCount = todos.size() - completedCount - overdueCount;

        String systemPrompt = "你是一位高效的时间管理教练，擅长分析用户的待办执行情况，"
                + "帮助用户发现拖延原因并提升执行力。请保持客观、专业、富有建设性。";
        String userPrompt = "以下是用户近一个月的待办任务数据（按创建时间倒序）：\n\n"
                + data.toString()
                + "\n统计：共 " + todos.size() + " 条，已完成 " + completedCount
                + " 条，已逾期未完成 " + overdueCount + " 条，进行中 " + ongoingCount + " 条。\n\n"
                + "请按以下结构生成月报（使用 Markdown）：\n"
                + "1. **本月执行概览**：基于统计数据，客观评价本月待办完成情况。\n"
                + "2. **完成情况分析**：总结已完成任务的共同特征（如标签、耗时、时间段），提炼用户的高效模式。\n"
                + "3. **未完成原因分析**：重点分析逾期未完成任务的原因，从任务本身（难度、时长、截止时间设置）、"
                + "用户行为（拖延、优先级低、任务拆分不当）等角度给出判断。\n"
                + "4. **改进建议**：针对未完成原因，给出 3-4 条具体可执行的改进建议。\n"
                + "5. **下月目标**：建议用户下月在待办管理上可以重点改进的 1-2 个方向。\n"
                + "要求：分析要有数据支撑，建议要具体可操作，避免空泛说教，篇幅控制在 800 字以内。";

        vo.setContent(deepSeekService.chat(systemPrompt, userPrompt));
        return vo;
    }

    // ==================== 待办周报 ====================

    public ReportVO todoWeeklyReport(String username) {
        return cacheService.getOrGenerate(username, "todo-weekly", () -> generateTodoWeekly(username));
    }

    private ReportVO generateTodoWeekly(String username) {
        List<Todo> todos = todoService.listLastWeek(username);
        ReportVO vo = new ReportVO();
        vo.setPeriod(lastWeekPeriod());
        vo.setCount(todos.size());

        if (todos.isEmpty()) {
            vo.setContent("过去一周你没有创建任何待办任务，本周报内容为空。");
            return vo;
        }

        StringBuilder data = new StringBuilder();
        int index = 1;
        for (Todo todo : todos) {
            boolean completed = Boolean.TRUE.equals(todo.getCompleted());
            boolean overdue = !completed && todo.getEndTime() != null
                    && todo.getEndTime().isBefore(LocalDateTime.now());
            String state = completed ? "已完成" : (overdue ? "已逾期" : "进行中");
            data.append(index++).append(". 【标题】").append(safe(todo.getTitle()))
                    .append(" 【标签】").append(safe(todo.getLabel()))
                    .append(" 【状态】").append(state)
                    .append(" 【开始】").append(format(todo.getStartTime()))
                    .append(" 【截止】").append(format(todo.getEndTime()))
                    .append(" 【内容】").append(ExportUtil.htmlToText(todo.getContent()))
                    .append("\n");
        }

        long completedCount = todos.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        long overdueCount = todos.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())
                && t.getEndTime() != null && t.getEndTime().isBefore(LocalDateTime.now())).count();
        long ongoingCount = todos.size() - completedCount - overdueCount;

        String systemPrompt = "你是一位高效的时间管理教练，擅长分析用户的待办执行情况。"
                + "请保持客观、专业、富有建设性。";
        String userPrompt = "以下是用户近一周的待办任务数据（按创建时间倒序）：\n\n"
                + data.toString()
                + "\n统计：共 " + todos.size() + " 条，已完成 " + completedCount
                + " 条，已逾期未完成 " + overdueCount + " 条，进行中 " + ongoingCount + " 条。\n\n"
                + "请按以下结构生成周报（使用 Markdown）：\n"
                + "1. **本周执行概览**：基于统计数据，客观评价本周待办完成情况。\n"
                + "2. **精彩回顾**：挑选 2-3 条本周值得关注的任务（已完成的高光时刻或需关注的逾期任务），简要点评。\n"
                + "3. **下周建议**：基于本周情况，给 2-3 条具体可执行的改进建议。\n"
                + "要求：分析要有数据支撑，建议要具体可操作，避免空泛说教，篇幅控制在 500 字以内。";

        vo.setContent(deepSeekService.chat(systemPrompt, userPrompt));
        return vo;
    }

    // ==================== 新建任务成功率预测 ====================

    public PredictVO predictSuccessRate(String username, PredictDTO dto) {
        // 拉取用户近 30 天的历史任务作为上下文
        List<Todo> history = todoService.listLastMonth(username);

        long completed = history.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
        long overdue = history.stream().filter(t -> !Boolean.TRUE.equals(t.getCompleted())
                && t.getEndTime() != null && t.getEndTime().isBefore(LocalDateTime.now())).count();

        StringBuilder historyData = new StringBuilder();
        int index = 1;
        for (Todo todo : history) {
            boolean done = Boolean.TRUE.equals(todo.getCompleted());
            boolean over = !done && todo.getEndTime() != null && todo.getEndTime().isBefore(LocalDateTime.now());
            historyData.append(index++).append(". 【标题】").append(safe(todo.getTitle()))
                    .append(" 【标签】").append(safe(todo.getLabel()))
                    .append(" 【状态】").append(done ? "已完成" : (over ? "已逾期" : "进行中"))
                    .append(" 【时长(小时)】").append(hoursBetween(todo.getStartTime(), todo.getEndTime()))
                    .append("\n");
        }

        String systemPrompt = "你是一位任务完成率预测专家。请结合用户的历史任务完成情况和新任务的特征，"
                + "预测该用户完成这条新任务的成功率，并给出分析。请严格只输出 JSON，不要输出 Markdown 代码块。";

        long durationHours = hoursBetween(dto.getStartTime(), dto.getEndTime());

        String userPrompt = "用户历史任务（近30天，共 " + history.size() + " 条，完成 " + completed
                + " 条，逾期 " + overdue + " 条）：\n" + historyData.toString()
                + "\n现在用户要创建一条新任务：\n"
                + "【标题】" + safe(dto.getTitle())
                + " 【标签】" + safe(dto.getLabel())
                + " 【内容】" + ExportUtil.htmlToText(dto.getContent())
                + " 【计划时长(小时)】" + durationHours
                + " 【是否每日重复】" + (Boolean.TRUE.equals(dto.getDaily()) ? "是" : "否")
                + " 【是否置顶】" + (Boolean.TRUE.equals(dto.getTop()) ? "是" : "否")
                + "\n\n请结合历史完成率（完成数/总数）以及新任务与历史已完成/已逾期任务的相似程度，"
                + "预测完成成功率。严格只输出如下 JSON，不要有任何其他文字：\n"
                + "{\"rate\": 整数0-100, \"level\": \"高|中|低\", \"reason\": \"不超过120字的分析说明，"
                + "说明为什么是这个成功率，并参考了哪些历史规律\"}";

        String json = deepSeekService.chat(systemPrompt, userPrompt);
        return parsePredictJson(json);
    }

    // ==================== 私有辅助 ====================

    private PredictVO parsePredictJson(String raw) {
        PredictVO vo = new PredictVO();
        vo.setRate(50);
        vo.setLevel("中");
        vo.setReason("AI 分析结果解析异常，默认预测为中等成功率。");
        if (raw == null) {
            return vo;
        }
        // 去除可能的 ```json 包裹
        String text = raw.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```json\\s*", "").replaceFirst("^```\\s*", "");
            int end = text.lastIndexOf("```");
            if (end > 0) {
                text = text.substring(0, end);
            }
        }
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(text.trim());
            int rate = node.path("rate").asInt(50);
            if (rate < 0) rate = 0;
            if (rate > 100) rate = 100;
            vo.setRate(rate);
            String level = node.path("level").asText("中");
            if (level.isEmpty()) {
                level = rate >= 70 ? "高" : (rate >= 40 ? "中" : "低");
            }
            vo.setLevel(level);
            vo.setReason(node.path("reason").asText(vo.getReason()));
        } catch (Exception e) {
            // 解析失败时返回默认值
        }
        return vo;
    }

    private long hoursBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        long minutes = java.time.Duration.between(start, end).toMinutes();
        return Math.max(0, Math.round(minutes / 60.0));
    }

    private String lastMonthPeriod() {
        LocalDateTime now = LocalDateTime.now();
        return now.minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " 至 " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String lastWeekPeriod() {
        LocalDateTime now = LocalDateTime.now();
        return now.minusDays(7).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                + " 至 " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(MINUTE_FMT);
    }
}
