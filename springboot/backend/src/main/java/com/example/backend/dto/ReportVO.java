package com.example.backend.dto;

import lombok.Data;

/**
 * AI 月报响应。
 */
@Data
public class ReportVO {

    /** 月报覆盖的时间范围描述，例如“2026-08-15 至 2026-09-15” */
    private String period;

    /** 数据条数（笔记数或待办数） */
    private int count;

    /** AI 生成的月报正文（Markdown 格式） */
    private String content;

    /** 是否来自缓存（数据未更新时复用历史报告，未消耗 Token） */
    private boolean cached;
}
