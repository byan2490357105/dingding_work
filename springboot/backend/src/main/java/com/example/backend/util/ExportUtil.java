package com.example.backend.util;

import com.example.backend.entity.Note;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 导出工具：CSV / Word 生成与文件下载响应。
 */
public final class ExportUtil {

    /** 时间格式：精确到分钟 */
    private static final DateTimeFormatter MINUTE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** 正文主题色（与 Word 内置“标题 2”蓝色一致） */
    private static final String HEADING_COLOR = "2E74B5";
    private static final String MAIN_TITLE_COLOR = "1F4E79";
    private static final String META_COLOR = "595959";
    private static final String CONTENT_COLOR = "333333";

    private ExportUtil() {
    }

    // ==================== CSV ====================

    /**
     * 生成 CSV（UTF-8 带 BOM，Excel 直接打开中文不乱码；字段全部加引号防逗号/换行破坏结构）。
     */
    public static byte[] toCsv(String[] headers, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM：Excel 依据 BOM 识别编码
        sb.append('\uFEFF');
        appendCsvRow(sb, headers);
        for (String[] row : rows) {
            appendCsvRow(sb, row);
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendCsvRow(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            String value = cells[i] == null ? "" : cells[i];
            sb.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        sb.append("\r\n");
    }

    // ==================== Word ====================

    /**
     * 将笔记列表导出为 Word 文档。
     * 样式：每个笔记标题为二级标题（大纲级别 2，导航窗格可见），
     * 创建时间与标签同行显示，正文 1.5 倍行距、宋体小五（9pt）。
     */
    public static byte[] notesToWord(List<Note> notes) {
        try (XWPFDocument document = new XWPFDocument()) {
            setupA4Page(document);
            createHeading2Style(document);

            // 文档主标题
            XWPFParagraph mainTitle = document.createParagraph();
            mainTitle.setAlignment(ParagraphAlignment.CENTER);
            mainTitle.setSpacingAfter(120);
            XWPFRun titleRun = mainTitle.createRun();
            titleRun.setText("随手记");
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setColor(MAIN_TITLE_COLOR);
            titleRun.setFontFamily("微软雅黑");
            titleRun.setFontFamily("微软雅黑", XWPFRun.FontCharRange.eastAsia);

            // 副标题：笔记数量 + 导出时间，底部灰色分隔线
            XWPFParagraph subLine = document.createParagraph();
            subLine.setAlignment(ParagraphAlignment.CENTER);
            subLine.setSpacingAfter(240);
            XWPFRun subRun = subLine.createRun();
            subRun.setText("共 " + notes.size() + " 篇笔记 · 导出于 "
                    + LocalDateTime.now().format(MINUTE_FORMAT));
            subRun.setFontSize(9);
            subRun.setColor(META_COLOR);
            subRun.setFontFamily("宋体");
            subRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
            addBottomBorder(subLine);

            if (notes.isEmpty()) {
                XWPFParagraph empty = document.createParagraph();
                empty.setAlignment(ParagraphAlignment.CENTER);
                empty.setSpacingBefore(240);
                XWPFRun emptyRun = empty.createRun();
                emptyRun.setText("暂无笔记");
                emptyRun.setFontSize(9);
                emptyRun.setColor(META_COLOR);
                emptyRun.setFontFamily("宋体");
                emptyRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
            }

            for (Note note : notes) {
                // 标题：应用自定义的二级标题样式
                XWPFParagraph titlePara = document.createParagraph();
                titlePara.setStyle("Heading2");
                titlePara.createRun().setText(
                        note.getTitle() == null || note.getTitle().isEmpty() ? "无标题" : note.getTitle());

                // 创建时间与标签同一行
                XWPFParagraph metaPara = document.createParagraph();
                metaPara.setSpacingAfter(120);
                XWPFRun timeRun = metaPara.createRun();
                timeRun.setText("创建时间：" + formatMinute(note.getCreateTime()));
                timeRun.setFontSize(9);
                timeRun.setColor(META_COLOR);
                timeRun.setFontFamily("宋体");
                timeRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);

                XWPFRun sepRun = metaPara.createRun();
                sepRun.setText("　　|　　");
                sepRun.setFontSize(9);
                sepRun.setColor("BFBFBF");
                sepRun.setFontFamily("宋体");
                sepRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);

                XWPFRun tagRun = metaPara.createRun();
                tagRun.setText("标签：" + (note.getTag() == null || note.getTag().isEmpty() ? "无" : note.getTag()));
                tagRun.setFontSize(9);
                tagRun.setColor(HEADING_COLOR);
                tagRun.setFontFamily("宋体");
                tagRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);

                // 内容：富文本转纯文本，1.5 倍行距，宋体小五
                String text = htmlToText(note.getContent());
                if (text.isEmpty()) {
                    XWPFParagraph emptyContent = document.createParagraph();
                    emptyContent.setSpacingAfter(120);
                    XWPFRun emptyRun = emptyContent.createRun();
                    emptyRun.setText("（暂无内容）");
                    emptyRun.setItalic(true);
                    emptyRun.setFontSize(9);
                    emptyRun.setColor("999999");
                    emptyRun.setFontFamily("宋体");
                    emptyRun.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
                } else {
                    for (String line : text.split("\n")) {
                        if (line.trim().isEmpty()) {
                            continue;
                        }
                        XWPFParagraph para = document.createParagraph();
                        para.setSpacingBetween(1.5, LineSpacingRule.AUTO);
                        para.setSpacingAfter(60);
                        XWPFRun run = para.createRun();
                        run.setText(line.trim());
                        run.setFontSize(9);
                        run.setColor(CONTENT_COLOR);
                        run.setFontFamily("宋体");
                        run.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成 Word 文档失败", e);
        }
    }

    /** 注册“Heading2”样式：蓝色加粗 14pt 微软雅黑，大纲级别 2（即二级标题） */
    private static void createHeading2Style(XWPFDocument document) throws Exception {
        XWPFStyles styles = document.createStyles();

        CTStyle style = CTStyle.Factory.newInstance();
        style.setStyleId("Heading2");
        style.setType(STStyleType.PARAGRAPH);
        style.addNewName().setVal("heading 2");
        style.addNewBasedOn().setVal("Normal");
        style.addNewQFormat();
        style.addNewUiPriority().setVal(BigInteger.valueOf(9));

        CTPPrGeneral ppr = style.addNewPPr();
        ppr.addNewKeepNext();
        ppr.addNewKeepLines();
        CTSpacing spacing = ppr.addNewSpacing();
        spacing.setBefore(BigInteger.valueOf(240));
        spacing.setAfter(BigInteger.valueOf(60));
        ppr.addNewOutlineLvl().setVal(BigInteger.valueOf(1));

        CTRPr rpr = style.addNewRPr();
        CTFonts fonts = rpr.addNewRFonts();
        fonts.setAscii("微软雅黑");
        fonts.setEastAsia("微软雅黑");
        fonts.setHAnsi("微软雅黑");
        // 元素存在即为加粗
        rpr.addNewB();
        rpr.addNewColor().setVal(HEADING_COLOR);
        rpr.addNewSz().setVal(BigInteger.valueOf(28));
        rpr.addNewSzCs().setVal(BigInteger.valueOf(28));

        styles.addStyle(new XWPFStyle(style));
    }

    /** A4 页面 + 常规页边距 */
    private static void setupA4Page(XWPFDocument document) {
        CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
        CTPageSz pageSize = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pageSize.setW(BigInteger.valueOf(11906));
        pageSize.setH(BigInteger.valueOf(16838));
        CTPageMar margin = sectPr.isSetPgMar() ? sectPr.getPgMar() : sectPr.addNewPgMar();
        margin.setTop(BigInteger.valueOf(1440));
        margin.setBottom(BigInteger.valueOf(1440));
        margin.setLeft(BigInteger.valueOf(1800));
        margin.setRight(BigInteger.valueOf(1800));
    }

    /** 段落底部灰色细线，用作装饰分隔 */
    private static void addBottomBorder(XWPFParagraph paragraph) {
        CTPPr ppr = paragraph.getCTP().getPPr();
        if (ppr == null) {
            ppr = paragraph.getCTP().addNewPPr();
        }
        CTPBdr borders = ppr.addNewPBdr();
        CTBorder bottom = borders.addNewBottom();
        bottom.setVal(STBorder.SINGLE);
        bottom.setSz(BigInteger.valueOf(6));
        bottom.setSpace(BigInteger.valueOf(4));
        bottom.setColor("CCCCCC");
    }

    // ==================== 通用 ====================

    /**
     * 富文本 HTML 转纯文本：块级标签转换行，去除其余标签，还原常见实体。
     */
    public static String htmlToText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|tr|blockquote)>", "\n")
                .replaceAll("(?i)<[^>]+>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    /** 时间格式化为 yyyy-MM-dd HH:mm，空值返回空串 */
    public static String formatMinute(LocalDateTime time) {
        return time == null ? "" : time.format(MINUTE_FORMAT);
    }

    /** 导出文件名时间后缀，如 20260915143052 */
    public static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /** 将生成的文件写入 HTTP 下载响应 */
    public static void writeToResponse(HttpServletResponse response, byte[] data,
                                       String contentType, String filename) throws Exception {
        String encoded = URLEncoder.encode(filename, "UTF-8").replace("+", "%20");
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        // 允许前端 JS 读取文件名响应头（跨域场景）
        response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
        response.getOutputStream().flush();
    }
}
