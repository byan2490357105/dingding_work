package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 钉钉表格单元格区域更新参数。
 */
@Data
public class DingTalkRangeUpdateDTO {

    /** 表格文件 id */
    private String workbookId;

    /** 区域，例如 Sheet1!A1:C10 */
    private String range;

    /** 二维数组数据 */
    private List<List<Object>> values;
}
