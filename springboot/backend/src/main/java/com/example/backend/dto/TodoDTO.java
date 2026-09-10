package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 待办任务前后端交互 DTO。
 */
@Data
public class TodoDTO {

    /** 待办主键 */
    private Long id;

    /** 待办标题 */
    private String title;

    /** 待办内容（富文本 HTML） */
    private String content;

    /** 开始时间，格式 yyyy-MM-dd HH:mm */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    /** 结束/截止时间，格式 yyyy-MM-dd HH:mm */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    /** 是否每日重复 */
    private Boolean daily;

    /** 每日重复的截止日期，格式 yyyy-MM-dd，为空表示长期每天重复 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate repeatUntil;

    /** 是否置顶 */
    private Boolean top;

    /** 是否已完成（持久化字段，可由“标记完成/取消完成”接口修改） */
    private Boolean completed;

    /**
     * 派生状态（只读，由后端计算）：
     * 已完成：is_completed = 1；
     * 逾期：未完成且 endTime 已过；
     * 进行中：未完成且未到截止时间。
     */
    private String status;

    /** 标签 */
    private String label;

    /** 创建日期，格式 yyyy-MM-dd HH:mm */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdAt;

    /** 修改时间，格式 yyyy-MM-dd HH:mm */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime updatedAt;
}
