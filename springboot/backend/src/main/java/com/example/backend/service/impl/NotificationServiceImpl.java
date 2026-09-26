package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.service.NotificationService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public void create(Long userId, String type, String title, String content, Long relatedId) {
        // 去重：同用户、同类型、同关联 ID 的消息不重复创建
        if (relatedId != null) {
            LambdaQueryWrapper<Notification> check = new LambdaQueryWrapper<>();
            check.eq(Notification::getUserId, userId)
                    .eq(Notification::getType, type)
                    .eq(Notification::getRelatedId, relatedId);
            if (notificationMapper.selectCount(check) > 0) {
                return;
            }
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setRelatedId(relatedId);
        n.setIsRead(false);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    @Override
    public Page<Notification> listPage(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        return notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public long unreadCount(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false);
            return notificationMapper.selectCount(wrapper);
    }

    @Override
    public void markRead(Long id) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getId, id)
                .set(Notification::getIsRead, true);
        notificationMapper.update(null, wrapper);
    }

    @Override
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<Notification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false)
                .set(Notification::getIsRead, true);
        notificationMapper.update(null, wrapper);
    }
}
