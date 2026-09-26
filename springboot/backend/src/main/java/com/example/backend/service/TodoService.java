package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.TodoDTO;
import com.example.backend.entity.Todo;

import java.util.List;
import java.util.Map;

public interface TodoService {

    /** 查询当前用户所有未删除待办，置顶优先 */
    List<TodoDTO> listByUsername(String username);

    /**
     * 分页查询当前用户待办。
     *
     * @param status         pending 未完成 / done 已完成 / 不传或 all 全部
     * @param keyword        标题/正文关键字
     * @param excludeLabels  需隐藏的标签（多选）
     */
    Page<TodoDTO> pageTodos(String username, long pageNum, long pageSize,
                           String status, String keyword, List<String> excludeLabels);

    /** 统计当前用户各标签下的待办数量（全量，供标签筛选面板） */
    List<Map<String, Object>> listLabelStats(String username);

    /** 基于当前用户全部待办标题与正文生成词云（2-gram 词频，标题/标签加权） */
    List<Map<String, Object>> wordCloud(String username, int limit);

    /** 查看待办详情（仅能查看属于当前用户的待办） */
    TodoDTO getTodo(String username, Long id);

    /**
     * 设置待办完成状态（已完成 / 取消完成）。
     *
     * @param completed true 标记为已完成，false 取消完成
     */
    TodoDTO updateCompleted(String username, Long id, boolean completed);

    /** 新建待办 */
    TodoDTO createTodo(String username, TodoDTO todoDTO);

    /** 修改待办，并刷新修改时间 */
    TodoDTO updateTodo(String username, Long id, TodoDTO todoDTO);

    /** 逻辑删除待办 */
    void deleteTodo(String username, Long id);

    /** 查询回收站中的待办 */
    List<TodoDTO> listDeletedTodos(String username);

    /** 从回收站恢复待办 */
    void restoreTodo(String username, Long id);

    /** 彻底删除待办（不可恢复） */
    void permanentlyDeleteTodo(String username, Long id);

    /** 导出用：查询当前用户所有未删除待办（置顶优先），返回实体 */
    List<Todo> listForExport(String username);

    /** 月报用：查询当前用户近 30 天创建的待办（含已完成与未完成，按创建时间倒序） */
    List<Todo> listLastMonth(String username);

    /** 周报用：查询当前用户近 7 天创建的待办（含已完成与未完成，按创建时间倒序） */
    List<Todo> listLastWeek(String username);

    /** 查询所有到达提醒时间且未发送提醒的未完成待办（定时任务用，跨用户） */
    List<Todo> listTodosToRemind();

    /** 标记待办为已发送提醒 */
    void markReminded(Long id);
}
