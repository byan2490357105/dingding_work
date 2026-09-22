package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.AiReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 报告历史 Mapper，继承 MyBatis-Plus BaseMapper。
 */
@Mapper
public interface AiReportMapper extends BaseMapper<AiReport> {
}
