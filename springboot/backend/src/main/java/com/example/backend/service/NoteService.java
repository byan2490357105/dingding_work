package com.example.backend.service;

import com.example.backend.dto.NoteDTO;
import com.example.backend.entity.Note;

import java.util.List;
import java.util.Map;

/**
 * 笔记服务。
 */
public interface NoteService {

    /**
     * 查询当前登录用户的所有笔记，按置顶 / 未置顶分为两个列表。
     *
     * @param username 当前登录用户名（由 JWT 解析得到）
     * @return {"pinned": 置顶列表, "normal": 未置顶列表}
     */
    Map<String, List<Note>> listMyNotes(String username);

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
}
