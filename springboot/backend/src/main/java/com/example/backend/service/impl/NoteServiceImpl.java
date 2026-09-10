package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.dto.NoteDTO;
import com.example.backend.entity.Note;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.NoteMapper;
import com.example.backend.service.NoteService;
import com.example.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NoteServiceImpl implements NoteService {

    private final NoteMapper noteMapper;
    private final UserService userService;

    public NoteServiceImpl(NoteMapper noteMapper, UserService userService) {
        this.noteMapper = noteMapper;
        this.userService = userService;
    }

    @Override
    public Map<String, List<Note>> listMyNotes(String username) {
        Long userId = requireUserId(username);

        // 置顶列表
        List<Note> pinned = noteMapper.selectList(baseWrapper(userId)
                .eq(Note::getIsPinned, 1)
                .orderByDesc(Note::getUpdateTime)
                .orderByDesc(Note::getId));

        // 未置顶列表
        List<Note> normal = noteMapper.selectList(baseWrapper(userId)
                .eq(Note::getIsPinned, 0)
                .orderByDesc(Note::getUpdateTime)
                .orderByDesc(Note::getId));

        Map<String, List<Note>> result = new HashMap<>(4);
        result.put("pinned", pinned);
        result.put("normal", normal);
        return result;
    }

    @Override
    public Note getNote(String username, Long noteId) {
        // requireOwnedNote 内部已校验笔记存在且属于当前登录用户
        return requireOwnedNote(username, noteId);
    }

    @Override
    public Note createNote(String username, NoteDTO dto) {
        // 外键约束：用户不存在则不能创建笔记
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在，无法创建笔记");
        }
        if (dto == null || dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("笔记标题不能为空");
        }

        // 记录用户提交的日期时间，精确到分钟；创建时间与修改时间一致
        LocalDateTime now = nowToMinute();

        Note note = new Note();
        note.setUserId(user.getId());
        note.setTitle(dto.getTitle().trim());
        note.setContent(dto.getContent());
        note.setTag(dto.getTag() == null ? "" : dto.getTag().trim());
        note.setIsPinned(0);
        note.setIsDeleted(0);
        note.setCreateTime(now);
        note.setUpdateTime(now);
        noteMapper.insert(note);
        return note;
    }

    @Override
    public Note updateNote(String username, NoteDTO dto) {
        if (dto == null || dto.getId() == null) {
            throw new BusinessException("缺少笔记 id");
        }
        Note note = requireOwnedNote(username, dto.getId());
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("笔记标题不能为空");
        }

        // 修改后更新数据表字段“修改时间”，精确到分钟
        note.setTitle(dto.getTitle().trim());
        note.setContent(dto.getContent());
        note.setTag(dto.getTag() == null ? "" : dto.getTag().trim());
        note.setUpdateTime(nowToMinute());
        noteMapper.updateById(note);
        return note;
    }

    @Override
    public void togglePin(String username, Long noteId) {
        Note note = requireOwnedNote(username, noteId);
        // 置顶状态取反：1 -> 0，0 -> 1
        note.setIsPinned(note.getIsPinned() != null && note.getIsPinned() == 1 ? 0 : 1);
        note.setUpdateTime(nowToMinute());
        noteMapper.updateById(note);
    }

    @Override
    public void deleteNote(String username, Long noteId) {
        Note note = requireOwnedNote(username, noteId);
        // 逻辑删除：UPDATE note SET is_deleted = 1 WHERE id = ? AND user_id = ?
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, note.getUserId())
                .set(Note::getIsDeleted, 1);
        noteMapper.update(null, wrapper);
    }

    @Override
    public List<Note> listDeletedNotes(String username) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 1)
                .orderByDesc(Note::getUpdateTime)
                .orderByDesc(Note::getId);
        return noteMapper.selectList(wrapper);
    }

    @Override
    public void restoreNote(String username, Long noteId) {
        Long userId = requireUserId(username);
        // 查回收站中的该笔记，校验归属
        LambdaQueryWrapper<Note> query = new LambdaQueryWrapper<>();
        query.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 1);
        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BusinessException("回收站中没有该笔记");
        }
        // 恢复：UPDATE note SET is_deleted = 0 WHERE id = ?
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .set(Note::getIsDeleted, 0);
        noteMapper.update(null, wrapper);
    }

    @Override
    public void permanentlyDeleteNote(String username, Long noteId) {
        Long userId = requireUserId(username);
        // 校验该笔记在回收站中且属于当前用户
        LambdaQueryWrapper<Note> query = new LambdaQueryWrapper<>();
        query.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 1);
        Note note = noteMapper.selectOne(query);
        if (note == null) {
            throw new BusinessException("回收站中没有该笔记");
        }
        // 彻底删除：真实 DELETE FROM note WHERE id = ?
        noteMapper.deleteById(noteId);
    }

    // ==================== 私有辅助方法 ====================

    /** 基础查询条件：当前用户 + 未删除 */
    private LambdaQueryWrapper<Note> baseWrapper(Long userId) {
        return new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getIsDeleted, 0);
    }

    /** 根据用户名解析用户 id，用户不存在则抛出业务异常 */
    private Long requireUserId(String username) {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        return user.getId();
    }

    /** 校验笔记存在、未删除且属于当前登录用户，返回该笔记 */
    private Note requireOwnedNote(String username, Long noteId) {
        Long userId = requireUserId(username);
        Note note = noteMapper.selectById(noteId);
        if (note == null || (note.getIsDeleted() != null && note.getIsDeleted() == 1)) {
            throw new BusinessException("笔记不存在或已删除");
        }
        if (!note.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作他人的笔记");
        }
        return note;
    }

    /** 当前时间，精确到分钟（秒、毫秒置零） */
    private LocalDateTime nowToMinute() {
        return LocalDateTime.now().withSecond(0).withNano(0);
    }
}
