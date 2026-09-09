package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Todo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TodoMapper extends BaseMapper<Todo> {
}
