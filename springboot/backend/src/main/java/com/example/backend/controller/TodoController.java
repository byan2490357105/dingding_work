package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.TodoDTO;
import com.example.backend.service.TodoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 待办任务接口，由 JwtInterceptor 统一校验登录用户。
 */
@RestController
@RequestMapping("/api/todo")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    /** 查看当前用户所有待办 */
    @GetMapping("/list")
    public Result<List<TodoDTO>> list(@RequestAttribute("username") String username) {
        return Result.success(todoService.listByUsername(username));
    }

    /** 新建待办 */
    @PostMapping
    public Result<TodoDTO> create(@RequestAttribute("username") String username,
                                  @RequestBody(required = false) TodoDTO todoDTO) {
        return Result.success("创建成功", todoService.createTodo(username, todoDTO));
    }

    /** 修改待办 */
    @PutMapping("/{id}")
    public Result<TodoDTO> update(@RequestAttribute("username") String username,
                                  @PathVariable("id") Long id,
                                  @RequestBody(required = false) TodoDTO todoDTO) {
        return Result.success("修改成功", todoService.updateTodo(username, id, todoDTO));
    }

    /** 删除待办 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("username") String username,
                               @PathVariable("id") Long id) {
        todoService.deleteTodo(username, id);
        return Result.success("删除成功", null);
    }
}
