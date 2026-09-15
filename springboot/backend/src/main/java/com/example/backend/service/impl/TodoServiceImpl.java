package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.dto.TodoDTO;
import com.example.backend.entity.Todo;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TodoMapper;
import com.example.backend.service.TodoService;
import com.example.backend.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;
    private final UserService userService;

    public TodoServiceImpl(TodoMapper todoMapper, UserService userService) {
        this.todoMapper = todoMapper;
        this.userService = userService;
    }

    @Override
    public List<TodoDTO> listByUsername(String username) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Todo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .orderByDesc(Todo::getTop)
                .orderByDesc(Todo::getUpdatedAt);
        return todoMapper.selectList(queryWrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public TodoDTO createTodo(String username, TodoDTO todoDTO) {
        Long userId = requireUserId(username);
        validateAndNormalize(todoDTO);

        LocalDateTime now = currentMinuteTime();
        Todo todo = new Todo();
        applyDTO(todo, todoDTO);
        todo.setUserId(userId);
        todo.setDeleted(false);
        // 新建任务默认未完成
        todo.setCompleted(false);
        todo.setCreatedAt(now);
        // 未修改时修改时间等于创建时间
        todo.setUpdatedAt(now);

        todoMapper.insert(todo);
        return toDTO(todo);
    }

    @Override
    public TodoDTO getTodo(String username, Long id) {
        Long userId = requireUserId(username);
        Todo todo = todoMapper.selectById(id);
        checkOwnership(todo, userId);
        return toDTO(todo);
    }

    @Override
    public TodoDTO updateCompleted(String username, Long id, boolean completed) {
        Long userId = requireUserId(username);
        Todo todo = todoMapper.selectById(id);
        checkOwnership(todo, userId);

        todo.setCompleted(completed);
        todo.setUpdatedAt(currentMinuteTime());
        todoMapper.updateById(todo);
        return toDTO(todo);
    }

    @Override
    public TodoDTO updateTodo(String username, Long id, TodoDTO todoDTO) {
        Long userId = requireUserId(username);
        validateAndNormalize(todoDTO);

        Todo todo = todoMapper.selectById(id);
        checkOwnership(todo, userId);

        applyDTO(todo, todoDTO);
        // 修改成功后刷新修改时间
        todo.setUpdatedAt(currentMinuteTime());

        todoMapper.updateById(todo);
        return toDTO(todo);
    }

    @Override
    public void deleteTodo(String username, Long id) {
        Long userId = requireUserId(username);
        Todo todo = todoMapper.selectById(id);
        checkOwnership(todo, userId);

        // 逻辑删除：UPDATE todo SET is_deleted = 1 WHERE id = ? AND user_id = ?
        LambdaUpdateWrapper<Todo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Todo::getId, id)
                .eq(Todo::getUserId, userId)
                .set(Todo::getDeleted, true);
        todoMapper.update(null, wrapper);
    }

    @Override
    public List<TodoDTO> listDeletedTodos(String username) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, true)
                .orderByDesc(Todo::getUpdatedAt);
        return todoMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void restoreTodo(String username, Long id) {
        Long userId = requireUserId(username);
        // 校验该待办在回收站中且属于当前用户
        LambdaQueryWrapper<Todo> query = new LambdaQueryWrapper<>();
        query.eq(Todo::getId, id)
                .eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, true);
        Todo todo = todoMapper.selectOne(query);
        if (todo == null) {
            throw new BusinessException("回收站中没有该待办");
        }
        // 恢复：UPDATE todo SET is_deleted = 0 WHERE id = ?
        LambdaUpdateWrapper<Todo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Todo::getId, id)
                .set(Todo::getDeleted, false);
        todoMapper.update(null, wrapper);
    }

    @Override
    public void permanentlyDeleteTodo(String username, Long id) {
        Long userId = requireUserId(username);
        // 校验该待办在回收站中且属于当前用户
        LambdaQueryWrapper<Todo> query = new LambdaQueryWrapper<>();
        query.eq(Todo::getId, id)
                .eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, true);
        Todo todo = todoMapper.selectOne(query);
        if (todo == null) {
            throw new BusinessException("回收站中没有该待办");
        }
        // 彻底删除：真实 DELETE FROM todo WHERE id = ?
        todoMapper.deleteById(id);
    }

    @Override
    public List<Todo> listForExport(String username) {
        Long userId = requireUserId(username);
        // 置顶优先，其余按创建时间倒序
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .orderByDesc(Todo::getTop)
                .orderByDesc(Todo::getCreatedAt);
        return todoMapper.selectList(wrapper);
    }

    /** 通过登录用户名找到数据库中的 user.id；用户不存在则不能创建待办 */
    private Long requireUserId(String username) {
        User user = userService.getByUsername(username);
        if (user == null || user.getId() == null) {
            throw new BusinessException(401, "登录用户不存在");
        }
        return user.getId();
    }

    /** 校验待办必须属于当前用户且未被删除 */
    private void checkOwnership(Todo todo, Long userId) {
        if (todo == null || !userId.equals(todo.getUserId())) {
            throw new BusinessException(404, "待办不存在或无权操作");
        }
        if (Boolean.TRUE.equals(todo.getDeleted())) {
            throw new BusinessException(404, "待办不存在或已删除");
        }
    }

    /** 校验并规范基础字段 */
    private void validateAndNormalize(TodoDTO dto) {
        if (dto == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new BusinessException("待办标题不能为空");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException("请填写开始时间和结束时间");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        if (dto.getTitle().trim().length() > 100) {
            throw new BusinessException("待办标题不能超过 100 个字符");
        }
        if (dto.getLabel() != null && dto.getLabel().trim().length() > 50) {
            throw new BusinessException("标签不能超过 50 个字符");
        }

        dto.setTitle(dto.getTitle().trim());
        if (dto.getLabel() != null) {
            dto.setLabel(dto.getLabel().trim());
        }
        if (!Boolean.TRUE.equals(dto.getDaily())) {
            // 非每日任务不保留每日截止日期
            dto.setRepeatUntil(null);
        }
        if (Boolean.TRUE.equals(dto.getDaily()) && dto.getRepeatUntil() != null
                && dto.getRepeatUntil().isBefore(dto.getStartTime().toLocalDate())) {
            throw new BusinessException("每日重复截止日期不能早于开始日期");
        }
    }

    /** 将 DTO 可编辑字段写入实体 */
    private void applyDTO(Todo todo, TodoDTO dto) {
        todo.setTitle(dto.getTitle().trim());
        todo.setContent(dto.getContent());
        todo.setStartTime(dto.getStartTime());
        todo.setEndTime(dto.getEndTime());
        todo.setDaily(Boolean.TRUE.equals(dto.getDaily()));
        todo.setRepeatUntil(Boolean.TRUE.equals(dto.getDaily()) ? dto.getRepeatUntil() : null);
        todo.setTop(Boolean.TRUE.equals(dto.getTop()));
        todo.setLabel(dto.getLabel());
    }

    private TodoDTO toDTO(Todo todo) {
        TodoDTO dto = new TodoDTO();
        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setContent(todo.getContent());
        dto.setStartTime(todo.getStartTime());
        dto.setEndTime(todo.getEndTime());
        dto.setDaily(todo.getDaily());
        dto.setRepeatUntil(todo.getRepeatUntil());
        dto.setTop(todo.getTop());
        dto.setCompleted(Boolean.TRUE.equals(todo.getCompleted()));
        dto.setStatus(deriveStatus(todo));
        dto.setLabel(todo.getLabel());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setUpdatedAt(todo.getUpdatedAt());
        return dto;
    }

    /**
     * 派生任务状态：
     * 已完成：is_completed = 1（即使已过截止时间也显示已完成）；
     * 逾期：未完成且截止时间早于当前时间；
     * 进行中：其余情况。
     */
    private String deriveStatus(Todo todo) {
        if (Boolean.TRUE.equals(todo.getCompleted())) {
            return "已完成";
        }
        if (todo.getEndTime() != null && todo.getEndTime().isBefore(LocalDateTime.now())) {
            return "逾期";
        }
        return "进行中";
    }

    /** 创建/修改时间精确到分钟 */
    private LocalDateTime currentMinuteTime() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
    }
}
