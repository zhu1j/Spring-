package com.crm.util;

import com.crm.entity.Customer;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel 导出工具类
 * <p>
 * 基于 Apache POI 实现，将客户数据导出为 .xlsx 文件。
 * 包含表头样式美化、列宽自适应、日期格式化等功能。
 *
 */
@Slf4j
public class ExcelUtil {
    //Excel 表头列名
    private static final String[] HEADERS = {
            "序号","公司名称","联系人","电话","邮箱",
            "国家/地区","客户类型","跟进状态","客户来源",
            "服务需求","微信号","网站","地址",
            "备注","录入人","创建时间","更新时间"
    };

    // 列宽（字符宽度）
    private static final int[] COLUMN_WIDTHS = {
            6, 25, 12, 15, 25,
            12, 12, 12, 15,
            30, 15, 25, 35,
            30, 12, 20, 20
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void export(HttpServletResponse response, List<Customer> data, String fileName){
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("客户资料");

            // ---- 1. 创建表头 ----
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // ---- 2. 写入数据行 ----
            CellStyle dataStyle = createDataStyle(workbook);
            for (int i =0; i < data.size(); i++) {
                Customer c = data.get(i);
                Row row = sheet.createRow(i + 1);
                fillRow(row, c, i + 1, dataStyle);
            }

            // ---- 3. 设置列宽 ----
            for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
                //POI 列宽单位是 1/256 个字符宽度
                sheet.setColumnWidth(i, COLUMN_WIDTHS[i] * 256);
            }

            // ---- 4. 冻结首行（表头固定） ----
            sheet.createFreezePane(0,1);

            // ---- 5. 输出到浏览器 ----
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedFileName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+","%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);

            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
                os.flush();
            }
            log.info("Excel导出成功，文件名: {}.xlsx，记录数: {}",fileName,data.size());

        } catch (IOException e) {
            log.error("Excel导出失败", e);
            throw  new RuntimeException("Excel导出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 填充第一行数据
     */
    private static void fillRow(Row row,Customer c, int index, CellStyle style){
        int col = 0;
        setCell(row, col++, String.valueOf(index), style);
        setCell(row, col++, c.getCompanyName(), style);
        setCell(row, col++, c.getContactPerson(), style);
        setCell(row, col++, c.getPhone(), style);
        setCell(row, col++, c.getEmail(), style);
        setCell(row, col++, c.getCountry(), style);
        setCell(row, col++, c.getCustomerType(), style);
        setCell(row, col++, c.getStatus(), style);
        setCell(row, col++, c.getSource(), style);
        setCell(row, col++, c.getServiceNeeds(), style);
        setCell(row, col++, c.getWechat(), style);
        setCell(row, col++, c.getWebsite(), style);
        setCell(row, col++, c.getAddress(), style);
        setCell(row, col++, c.getRemark(), style);
        setCell(row, col++, c.getCreatedBy(), style);
        setCell(row, col++, formatDate(c.getCreatedAt()), style);
        setCell(row, col++, formatDate(c.getUpdatedAt()), style);

    }
    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value== null ? "" : value);
        cell.setCellStyle(style);
    }

    public static String formatDate(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DATE_FMT);
    }


    /**
     * 创建表头样式：加粗、深蓝背景、白色字体、居中对齐
     */
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 字体
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // 填充色
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 对齐
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 创建数据行样式：正常字体、无背景、左对齐
     */
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // 垂直居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 边框
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // 自动换行（应对长文本）
        style.setWrapText(true);

        return style;
    }
}
