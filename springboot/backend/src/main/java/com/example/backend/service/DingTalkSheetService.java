package com.example.backend.service;

import java.util.List;
import java.util.Map;

/**
 * 钉钉表格操作服务：使用当前登录用户绑定的钉钉访问令牌。
 */
public interface DingTalkSheetService {

    Map<String, Object> listSheets(String username, String workbookId);

    Map<String, Object> getRange(String username, String workbookId, String range);

    Map<String, Object> updateRange(String username, String workbookId, String range, List<List<Object>> values);
}
