package reports;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FailureExportUtil {

    private static final int EXCEL_CELL_LIMIT = 32767;

    private static final String FILE_PATH =
            "test-output/FailedCases_"
                    + System.currentTimeMillis()
                    + ".xlsx";
    private static final Queue<FailureRecord> FAILURES = new ConcurrentLinkedQueue<>();

    public static void exportFailure(
            String tcId,
            String classification,
            String error,
            String fullLog,
            Map<String, String> data) {
        FAILURES.add(new FailureRecord(tcId, classification, error, fullLog, data));
    }

    public static void flushFailures() {
        synchronized (FailureExportUtil.class) {
            if (FAILURES.isEmpty()) {
                return;
            }

            try {

                File file = new File(FILE_PATH);
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }

                try (Workbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("Failures");
                    createHeader(workbook, sheet);

                    FailureRecord record;
                    while ((record = FAILURES.poll()) != null) {
                        appendFailure(workbook, sheet, record);
                    }

                    sizeColumns(sheet);

                    try (FileOutputStream fos = new FileOutputStream(FILE_PATH)) {
                        workbook.write(fos);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void createHeader(Workbook workbook, Sheet sheet) {
        Row header = sheet.createRow(0);

        String[] headers = {
                "TC_ID",
                "Sub-Module",
                "Priority",
                "Summary",
                "Steps",
                "Expected Result",
                "Defect Type",
                "Error Message",
                "Full Automation Log"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private static void appendFailure(Workbook workbook, Sheet sheet, FailureRecord record) {
        int lastRow = sheet.getLastRowNum();
        Row row = sheet.createRow(lastRow + 1);

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        row.createCell(0).setCellValue(record.tcId());
        row.createCell(1).setCellValue(getValue(record.data(), "Sub-Module"));
        row.createCell(2).setCellValue(getValue(record.data(), "Priority"));
        row.createCell(3).setCellValue(getValue(record.data(), "Summary"));

        Cell stepsCell = row.createCell(4);
        stepsCell.setCellValue(getValue(record.data(), "Steps"));
        stepsCell.setCellStyle(wrapStyle);

        Cell expectedCell = row.createCell(5);
        expectedCell.setCellValue(getValue(record.data(), "Expected"));
        expectedCell.setCellStyle(wrapStyle);

        row.createCell(6).setCellValue(record.classification());

        Cell errorCell = row.createCell(7);
        errorCell.setCellValue(record.error());
        errorCell.setCellStyle(wrapStyle);

        Cell logCell = row.createCell(8);
        logCell.setCellValue(limitForExcel(record.fullLog()));
        logCell.setCellStyle(wrapStyle);
    }

    private static void sizeColumns(Sheet sheet) {
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.setColumnWidth(3, 8000);
        sheet.setColumnWidth(4, 20000);
        sheet.setColumnWidth(5, 12000);
        sheet.setColumnWidth(6, 8000);
        sheet.setColumnWidth(7, 20000);
        sheet.setColumnWidth(8, 30000);
    }

    private static String getValue(Map<String, String> data, String key) {
        if (data == null) {
            return "-";
        }

        String value = data.get(key);
        return value == null ? "-" : value;
    }

    private static String limitForExcel(String value) {
        if (value == null) {
            return "-";
        }

        if (value.length() <= EXCEL_CELL_LIMIT) {
            return value;
        }

        String suffix = "\n\n[Log truncated because Excel cells support a maximum of "
                + EXCEL_CELL_LIMIT + " characters.]";
        return value.substring(0, EXCEL_CELL_LIMIT - suffix.length()) + suffix;
    }

    private record FailureRecord(
            String tcId,
            String classification,
            String error,
            String fullLog,
            Map<String, String> data) {
    }
}
