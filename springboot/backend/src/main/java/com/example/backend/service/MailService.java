package com.example.backend.service;

import com.example.backend.entity.Todo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 邮件推送服务：组装 HTML 邮件并通过 JavaMailSender 发送。
 * 所有方法均吞掉异常（仅打印到 stderr），保证定时任务不会因单条邮件失败而中断。
 */
@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String from;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** 待办提醒：列出逾期待办与今日将到期待办 */
    public void sendTodoReminder(String toEmail, String username,
                                 List<Todo> overdueTodos, List<Todo> dueTodayTodos) {
        try {
            String subject = "【随手工作台】待办提醒";
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;color:#333;\">");
            html.append("<h3>你好，").append(escape(username)).append("，今天的待办提醒来啦</h3>");

            html.append("<h4 style=\"color:#d9534f;\">⚠ 逾期待办（").append(overdueTodos.size()).append("）</h4>");
            if (overdueTodos.isEmpty()) {
                html.append("<p>暂无逾期待办，表现很棒！</p>");
            } else {
                appendTodoList(html, overdueTodos, true);
            }

            html.append("<h4 style=\"color:#0275d8;\">📅 今日将到期待办（").append(dueTodayTodos.size()).append("）</h4>");
            if (dueTodayTodos.isEmpty()) {
                html.append("<p>今天没有将到期的待办</p>");
            } else {
                appendTodoList(html, dueTodayTodos, false);
            }

            html.append("<hr><p style=\"font-size:12px;color:#999;\">本邮件由随手工作台自动发送，请勿直接回复</p>");
            html.append("</div>");

            sendHtml(toEmail, subject, html.toString());
        } catch (Exception e) {
            System.err.println("[MailService] sendTodoReminder 失败 to=" + toEmail + " err=" + e.getMessage());
        }
    }

    /** 今日待办摘要：列出所有未完成待办 */
    public void sendDailySummary(String toEmail, String username, List<Todo> pendingTodos) {
        try {
            String subject = "【随手工作台】今日待办摘要";
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;color:#333;\">");
            html.append("<h3>你好，").append(escape(username)).append("，这是你今天的待办摘要</h3>");

            html.append("<h4>📋 未完成待办（").append(pendingTodos.size()).append("）</h4>");
            if (pendingTodos.isEmpty()) {
                html.append("<p>今天没有未完成待办，享受轻松一天吧！</p>");
            } else {
                appendTodoList(html, pendingTodos, false);
            }

            html.append("<hr><p style=\"font-size:12px;color:#999;\">本邮件由随手工作台自动发送，请勿直接回复</p>");
            html.append("</div>");

            sendHtml(toEmail, subject, html.toString());
        } catch (Exception e) {
            System.err.println("[MailService] sendDailySummary 失败 to=" + toEmail + " err=" + e.getMessage());
        }
    }

    /** 单条待办提醒：到达用户设置的提醒时间时发送 */
    public void sendSingleTodoReminder(String toEmail, String username, Todo todo) {
        try {
            String subject = "【随手工作台】待办提醒：" + safe(todo.getTitle());
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;color:#333;line-height:1.6;\">");
            html.append("<h3>⏰ 提醒时间到了</h3>");
            html.append("<p>你好，").append(escape(username)).append("，你设置的待办提醒时间已到达：</p>");
            html.append("<table style=\"border-collapse:collapse;width:100%;font-size:14px;margin:12px 0;\">");
            html.append("<tr><th style=\"border:1px solid #ddd;padding:6px;text-align:left;background:#f5f5f5;\">标题</th>")
                    .append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(escape(safe(todo.getTitle()))).append("</td></tr>");
            html.append("<tr><th style=\"border:1px solid #ddd;padding:6px;text-align:left;background:#f5f5f5;\">开始时间</th>")
                    .append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(format(todo.getStartTime())).append("</td></tr>");
            html.append("<tr><th style=\"border:1px solid #ddd;padding:6px;text-align:left;background:#f5f5f5;\">截止时间</th>")
                    .append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(format(todo.getEndTime())).append("</td></tr>");
            html.append("</table>");
            if (todo.getContent() != null && !todo.getContent().trim().isEmpty()) {
                html.append("<p style=\"color:#666;\">任务内容：</p>");
                html.append("<div style=\"background:#f9f9f9;padding:10px;border-radius:4px;\">")
                        .append(escape(safe(todo.getContent())).replace("<br>", ""))
                        .append("</div>");
            }
            html.append("<hr><p style=\"font-size:12px;color:#999;\">本邮件由随手工作台自动发送，请勿直接回复</p>");
            html.append("</div>");
            sendHtml(toEmail, subject, html.toString());
        } catch (Exception e) {
            System.err.println("[MailService] sendSingleTodoReminder 失败 to=" + toEmail + " err=" + e.getMessage());
        }
    }

    /** AI 待办周报：直接发送 AI 生成的报告内容 */
    public void sendWeeklyReport(String toEmail, String username, String reportContent) {
        try {
            String subject = "【随手工作台】AI 待办周报";
            StringBuilder html = new StringBuilder();
            html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;color:#333;line-height:1.6;\">");
            html.append("<h3>你好，").append(escape(username)).append("，这是你本周的待办周报</h3>");
            // AI 内容为 Markdown 文本，按段落简单转 HTML
            String body = reportContent == null ? "" : reportContent;
            // 简单转义，避免 HTML 注入
            body = escape(body);
            // 按 \n\n 拆段，每段用 <p> 包裹；保留 ## 标题样式
            String[] blocks = body.split("\n\\s*\n");
            for (String block : blocks) {
                String trimmed = block.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("### ")) {
                    html.append("<h5>").append(trimmed.substring(4)).append("</h5>");
                } else if (trimmed.startsWith("## ")) {
                    html.append("<h4>").append(trimmed.substring(3)).append("</h4>");
                } else if (trimmed.startsWith("# ")) {
                    html.append("<h3>").append(trimmed.substring(2)).append("</h3>");
                } else {
                    // 行内换行替换为 <br>
                    html.append("<p>").append(trimmed.replace("\n", "<br>")).append("</p>");
                }
            }
            html.append("<hr><p style=\"font-size:12px;color:#999;\">本邮件由随手工作台自动发送，请勿直接回复</p>");
            html.append("</div>");

            sendHtml(toEmail, subject, html.toString());
        } catch (Exception e) {
            System.err.println("[MailService] sendWeeklyReport 失败 to=" + toEmail + " err=" + e.getMessage());
        }
    }

    // ==================== 内部辅助 ====================

    private void sendHtml(String toEmail, String subject, String html) throws Exception {
        if (from == null || from.trim().isEmpty()) {
            System.err.println("[MailService] 发件人邮箱未配置（spring.mail.from 为空），跳过发送 to=" + toEmail);
            return;
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from.trim());
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private void appendTodoList(StringBuilder html, List<Todo> todos, boolean showOverdue) {
        html.append("<table style=\"border-collapse:collapse;width:100%;font-size:14px;\">");
        html.append("<tr><th style=\"border:1px solid #ddd;padding:6px;text-align:left;\">标题</th>")
                .append("<th style=\"border:1px solid #ddd;padding:6px;text-align:left;\">截止时间</th>")
                .append("<th style=\"border:1px solid #ddd;padding:6px;text-align:left;\">状态</th></tr>");
        for (Todo todo : todos) {
            html.append("<tr>");
            html.append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(escape(safe(todo.getTitle()))).append("</td>");
            html.append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(format(todo.getEndTime())).append("</td>");
            String state;
            if (Boolean.TRUE.equals(todo.getCompleted())) {
                state = "已完成";
            } else if (showOverdue || (todo.getEndTime() != null
                    && todo.getEndTime().isBefore(LocalDateTime.now()))) {
                state = "<span style=\"color:#d9534f;\">已逾期</span>";
            } else {
                state = "进行中";
            }
            html.append("<td style=\"border:1px solid #ddd;padding:6px;\">").append(state).append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
    }

    private String format(LocalDateTime time) {
        return time == null ? "" : time.format(FMT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>");
    }
}
