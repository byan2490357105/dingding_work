package com.example.backend.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.dto.ReportVO;
import com.example.backend.entity.AiReport;
import com.example.backend.entity.Note;
import com.example.backend.entity.Todo;
import com.example.backend.entity.User;
import com.example.backend.mapper.AiReportMapper;
import com.example.backend.mapper.NoteMapper;
import com.example.backend.mapper.TodoMapper;
import com.example.backend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * AI 报告缓存服务：基于数据指纹的缓存命中判断（MySQL 实现，无需 Redis）。
 *
 * 数据未变化时复用 ai_report 表中的最新历史报告（cached=true），不消耗 DeepSeek Token；
 * 数据变化或无历史记录时调 generator 重新生成（cached=false），
 * 同时写入 MySQL ai_report 历史记录表。
 */
@Service
public class AiReportCacheService {

    @Resource
    private AiReportMapper aiReportMapper;

    @Resource
    private UserService userService;

    @Resource
    private NoteMapper noteMapper;

    @Resource
    private TodoMapper todoMapper;

    /**
     * 报告缓存：命中且指纹相同则直接返回（cached=true），未命中则调 generator 生成并写历史表（cached=false）。
     *
     * @param username   当前登录用户名
     * @param reportType note-weekly | todo-weekly | note-monthly | todo-monthly
     * @param generator  缓存未命中时调用的报告生成器（不应自行设置 cached 标志位）
     */
    public ReportVO getOrGenerate(String username, String reportType, Supplier<ReportVO> generator) {
        String fingerprint = computeFingerprint(username, reportType);

        // 查该用户 + 报告类型的最新一条历史记录
        User user = userService.getByUsername(username);
        Long userId = user == null ? null : user.getId();

        if (userId != null) {
            QueryWrapper<AiReport> qw = new QueryWrapper<>();
            qw.eq("user_id", userId)
                    .eq("report_type", reportType)
                    .orderByDesc("generated_at")
                    .last("LIMIT 1");
            AiReport latest = aiReportMapper.selectOne(qw);
            if (latest != null && fingerprint.equals(latest.getDataFingerprint())) {
                // 指纹相同 → 数据未更新，直接复用历史报告（省 Token）
                ReportVO vo = new ReportVO();
                vo.setPeriod(latest.getPeriod());
                vo.setCount(0); // 历史记录不存 count，返回 0 不影响展示
                vo.setContent(latest.getContent());
                vo.setCached(true);
                return vo;
            }
        }

        // 缓存未命中或指纹不匹配 → 调 generator 生成新报告
        ReportVO fresh = generator.get();
        fresh.setCached(false);

        // 写入 MySQL 历史记录（失败不影响主流程）
        try {
            AiReport record = new AiReport();
            record.setUserId(userId);
            record.setReportType(reportType);
            record.setPeriod(fresh.getPeriod());
            record.setContent(fresh.getContent());
            record.setDataFingerprint(fingerprint);
            record.setGeneratedAt(LocalDateTime.now());
            aiReportMapper.insert(record);
        } catch (Exception e) {
            System.err.println("[AiReportCacheService] 写入 AI 报告历史失败: " + e.getMessage());
        }

        return fresh;
    }

    /**
     * 计算数据指纹：格式为 "count|maxTime"。
     * note 类查询近 N 天 max(update_time) + count(*)；
     * todo 类查询近 N 天 max(updated_at) + count(*)。
     * 数据未变化时指纹保持稳定，从而命中缓存。
     */
    private String computeFingerprint(String username, String reportType) {
        User user = userService.getByUsername(username);
        if (user == null || user.getId() == null) {
            return "0|null";
        }
        Long userId = user.getId();
        boolean weekly = reportType.contains("weekly");
        int days = weekly ? 7 : 30;
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        if (reportType.startsWith("note")) {
            QueryWrapper<Note> qw = new QueryWrapper<>();
            qw.select("max(update_time) as maxTime, count(*) as cnt")
                    .eq("user_id", userId)
                    .eq("is_deleted", 0)
                    .ge("create_time", since);
            List<Map<String, Object>> maps = noteMapper.selectMaps(qw);
            return mapToFingerprint(maps);
        } else if (reportType.startsWith("todo")) {
            QueryWrapper<Todo> qw = new QueryWrapper<>();
            qw.select("max(updated_at) as maxTime, count(*) as cnt")
                    .eq("user_id", userId)
                    .eq("is_deleted", 0)
                    .ge("created_at", since);
            List<Map<String, Object>> maps = todoMapper.selectMaps(qw);
            return mapToFingerprint(maps);
        }
        return "unknown";
    }

    /** 将 selectMaps 返回的单行结果转为 "count|maxTime" 形式的指纹 */
    private String mapToFingerprint(List<Map<String, Object>> maps) {
        if (maps == null || maps.isEmpty() || maps.get(0) == null) {
            return "0|null";
        }
        Map<String, Object> row = maps.get(0);
        Object cnt = row.get("cnt");
        Object maxTime = row.get("maxTime");
        String cntStr = cnt == null ? "0" : cnt.toString();
        String maxTimeStr = maxTime == null ? "null" : maxTime.toString();
        return cntStr + "|" + maxTimeStr;
    }
}
