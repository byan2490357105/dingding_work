package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.backend.dto.StatsVO;
import com.example.backend.entity.Note;
import com.example.backend.entity.Todo;
import com.example.backend.entity.User;
import com.example.backend.mapper.NoteMapper;
import com.example.backend.mapper.TodoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析仪表盘服务：聚合笔记 / 待办数据，
 * 输出统计卡片、待办完成趋势、笔记热力图、标签分布。
 */
@Service
public class StatsService {

    @Resource
    private NoteMapper noteMapper;

    @Resource
    private TodoMapper todoMapper;

    @Resource
    private UserService userService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 获取当前登录用户的仪表盘数据 */
    public StatsVO getDashboard(String username) {
        User user = userService.getByUsername(username);
        StatsVO vo = new StatsVO();
        if (user == null || user.getId() == null) {
            // 用户不存在时返回空结构，避免空指针
            vo.setTodoCompletionTrend(new ArrayList<>());
            vo.setNoteHeatmap(new ArrayList<>());
            Map<String, List<Map<String, Object>>> tagDist = new HashMap<>();
            tagDist.put("notes", new ArrayList<>());
            tagDist.put("todos", new ArrayList<>());
            vo.setTagDistribution(tagDist);
            vo.setTotalNotes(0);
            vo.setTotalTodos(0);
            vo.setCompletedTodos(0);
            vo.setCompletionRate(0.0);
            vo.setMonthNotes(0);
            return vo;
        }
        Long userId = user.getId();

        LocalDate today = LocalDate.now();
        LocalDateTime trendSince = today.minusDays(29).atStartOfDay();
        LocalDateTime yearSince = today.minusDays(364).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();

        // ----- 待办完成趋势：近 30 天每天的总数与已完成数 -----
        QueryWrapper<Todo> todoTrendQw = new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .ge("created_at", trendSince);
        List<Todo> trendTodos = todoMapper.selectList(todoTrendQw);
        Map<String, int[]> trendMap = new HashMap<>();
        for (int i = 0; i < 30; i++) {
            String dateKey = today.minusDays(29 - i).format(DATE_FMT);
            trendMap.put(dateKey, new int[]{0, 0});
        }
        for (Todo todo : trendTodos) {
            if (todo.getCreatedAt() == null) continue;
            String dateKey = todo.getCreatedAt().toLocalDate().format(DATE_FMT);
            int[] bucket = trendMap.get(dateKey);
            if (bucket == null) {
                bucket = new int[]{0, 0};
                trendMap.put(dateKey, bucket);
            }
            bucket[0] += 1;
            if (Boolean.TRUE.equals(todo.getCompleted())) {
                bucket[1] += 1;
            }
        }
        List<Map<String, Object>> todoCompletionTrend = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(trendMap.keySet());
        java.util.Collections.sort(sortedDates);
        for (String date : sortedDates) {
            int[] bucket = trendMap.get(date);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("total", bucket[0]);
            point.put("completed", bucket[1]);
            todoCompletionTrend.add(point);
        }
        vo.setTodoCompletionTrend(todoCompletionTrend);

        // ----- 笔记热力图：近一年每天的笔记数 -----
        QueryWrapper<Note> noteHeatQw = new QueryWrapper<Note>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .ge("create_time", yearSince);
        List<Note> heatNotes = noteMapper.selectList(noteHeatQw);
        Map<String, Integer> heatMap = new HashMap<>();
        for (Note note : heatNotes) {
            if (note.getCreateTime() == null) continue;
            String dateKey = note.getCreateTime().toLocalDate().format(DATE_FMT);
            heatMap.merge(dateKey, 1, Integer::sum);
        }
        List<Map<String, Object>> noteHeatmap = new ArrayList<>();
        for (int i = 0; i < 365; i++) {
            String dateKey = today.minusDays(364 - i).format(DATE_FMT);
            int count = heatMap.getOrDefault(dateKey, 0);
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", dateKey);
            point.put("count", count);
            noteHeatmap.add(point);
        }
        vo.setNoteHeatmap(noteHeatmap);

        // ----- 标签分布 -----
        Map<String, List<Map<String, Object>>> tagDistribution = new HashMap<>();
        tagDistribution.put("notes", buildTagDistFromNotes(userId));
        tagDistribution.put("todos", buildTagDistFromTodos(userId));
        vo.setTagDistribution(tagDistribution);

        // ----- 汇总统计 -----
        long totalNotes = countNotes(userId);
        long totalTodos = countTodos(userId);
        long completedTodos = countCompletedTodos(userId);
        long monthNotes = countMonthNotes(userId, monthStart);
        vo.setTotalNotes((int) totalNotes);
        vo.setTotalTodos((int) totalTodos);
        vo.setCompletedTodos((int) completedTodos);
        vo.setCompletionRate(totalTodos == 0 ? 0.0
                : Math.round((double) completedTodos / totalTodos * 1000) / 10.0);
        vo.setMonthNotes((int) monthNotes);

        return vo;
    }

    /** 笔记按 tag 分组统计（is_deleted=0） */
    private List<Map<String, Object>> buildTagDistFromNotes(Long userId) {
        QueryWrapper<Note> qw = new QueryWrapper<Note>()
                .select("tag, count(*) as cnt")
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .groupBy("tag");
        List<Map<String, Object>> maps = noteMapper.selectMaps(qw);
        return normalizeTagDist(maps);
    }

    /** 待办按 label 分组统计（is_deleted=0） */
    private List<Map<String, Object>> buildTagDistFromTodos(Long userId) {
        QueryWrapper<Todo> qw = new QueryWrapper<Todo>()
                .select("label as tag, count(*) as cnt")
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .groupBy("label");
        List<Map<String, Object>> maps = todoMapper.selectMaps(qw);
        return normalizeTagDist(maps);
    }

    /** 统一把 selectMaps 结果映射为 {tag, count} 列表，按数量倒序，未知标签归为 "未分类" */
    private List<Map<String, Object>> normalizeTagDist(List<Map<String, Object>> maps) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (maps == null) return result;
        for (Map<String, Object> row : maps) {
            Object tag = row.get("tag");
            Object cnt = row.get("cnt");
            String tagStr = (tag == null || tag.toString().trim().isEmpty()) ? "未分类" : tag.toString();
            int count = 0;
            if (cnt instanceof Number) {
                count = ((Number) cnt).intValue();
            } else if (cnt != null) {
                try {
                    count = Integer.parseInt(cnt.toString());
                } catch (NumberFormatException ignored) {
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tag", tagStr);
            item.put("count", count);
            result.add(item);
        }
        result.sort(Comparator.comparingInt(item -> -((Number) item.get("count")).intValue()));
        return result;
    }

    private long countNotes(Long userId) {
        return noteMapper.selectCount(new QueryWrapper<Note>()
                .eq("user_id", userId)
                .eq("is_deleted", 0));
    }

    private long countTodos(Long userId) {
        return todoMapper.selectCount(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .eq("is_deleted", 0));
    }

    private long countCompletedTodos(Long userId) {
        return todoMapper.selectCount(new QueryWrapper<Todo>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .eq("is_completed", 1));
    }

    private long countMonthNotes(Long userId, LocalDateTime monthStart) {
        return noteMapper.selectCount(new QueryWrapper<Note>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .ge("create_time", monthStart));
    }
}
