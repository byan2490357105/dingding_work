package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.dto.DingTalkRangeUpdateDTO;
import com.example.backend.service.DingTalkSheetService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 钉钉表格操作接口，使用当前登录用户绑定的钉钉身份调用。
 */
@RestController
@RequestMapping("/api/third-party/dingtalk/sheets")
public class DingTalkSheetController {

    private final DingTalkSheetService dingTalkSheetService;

    public DingTalkSheetController(DingTalkSheetService dingTalkSheetService) {
        this.dingTalkSheetService = dingTalkSheetService;
    }

    /** 查看表格文件下的工作表 */
    @GetMapping
    public Result<Map<String, Object>> listSheets(@RequestAttribute("username") String username,
                                                  @RequestParam("workbookId") String workbookId) {
        return Result.success(dingTalkSheetService.listSheets(username, workbookId));
    }

    /** 读取单元格区域，range 形如 Sheet1!A1:C10 */
    @GetMapping("/range")
    public Result<Map<String, Object>> getRange(@RequestAttribute("username") String username,
                                                @RequestParam("workbookId") String workbookId,
                                                @RequestParam("range") String range) {
        return Result.success(dingTalkSheetService.getRange(username, workbookId, range));
    }

    /** 写入单元格区域 */
    @PutMapping("/range")
    public Result<Map<String, Object>> updateRange(@RequestAttribute("username") String username,
                                                   @RequestBody(required = false) DingTalkRangeUpdateDTO updateDTO) {
        if (updateDTO == null) {
            return Result.error(400, "请求参数不能为空");
        }
        return Result.success("写入成功", dingTalkSheetService.updateRange(
                username, updateDTO.getWorkbookId(), updateDTO.getRange(), updateDTO.getValues()));
    }
}
