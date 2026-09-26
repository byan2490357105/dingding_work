package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.NoteDTO;
import com.example.backend.entity.Note;

import java.util.List;
import java.util.Map;

/**
 * 笔记服务。
 */
public interface NoteService {

    /**
     * 分页查询当前登录用户的笔记（置顶优先，按修改时间倒序）。
     *
     * @param pageNum      页码（从 1 开始）
     * @param pageSize     每页条数
     * @param keyword      标题/正文关键字，为空不过滤
     * @param excludeTags  需隐藏的标签集合（多选），为空不过滤
     */
    Page<Note> pageMyNotes(String username, long pageNum, long pageSize,
                           String keyword, List<String> excludeTags);

    /**
     * 统计当前用户各标签下的笔记数量（全量，供标签筛选面板展示）。
     *
     * @return 每项含 tag（标签名）和 count（数量），按数量倒序
     */
    List<Map<String, Object>> listTagStats(String username);

    /**
     * 基于当前用户全部笔记的标题与正文生成词云（2-gram 词频，标题/标签加权）。
     */
    List<Map<String, Object>> wordCloud(String username, int limit);

    /**
     * 查询当前登录用户的所有笔记，按置顶 / 未置顶分为两个列表。
     *
     * @param username 当前登录用户名（由 JWT 解析得到）
     * @return {"pinned": 置顶列表, "normal": 未置顶列表}
     */
    Map<String, List<Note>> listMyNotes(String username);

    /**
     * 查看笔记详情（仅能查看属于当前登录用户的笔记）。
     */
    Note getNote(String username, Long noteId);

    /**
     * 新建笔记。用户不存在则不能创建。
     */
    Note createNote(String username, NoteDTO dto);

    /**
     * 修改笔记（仅标题、内容、标签），提交后更新修改时间。
     */
    Note updateNote(String username, NoteDTO dto);

    /**
     * 切换笔记置顶状态。
     */
    void togglePin(String username, Long noteId);

    /**
     * 删除笔记（逻辑删除）。
     */
    void deleteNote(String username, Long noteId);

    /** 查询回收站中的笔记 */
    List<Note> listDeletedNotes(String username);

    /** 从回收站恢复笔记 */
    void restoreNote(String username, Long noteId);

    /** 彻底删除笔记（不可恢复） */
    void permanentlyDeleteNote(String username, Long noteId);

    /** 导出用：查询当前用户所有未删除笔记（置顶优先） */
    List<Note> listForExport(String username);

    /** 月报用：查询当前用户近 30 天创建的笔记（按创建时间倒序） */
    List<Note> listLastMonth(String username);

    /** 周报用：查询当前用户近 7 天创建的笔记（按创建时间倒序） */
    List<Note> listLastWeek(String username);
}
