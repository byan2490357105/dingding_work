package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户笔记实体，对应数据库 note 表。
 */
@Data
@TableName("note")
public class Note {

    /** 笔记主键，数据库自增 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 id（外键，关联 user.id） */
    private Long userId;

    /** 笔记标题 */
    private String title;

    /** 笔记内容（富文本 HTML） */
    private String content;

    /** 创建时间（精确到分钟） */
    private LocalDateTime createTime;

    /** 修改时间（未修改时等于创建时间，精确到分钟） */
    private LocalDateTime updateTime;

    /** 是否删除：0 否，1 是。逻辑删除标志，由 Service 层手动管理 */
    private Integer isDeleted;

    /** 是否置顶：0 否，1 是 */
    private Integer isPinned;

    /** 标签 */
    private String tag;
}
