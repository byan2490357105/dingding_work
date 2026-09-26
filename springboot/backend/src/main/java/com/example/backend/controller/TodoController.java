package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.TodoDTO;
import com.example.backend.entity.Todo;
import com.example.backend.service.TodoService;
import com.example.backend.util.ExportUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
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

    /**
     * 分页查询待办（日历/时间轴视图仍使用 /list 全量接口）。
     *
     * @param status        pending 未完成 / done 已完成 / all 全部
     * @param keyword       标题/正文关键字
     * @param excludeLabel  需隐藏的标签（可多选）
     */
    @GetMapping("/page")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<TodoDTO>> page(
            @RequestAttribute("username") String username,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "8") long pageSize,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(value = "excludeLabel", required = false) List<String> excludeLabel) {
        return Result.success(todoService.pageTodos(
                username, pageNum, pageSize, status, keyword, excludeLabel));
    }

    /** 统计当前用户各标签下的待办数量（供标签筛选面板） */
    @GetMapping("/labels")
    public Result<List<java.util.Map<String, Object>>> labelStats(
            @RequestAttribute("username") String username) {
        return Result.success(todoService.listLabelStats(username));
    }

    /** 生成当前用户全部待办的词云数据 */
    @GetMapping("/wordcloud")
    public Result<List<java.util.Map<String, Object>>> wordCloud(
            @RequestAttribute("username") String username,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(todoService.wordCloud(username, limit));
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

    /** 导出当前用户所有待办为 CSV 文件（表头：待办标题/待办内容/待办开始时间/待办结束时间/标签/创建时间） */
    @GetMapping("/export/csv")
    public void exportCsv(@RequestAttribute("username") String username,
                          HttpServletResponse response) throws Exception {
        List<Todo> todos = todoService.listForExport(username);
        List<String[]> rows = new ArrayList<>();
        for (Todo todo : todos) {
            rows.add(new String[]{
                    todo.getTitle() == null ? "" : todo.getTitle(),
                    ExportUtil.htmlToText(todo.getContent()),
                    ExportUtil.formatMinute(todo.getStartTime()),
                    ExportUtil.formatMinute(todo.getEndTime()),
                    todo.getLabel() == null ? "" : todo.getLabel(),
                    ExportUtil.formatMinute(todo.getCreatedAt())
            });
        }
        byte[] data = ExportUtil.toCsv(
                new String[]{"待办标题", "待办内容", "待办开始时间", "待办结束时间", "标签", "创建时间"}, rows);
        ExportUtil.writeToResponse(response, data, "text/csv;charset=UTF-8",
                "待办导出_" + ExportUtil.timestamp() + ".csv");
    }
}
