package com.example.backend.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预测待办任务完成成功率的请求。
 * 字段与新建待办一致。
 */
@Data
public class PredictDTO {

    private String title;

    private String content;

    private String label;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime endTime;

    /** 是否每日重复 */
    private Boolean daily;

    /** 是否置顶 */
    private Boolean top;
}
