package com.example.backend.service;

import com.example.backend.dto.TodoDTO;

import java.util.List;

public interface TodoService {

    /** 查询当前用户所有未删除待办，置顶优先 */
    List<TodoDTO> listByUsername(String username);

    /** 新建待办 */
    TodoDTO createTodo(String username, TodoDTO todoDTO);

    /** 修改待办，并刷新修改时间 */
    TodoDTO updateTodo(String username, Long id, TodoDTO todoDTO);

    /** 逻辑删除待办 */
    void deleteTodo(String username, Long id);
}
