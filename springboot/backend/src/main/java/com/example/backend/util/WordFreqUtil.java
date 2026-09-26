package com.example.backend.util;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词频统计工具（基于 jieba 分词）。
 * 中文用 jieba 精确模式分词，英文/数字按完整单词提取；
 * 标题、标签等完整词可由调用方加权重复传入。
 */
public final class WordFreqUtil {

    private WordFreqUtil() {
    }

    /** jieba 分词器（精确模式，线程安全可复用） */
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    /** 英文单词或数字（2 字符及以上，过滤单字母） */
    private static final Pattern EN_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");

    /** 停用词：常见无意义词、系统用词 */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "上", "也",
            "很", "到", "说", "要", "去", "你", "会", "着", "没", "看", "好", "自", "己", "这",
            "一个", "我们", "你们", "他们", "什么", "怎么", "可以", "因为", "所以", "但是", "如果",
            "还是", "就是", "这个", "那个", "这样", "那样", "时候", "现在", "已经", "还有",
            "待办", "任务", "笔记", "内容", "标题",
            "进行", "需要", "应该", "可能", "或者", "不过", "而已", "然后", "比如",
            "里面", "外面", "上面", "下面", "前面", "后面", "旁边", "中间"
    ));

    /**
     * 统计一组文本中的高频词。
     *
     * @param texts 待统计文本（调用方可将标题/标签重复传入以加权）
     * @param limit 返回前 N 个高频词
     * @return 有序列表，每项含 name（词）和 value（频次），按频次倒序
     */
    public static List<Map<String, Object>> topWords(List<String> texts, int limit) {
        Map<String, Integer> freq = new HashMap<>();
        for (String text : texts) {
            if (text == null || text.isEmpty()) {
                continue;
            }
            // jieba 中文分词
            List<SegToken> tokens = SEGMENTER.process(text, JiebaSegmenter.SegMode.INDEX);
            for (SegToken token : tokens) {
                String word = token.word.trim();
                if (word.length() < 2) {
                    continue;
                }
                // 过滤纯标点/空白
                if (word.matches("[\\p{Punct}\\s]+")) {
                    continue;
                }
                freq.merge(word, 1, Integer::sum);
            }
            // 英文 / 数字：完整单词（jieba 对英文切分不理想，单独提取）
            Matcher enMatcher = EN_PATTERN.matcher(text);
            while (enMatcher.find()) {
                freq.merge(enMatcher.group().toLowerCase(), 1, Integer::sum);
            }
        }
        return freq.entrySet().stream()
                .filter(e -> !STOP_WORDS.contains(e.getKey()))
                .sorted(Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed())
                .limit(limit)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>(2);
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }
}
