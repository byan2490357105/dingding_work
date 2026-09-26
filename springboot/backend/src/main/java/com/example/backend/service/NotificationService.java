package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.Notification;

import java.util.List;

/**
 * 站内消息服务。
 */
public interface NotificationService {

    /**
     * 创建消息（同 type + relatedId + userId 已存在则跳过去重）。
     */
    void create(Long userId, String type, String title, String content, Long relatedId);

    /**
     * 分页查询用户消息（按创建时间降序）。
     */
    Page<Notification> listPage(Long userId, int pageNum, int pageSize);

    /**
     * 未读消息数量。
     */
    long unreadCount(Long userId);

    /**
     * 标记单条已读。
     */
    void markRead(Long id);

    /**
     * 标记用户所有消息已读。
     */
    void markAllRead(Long userId);
}
