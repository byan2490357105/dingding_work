package com.example.backend.thirdparty.dingtalk;

import com.example.backend.exception.BusinessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 钉钉表格（文档）OpenAPI 客户端。
 * 接口路径与参数以钉钉开放平台“文档”能力为准，未具备权限时钉钉会返回错误码。
 */
@Component
public class DingTalkSheetClient {

    private final DingTalkProperties properties;
    private final RestTemplate restTemplate;

    public DingTalkSheetClient(DingTalkProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    /** 获取表格文件内的所有工作表 */
    public Map<String, Object> listSheets(String accessToken, String workbookId) {
        String url = docUrl(properties.getListSheetsPath().replace("{workbookId}", encode(workbookId)));
        return exchange(url, HttpMethod.GET, accessToken, null);
    }

    /** 读取单元格区域，range 形如 Sheet1!A1:C10 */
    public Map<String, Object> getRange(String accessToken, String workbookId, String range) {
        String url = docUrl(rangeUrl(workbookId, range));
        return exchange(url, HttpMethod.GET, accessToken, null);
    }

    /** 更新单元格区域 */
    public Map<String, Object> updateRange(String accessToken, String workbookId,
                                           String range, List<List<Object>> values) {
        Map<String, Object> body = new HashMap<>();
        body.put("values", values);
        String url = docUrl(rangeUrl(workbookId, range));
        return exchange(url, HttpMethod.PUT, accessToken, body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> exchange(String url, HttpMethod method, String accessToken, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-acs-dingtalk-access-token", accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, method, entity, Map.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new BusinessException(502, "调用钉钉表格接口失败：" + e.getMessage());
        }
    }

    private String docUrl(String path) {
        return properties.getDocBaseUrl() + path;
    }

    private String rangeUrl(String workbookId, String range) {
        return properties.getRangePath()
                .replace("{workbookId}", encode(workbookId))
                .replace("{range}", encode(range));
    }

    private String encode(String value) {
        return value == null ? "" : UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }
}
