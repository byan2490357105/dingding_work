package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.NoteDTO;
import com.example.backend.entity.Note;
import com.example.backend.service.NoteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 笔记（随手记）接口。
 * 路径位于 /api/** 下，由 JwtInterceptor 校验 Token，
 * 并通过 request 中的 username 识别提交用户。
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /** 查看当前登录用户的所有笔记，分为置顶 / 未置顶两个列表 */
    @GetMapping
    public Result<Map<String, List<Note>>> list(HttpServletRequest request) {
        return Result.success(noteService.listMyNotes(currentUsername(request)));
    }

    /** 查看笔记详情 */
    @GetMapping("/{id}")
    public Result<Note> detail(@PathVariable Long id, HttpServletRequest request) {
        return Result.success(noteService.getNote(currentUsername(request), id));
    }

    /** 新建笔记 */
    @PostMapping
    public Result<Note> create(@RequestBody NoteDTO dto, HttpServletRequest request) {
        return Result.success("新建笔记成功", noteService.createNote(currentUsername(request), dto));
    }

    /** 修改笔记（提交后后端更新修改时间） */
    @PutMapping
    public Result<Note> update(@RequestBody NoteDTO dto, HttpServletRequest request) {
        return Result.success("修改笔记成功", noteService.updateNote(currentUsername(request), dto));
    }

    /** 切换置顶状态 */
    @PutMapping("/pin/{id}")
    public Result<Void> togglePin(@PathVariable Long id, HttpServletRequest request) {
        noteService.togglePin(currentUsername(request), id);
        return Result.success("操作成功", null);
    }

    /** 删除笔记（逻辑删除，进入回收站） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        noteService.deleteNote(currentUsername(request), id);
        return Result.success("已移入回收站", null);
    }

    /** 查看回收站笔记列表 */
    @GetMapping("/trash")
    public Result<List<Note>> trash(HttpServletRequest request) {
        return Result.success(noteService.listDeletedNotes(currentUsername(request)));
    }

    /** 从回收站恢复笔记 */
    @PutMapping("/restore/{id}")
    public Result<Void> restore(@PathVariable Long id, HttpServletRequest request) {
        noteService.restoreNote(currentUsername(request), id);
        return Result.success("恢复成功", null);
    }

    /** 彻底删除笔记（不可恢复） */
    @DeleteMapping("/permanent/{id}")
    public Result<Void> permanentDelete(@PathVariable Long id, HttpServletRequest request) {
        noteService.permanentlyDeleteNote(currentUsername(request), id);
        return Result.success("已彻底删除", null);
    }

    /** 从请求中取出 JWT 拦截器解析出的用户名 */
    private String currentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }
}
