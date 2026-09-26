package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.TodoDTO;
import com.example.backend.entity.Todo;
import com.example.backend.entity.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.TodoMapper;
import com.example.backend.service.TodoService;
import com.example.backend.service.UserService;
import com.example.backend.util.ExportUtil;
import com.example.backend.util.WordFreqUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Page<TodoDTO> pageTodos(String username, long pageNum, long pageSize,
                                   String status, String keyword, List<String> excludeLabels) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false);
        // 完成状态过滤
        if ("pending".equalsIgnoreCase(status)) {
            wrapper.eq(Todo::getCompleted, false);
        } else if ("done".equalsIgnoreCase(status)) {
            wrapper.eq(Todo::getCompleted, true);
        }
        // 关键字：标题或正文模糊匹配
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Todo::getTitle, kw).or().like(Todo::getContent, kw));
        }
        // 隐藏标签：多选 notIn
        if (excludeLabels != null && !excludeLabels.isEmpty()) {
            List<String> valid = excludeLabels.stream()
                    .filter(t -> t != null && !t.trim().isEmpty())
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());
            if (!valid.isEmpty()) {
                wrapper.notIn(Todo::getLabel, valid);
            }
        }
        // 置顶优先，其次按修改时间倒序
        wrapper.orderByDesc(Todo::getTop)
                .orderByDesc(Todo::getUpdatedAt)
                .orderByDesc(Todo::getId);
        Page<Todo> page = todoMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        // convert 返回 IPage，实际仍是同一个 Page 实例，强转回 Page
        @SuppressWarnings("unchecked")
        Page<TodoDTO> dtoPage = (Page<TodoDTO>) page.convert(this::toDTO);
        return dtoPage;
    }

    @Override
    public List<Map<String, Object>> listLabelStats(String username) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .select(Todo::getLabel);
        Map<String, Long> counts = todoMapper.selectList(wrapper).stream()
                .filter(t -> t != null && t.getLabel() != null)
                .map(t -> t.getLabel().trim())
                .filter(t -> !t.isEmpty())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>(2);
                    item.put("label", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> wordCloud(String username, int limit) {
        Long userId = requireUserId(username);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .select(Todo::getTitle, Todo::getContent, Todo::getLabel);
        List<Todo> todos = todoMapper.selectList(wrapper);
        List<String> texts = new ArrayList<>();
        for (Todo todo : todos) {
            texts.add(ExportUtil.htmlToText(todo.getContent()));
            if (todo.getTitle() != null && !todo.getTitle().isEmpty()) {
                texts.addAll(Collections.nCopies(3, todo.getTitle()));
            }
            if (todo.getLabel() != null && !todo.getLabel().trim().isEmpty()) {
                texts.addAll(Collections.nCopies(5, todo.getLabel().trim()));
            }
        }
        return WordFreqUtil.topWords(texts, limit <= 0 ? 50 : limit);
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
        // 新建任务默认未发送提醒
        todo.setReminded(false);
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

        // 记录修改前的提醒时间，用于判断是否需要重置提醒状态
        LocalDateTime oldRemindTime = todo.getRemindTime();
        applyDTO(todo, todoDTO);
        if (oldRemindTime == null ? todoDTO.getRemindTime() != null : !oldRemindTime.equals(todoDTO.getRemindTime())) {
            todo.setReminded(false);
        }
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

    @Override
    public List<Todo> listLastMonth(String username) {
        Long userId = requireUserId(username);
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .ge(Todo::getCreatedAt, since)
                .orderByDesc(Todo::getCreatedAt);
        return todoMapper.selectList(wrapper);
    }

    @Override
    public List<Todo> listLastWeek(String username) {
        Long userId = requireUserId(username);
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getUserId, userId)
                .eq(Todo::getDeleted, false)
                .ge(Todo::getCreatedAt, since)
                .orderByDesc(Todo::getCreatedAt);
        return todoMapper.selectList(wrapper);
    }

    @Override
    public List<Todo> listTodosToRemind() {
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getDeleted, false)
                .eq(Todo::getCompleted, false)
                .eq(Todo::getReminded, false)
                .isNotNull(Todo::getRemindTime)
                .le(Todo::getRemindTime, now)
                .orderByAsc(Todo::getRemindTime);
        return todoMapper.selectList(wrapper);
    }

    @Override
    public void markReminded(Long id) {
        if (id == null) {
            return;
        }
        LambdaUpdateWrapper<Todo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Todo::getId, id)
                .set(Todo::getReminded, true);
        todoMapper.update(null, wrapper);
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
        todo.setRemindTime(dto.getRemindTime());
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
        dto.setRemindTime(todo.getRemindTime());
        dto.setReminded(Boolean.TRUE.equals(todo.getReminded()));
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
