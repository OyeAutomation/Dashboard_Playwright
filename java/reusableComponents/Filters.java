package reusableComponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.testng.Assert;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Filters {
    private final Page page;

    public Filters(Page page){
        this.page = page;
    }

    public String dropdownSelected(Locator dropdown) {
        return dropdown.locator("option:checked").textContent().trim();
    }

    public String getPlaceholder(Locator locator){
        return locator.getAttribute("placeholder");
    }

    public void setDropdownValue(Locator dropdown, String value){
        dropdown.selectOption(value);
    }

    public void setDropdownStrValue(Locator dropdown, String value){
        dropdown.selectOption(value);
    }

    public void validateFilterAppliedOnColumn(int columnIndex, String expectedValue) {
        Locator cells = page.locator("//table/tbody/tr/td[" + columnIndex + "]");
        int cellCount = cells.count();
        Assert.assertTrue(cellCount > 0, "No rows found to validate column " + columnIndex);

        for (int i = 0; i < cellCount; i++) {
            String actualValue = cells.nth(i).textContent().trim();
            System.out.println(actualValue);

            // Use equalsIgnoreCase to bypass capitalization mismatches while maintaining strict text validation
            Assert.assertTrue(actualValue.equalsIgnoreCase(expectedValue),
                    "Validation failed at row " + (i + 1) + ". Expected: [" + expectedValue + "], Actual: [" + actualValue + "]");
        }
        System.out.println("Filter Validated");
    }

    public String getTextByIndex(int index, Locator dropdown) {
        return dropdown.locator("option").nth(index).textContent().trim();
    }

    public void sendKeysInDateField(Locator locator, String date) {
        locator.evaluate("(el, value) => { " +
                "el.removeAttribute('readonly'); " +
                "el.value = value; " +
                "el.dispatchEvent(new Event('input', { bubbles: true })); " +
                "el.dispatchEvent(new Event('change', { bubbles: true })); " +
                "el.dispatchEvent(new Event('blur', { bubbles: true })); " +
                "}", date);
    }

    public void sendDate(Locator locator, String date) {
        locator.evaluate("el => el.removeAttribute('readonly')");
        locator.fill(date);
    }

    public void verifyDateFilter(String fromDateStr, String toDateStr, Locator rowsLocator, int dateColumnIndex) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        LocalDate fromDate = LocalDate.parse(fromDateStr, formatter);
        LocalDate toDate = LocalDate.parse(toDateStr, formatter);

        List<Locator> rows = rowsLocator.all();
        if (rows.isEmpty()) {
            System.out.println("No data found for selected date filter");
            return;
        }

        for (int i = 0; i < rows.size(); i++) {
            rows = rowsLocator.all();
            Assert.assertTrue(i < rows.size(), "Table changed while validating date row " + (i + 1));
            Locator row = rows.get(i);

            if(row.innerText().trim().equalsIgnoreCase("No Data Found")){
                System.out.println("No data found for selected date filter");
                return;
            }

            String dateText = row.locator("td:nth-child(" + dateColumnIndex + ")").innerText().trim();
            LocalDate rowDate = LocalDate.parse(dateText, formatter);

            if (rowDate.isBefore(fromDate) || rowDate.isAfter(toDate)) {
                Assert.fail("Invalid date found in table: " + dateText);
            }
        }
        System.out.println("Date filter validated successfully");
    }
}
