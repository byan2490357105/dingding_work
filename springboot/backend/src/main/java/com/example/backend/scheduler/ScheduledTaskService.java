package com.example.backend.scheduler;

import com.example.backend.ai.AiReportService;
import com.example.backend.dto.ReportVO;
import com.example.backend.entity.Todo;
import com.example.backend.entity.User;
import com.example.backend.service.MailService;
import com.example.backend.service.NotificationService;
import com.example.backend.service.TodoService;
import com.example.backend.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 定时任务：扫描用户待办并通过邮件推送提醒 / 摘要 / AI 周报。
 * 同时写入站内消息（notification 表），供个人消息栏展示。
 */
@Service
public class ScheduledTaskService {

    private final UserService userService;
    private final TodoService todoService;
    private final MailService mailService;
    private final AiReportService aiReportService;
    private final NotificationService notificationService;

    public ScheduledTaskService(UserService userService,
                                 TodoService todoService,
                                 MailService mailService,
                                 AiReportService aiReportService,
                                 NotificationService notificationService) {
        this.userService = userService;
        this.todoService = todoService;
        this.mailService = mailService;
        this.aiReportService = aiReportService;
        this.notificationService = notificationService;
    }

    /**
     * 每天 8:00：扫描逾期与今日将到期未完成待办，发送提醒邮件。
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void scanOverdueAndRemind() {
        List<User> users = userService.listUsersWithEmail();
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            try {
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    continue;
                }
                // listForExport 返回当前用户所有未删除待办
                List<Todo> allTodos = todoService.listForExport(username);
                if (allTodos == null || allTodos.isEmpty()) {
                    continue;
                }
                LocalDateTime now = LocalDateTime.now();
                LocalDate today = now.toLocalDate();

                // 逾期：未完成 && endTime != null && endTime < now
                List<Todo> overdue = allTodos.stream()
                        .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                        .filter(t -> t.getEndTime() != null && t.getEndTime().isBefore(now))
                        .collect(Collectors.toList());

                // 今日将到期：未完成 && endTime 在今天内
                List<Todo> dueToday = allTodos.stream()
                        .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                        .filter(t -> t.getEndTime() != null
                                && t.getEndTime().toLocalDate().equals(today)
                                && !t.getEndTime().isBefore(now))
                        .collect(Collectors.toList());

                if (overdue.isEmpty() && dueToday.isEmpty()) {
                    continue;
                }
                mailService.sendTodoReminder(user.getEmail(), username,
                        new ArrayList<>(overdue), new ArrayList<>(dueToday));
                // 写入站内消息：逾期待办逐条通知
                for (Todo todo : overdue) {
                    String html = mailService.buildTodoReminderHtml(username,
                            Collections.singletonList(todo), Collections.emptyList());
                    notificationService.create(user.getId(), "TODO_OVERDUE",
                            "待办逾期：" + safe(todo.getTitle()), html, todo.getId());
                }
            } catch (Exception e) {
                // 单个用户处理失败不影响其他用户
                System.err.println("[ScheduledTaskService] scanOverdueAndRemind user="
                        + user.getUsername() + " err=" + e.getMessage());
            }
        }
    }

    /**
     * 每天 9:00：发送今日未完成待办摘要邮件。
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailySummary() {
        List<User> users = userService.listUsersWithEmail();
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            try {
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    continue;
                }
                List<Todo> allTodos = todoService.listForExport(username);
                if (allTodos == null || allTodos.isEmpty()) {
                    continue;
                }
                // 未完成待办
                List<Todo> pending = allTodos.stream()
                        .filter(t -> !Boolean.TRUE.equals(t.getCompleted()))
                        .collect(Collectors.toList());
                if (pending.isEmpty()) {
                    continue;
                }
                mailService.sendDailySummary(user.getEmail(), username, new ArrayList<>(pending));
            } catch (Exception e) {
                System.err.println("[ScheduledTaskService] sendDailySummary user="
                        + user.getUsername() + " err=" + e.getMessage());
            }
        }
    }

    /**
     * 每周一 10:00：调用 AiReportService 生成周报并发送邮件。
     * 注意：AiReportService.todoWeeklyReport 由另一个 agent 同步新增，签名
     * ReportVO todoWeeklyReport(String username)。
     */
    @Scheduled(cron = "0 0 10 * * MON")
    public void generateWeeklyReportArchive() {
        List<User> users = userService.listUsersWithEmail();
        if (users == null || users.isEmpty()) {
            return;
        }
        for (User user : users) {
            try {
                String username = user.getUsername();
                if (username == null || username.trim().isEmpty()) {
                    continue;
                }
                ReportVO report = aiReportService.todoWeeklyReport(username);
                if (report == null) {
                    continue;
                }
                String content = report.getContent();
                if (content == null || content.trim().isEmpty()) {
                    continue;
                }
                mailService.sendWeeklyReport(user.getEmail(), username, content);
                // 写入站内消息
                String reportHtml = mailService.buildWeeklyReportHtml(username, content);
                notificationService.create(user.getId(), "AI_REPORT",
                        "AI 待办周报", reportHtml, null);
            } catch (Exception e) {
                System.err.println("[ScheduledTaskService] generateWeeklyReportArchive user="
                        + user.getUsername() + " err=" + e.getMessage());
            }
        }
    }

    /**
     * 每分钟扫描：到达 remind_time 且未发送提醒的待办，向用户邮箱发送提醒邮件。
     * 发送成功后标记 reminded=true，防止重复推送。
     */
    @Scheduled(cron = "0 * * * * *")
    public void scanAndSendEmailReminder() {
        List<Todo> todos = todoService.listTodosToRemind();
        if (todos == null || todos.isEmpty()) {
            return;
        }
        for (Todo todo : todos) {
            try {
                User user = userService.getById(todo.getUserId());
                if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    // 用户无邮箱，跳过但仍标记已提醒，避免重复扫描
                    todoService.markReminded(todo.getId());
                    continue;
                }
                String username = user.getUsername() == null ? "" : user.getUsername();
                mailService.sendSingleTodoReminder(user.getEmail(), username, todo);
                // 写入站内消息
                String reminderHtml = mailService.buildSingleTodoReminderHtml(username, todo);
                notificationService.create(user.getId(), "TODO_REMIND",
                        "待办提醒：" + safe(todo.getTitle()), reminderHtml, todo.getId());
                // 无论邮件服务是否实际送达，都标记已提醒（邮件服务内部已记录日志）
                todoService.markReminded(todo.getId());
            } catch (Exception e) {
                System.err.println("[ScheduledTaskService] scanAndSendEmailReminder todoId="
                        + todo.getId() + " err=" + e.getMessage());
            }
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
