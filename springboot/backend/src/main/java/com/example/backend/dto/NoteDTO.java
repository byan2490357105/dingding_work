package com.example.backend.dto;

import lombok.Data;

/**
 * 前后端交互的笔记 DTO，用于新建/修改笔记。
 */
@Data
public class NoteDTO {

    /** 笔记 id（修改时必传，新建时不传） */
    private Long id;

    /** 笔记标题 */
    private String title;

    /** 笔记内容（富文本 HTML） */
    private String content;

    /** 标签（学习/生活/科研/出行/自定义） */
    private String tag;

    /** 是否置顶：0 否，1 是（置顶切换时使用） */
    private Integer isPinned;
}
