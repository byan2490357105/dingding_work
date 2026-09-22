package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户待办任务。
 */
@Data
@TableName("todo")
public class Todo {

    /** 待办主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户 id，外键关联 user.id */
    private Long userId;

    /** 待办标题 */
    private String title;

    /** 待办内容（富文本 HTML） */
    private String content;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束/截止时间 */
    private LocalDateTime endTime;

    /** 是否每日重复（0 否，1 是） */
    @TableField("is_daily")
    private Boolean daily;

    /** 每日重复的截止日期，为空表示长期每天重复 */
    private LocalDate repeatUntil;

    /** 是否删除（逻辑删除，由 Service 层手动管理） */
    @TableField("is_deleted")
    private Boolean deleted;

    /** 是否置顶 */
    @TableField("is_top")
    private Boolean top;

    /** 是否完成（0 未完成，1 已完成）；逾期状态由 end_time 与当前时间比较派生，不入库 */
    @TableField("is_completed")
    private Boolean completed;

    /** 提醒时间：到达该时间时发送邮件提醒，为空表示不提醒 */
    @TableField("remind_time")
    private LocalDateTime remindTime;

    /** 是否已发送提醒（0 未发送，1 已发送），防止重复推送 */
    @TableField("is_reminded")
    private Boolean reminded;

    /** 标签 */
    private String label;

    /** 创建日期 */
    private LocalDateTime createdAt;

    /** 修改时间，未修改时等于创建时间 */
    private LocalDateTime updatedAt;
}
