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
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 查看待办详情 */
    @GetMapping("/{id}")
    public Result<TodoDTO> detail(@RequestAttribute("username") String username,
                                  @PathVariable("id") Long id) {
        return Result.success(todoService.getTodo(username, id));
    }

    /**
     * 设置完成状态。
     * 传 completed=true 标记为“已完成”；completed=false 取消完成（取消后若已过截止时间会变为“逾期”）。
     */
    @PutMapping("/{id}/complete")
    public Result<TodoDTO> updateCompleted(@RequestAttribute("username") String username,
                                           @PathVariable("id") Long id,
                                           @RequestParam(value = "completed", defaultValue = "true") Boolean completed) {
        return Result.success(todoService.updateCompleted(username, id, Boolean.TRUE.equals(completed)));
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

    /** 删除待办（逻辑删除，进入回收站） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestAttribute("username") String username,
                               @PathVariable("id") Long id) {
        todoService.deleteTodo(username, id);
        return Result.success("已移入回收站", null);
    }

    /** 查看回收站待办列表 */
    @GetMapping("/trash")
    public Result<List<TodoDTO>> trash(@RequestAttribute("username") String username) {
        return Result.success(todoService.listDeletedTodos(username));
    }

    /** 从回收站恢复待办 */
    @PutMapping("/restore/{id}")
    public Result<Void> restore(@RequestAttribute("username") String username,
                                @PathVariable("id") Long id) {
        todoService.restoreTodo(username, id);
        return Result.success("恢复成功", null);
    }

    /** 彻底删除待办（不可恢复） */
    @DeleteMapping("/permanent/{id}")
    public Result<Void> permanentDelete(@RequestAttribute("username") String username,
                                        @PathVariable("id") Long id) {
        todoService.permanentlyDeleteTodo(username, id);
        return Result.success("已彻底删除", null);
    }
}
