package reports;

import org.apache.poi.ss.usermodel.*;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExcelDataManager {

    private static final Map<String, Map<String, String>> TEST_DATA = loadTestData();

    public static Map<String, String> getTestData(String tcId) {
        Map<String, String> data = TEST_DATA.get(tcId.toLowerCase());

        if (data != null) {
            return new HashMap<>(data);
        }

        Map<String, String> fallback = new HashMap<>();
        fallback.put("TC_ID", tcId);
        fallback.put("Summary", "No data found for TC_ID: " + tcId);
        fallback.put("Priority", "-");
        fallback.put("Steps", "-");
        fallback.put("Expected", "-");
        fallback.put("Sub-Module", "-");

        return fallback;
    }

    private static Map<String, Map<String, String>> loadTestData() {
        Map<String, Map<String, String>> testData = new HashMap<>();

        try (InputStream is = ExcelDataManager.class
                .getClassLoader()
                .getResourceAsStream("TestCase Master.xlsx")) {

            if (is == null) {
                throw new RuntimeException("Excel file not found in resources");
            }

            try (Workbook workbook = WorkbookFactory.create(is)) {
                Sheet sheet = workbook.getSheetAt(0);

                for (Row row : sheet) {

                    if (row.getRowNum() == 0) continue; // skip header

                    Cell tcCell = row.getCell(0);
                    if (tcCell == null) continue;

                    String excelTcId = tcCell.toString().trim();
                    if (excelTcId.isEmpty()) continue;

                    Map<String, String> data = new HashMap<>();
                    data.put("TC_ID", excelTcId);
                    data.put("Sub-Module", getCellValue(row.getCell(1)));
                    data.put("Priority", getCellValue(row.getCell(2)));
                    data.put("Summary", getCellValue(row.getCell(3)));
                    data.put("Steps", getCellValue(row.getCell(4)));
                    data.put("Expected", getCellValue(row.getCell(5)));

                    testData.put(excelTcId.toLowerCase(), data);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Collections.unmodifiableMap(testData);
    }

    // 🔹 Safe cell reader (handles null + different types)
    private static String getCellValue(Cell cell) {

        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            default:
                return cell.toString();
        }
    }
}
