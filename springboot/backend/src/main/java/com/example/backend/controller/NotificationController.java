package com.example.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.Result;
import com.example.backend.entity.Notification;
import com.example.backend.service.NotificationService;
import com.example.backend.service.UserService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息接口。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @Resource
    private UserService userService;

    /** 分页查询消息列表 */
    @GetMapping
    public Result<Page<Notification>> list(
            @RequestAttribute("username") String username,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = userService.getByUsername(username).getId();
        return Result.success(notificationService.listPage(userId, pageNum, pageSize));
    }

    /** 未读数量 */
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount(@RequestAttribute("username") String username) {
        Long userId = userService.getByUsername(username).getId();
        Map<String, Long> map = new HashMap<>(1);
        map.put("count", notificationService.unreadCount(userId));
        return Result.success(map);
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success(null);
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead(@RequestAttribute("username") String username) {
        Long userId = userService.getByUsername(username).getId();
        notificationService.markAllRead(userId);
        return Result.success(null);
    }
}
