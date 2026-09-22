package com.example.backend.dto;

import lombok.Data;

/**
 * AI 预测待办完成成功率的响应。
 */
@Data
public class PredictVO {

    /** 成功率，0-100 的整数 */
    private int rate;

    /** 等级描述：低 / 中 / 高 */
    private String level;

    /** AI 给出的分析说明 */
    private String reason;
}
