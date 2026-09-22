package com.example.backend.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据分析仪表盘响应。
 * 用于前端 Dashboard 页展示统计卡片、待办完成趋势、笔记热力图、标签分布。
 */
@Data
public class StatsVO {

    /** 近 30 天每天的待办总数与已完成数，元素形如 {date, total, completed} */
    private List<Map<String, Object>> todoCompletionTrend;

    /** 近一年每天的笔记数，元素形如 {date, count} */
    private List<Map<String, Object>> noteHeatmap;

    /** 标签分布：key 为 "notes" / "todos"，value 为 [{tag, count}] */
    private Map<String, List<Map<String, Object>>> tagDistribution;

    /** 笔记总数 */
    private int totalNotes;

    /** 待办总数 */
    private int totalTodos;

    /** 已完成待办数 */
    private int completedTodos;

    /** 完成率（0-100） */
    private double completionRate;

    /** 本月新增笔记数 */
    private int monthNotes;
}
