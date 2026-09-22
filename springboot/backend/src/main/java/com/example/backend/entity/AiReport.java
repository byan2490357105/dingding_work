package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 报告历史记录，对应数据库 ai_report 表。
 * 每次 AI 生成新报告时写入一条，支持按月回看对比。
 */
@Data
@TableName("ai_report")
public class AiReport {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 报告类型：note-weekly | todo-weekly | note-monthly | todo-monthly */
    private String reportType;

    /** 报告覆盖的时间范围描述 */
    private String period;

    /** AI 生成的报告正文（Markdown） */
    private String content;

    /** 生成时的数据指纹，用于缓存命中判断 */
    private String dataFingerprint;

    /** 生成时间 */
    private LocalDateTime generatedAt;
}
