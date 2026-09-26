package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内消息（待办提醒、逾期通知、AI 报告等）。
 */
@Data
@TableName("notification")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 消息类型：TODO_REMIND | TODO_OVERDUE | AI_REPORT */
    private String type;

    private String title;

    /** 消息内容（HTML 格式，与邮件正文一致） */
    private String content;

    /** 关联的 todo.id 或 ai_report.id，用于去重 */
    private Long relatedId;

    /** 是否已读（0 未读，1 已读） */
    @TableField("is_read")
    private Boolean isRead;

    private LocalDateTime createdAt;
}
