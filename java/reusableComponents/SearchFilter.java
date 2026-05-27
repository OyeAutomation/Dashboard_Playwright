package reusableComponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.testng.Assert;
import utils.ElementUtils;
import utils.WaitUtils;

import java.util.List;

public class SearchFilter {

    private final Page page;
    private final ElementUtils elementUtils;
    private final WaitUtils wait;

    public SearchFilter(Page page) {
        this.page = page;
        this.elementUtils = new ElementUtils(page);
        this.wait = new WaitUtils(page);
    }

    public boolean blankSearch(Locator searchField, Locator disabledBtn) {
        elementUtils.clearField(searchField);
        return !wait.isBtnEnabled(disabledBtn);
    }

    public void spaceSearch(Locator searchField, Locator searchBtn) {
        elementUtils.clearField(searchField);
        elementUtils.sendKeys(searchField, "  ");
        boolean isButtonDisabled = !wait.isBtnEnabled(searchBtn);
        Assert.assertTrue(isButtonDisabled, "Button is enabled for spaces-only input");
    }

    public String specialSearch(Locator searchField, Locator searchBtn, Locator toast, String value, Locator loader, Locator noDataText) {
        elementUtils.clearField(searchField);
        elementUtils.sendKeys(searchField, value);
        elementUtils.doClick(searchBtn);

        wait.waitForEitherElementToBeVisible(toast, noDataText);

        if (toast.count() > 0 && toast.first().isVisible()) {
            return toast.first().innerText();
        }

        noDataText.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return noDataText.innerText();
    }

    public String hashError(Locator searchField, Locator searchBtn, Locator toast, Locator noDataText) {
        elementUtils.clearField(searchField);
        elementUtils.sendKeys(searchField, "##");
        elementUtils.doClick(searchBtn);

        wait.waitForEitherElementToBeVisible(toast, noDataText);

        if (toast.count() > 0 && toast.first().isVisible()) {
            return toast.first().innerText();
        }

        noDataText.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return noDataText.innerText();
    }

    public String invalidSearch(Locator searchField, Locator searchBtn, Locator messageElement, String invalidVal) {
        searchField.clear();
        searchField.fill(invalidVal);
        searchBtn.click();

        messageElement.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String message = messageElement.innerText().trim();

        if (message.isEmpty()) {
            throw new AssertionError("Error message element appeared but contained no text.");
        }
        return message;
    }

    public void validSearch(Locator searchField, Locator searchBtn, int columnIndex, Locator rowsLocator, String value) {
        elementUtils.clearField(searchField);
        elementUtils.sendKeys(searchField, value);

        // Capture the current network/API state or attach a listener if needed,
        // or rely on a deterministic wait.
        elementUtils.doClick(searchBtn);

        // FIX 1: Wait for the network to settle so the table actually updates
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // FIX 2: Handle the "No Data" placeholder safely if it appears
        Locator noDataFoundPlaceholder = page.locator("//td[normalize-space(translate(text(), " +
                "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'))='no data found']");

        if (noDataFoundPlaceholder.isVisible()) {
            Assert.fail("Search returned 'No Data Found' for value: " + value);
        }

        // FIX 3: Fetch the fresh rows *after* the DOM has definitely updated
        List<Locator> rows = rowsLocator.all();
        Assert.assertFalse(rows.isEmpty(), "No rows found after search for value: " + value);

        String expectedValue = value.trim().toLowerCase();
        int rowCount = rows.size();

        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            List<Locator> cells = rows.get(rowIndex).locator("td").all();

            Assert.assertTrue(columnIndex < cells.size(),
                    "Row " + rowIndex + " has only " + cells.size() + " column(s), but index " + columnIndex + " was requested.");

            String actualValue = cells.get(columnIndex).innerText().trim().toLowerCase();
            Assert.assertTrue(actualValue.contains(expectedValue),
                    "Row " + rowIndex + " value '" + actualValue + "' does not contain '" + expectedValue + "'.");
        }
    }

    public void resetDisabled(Locator searchField, Locator resetBtn) {
        elementUtils.clearField(searchField);
        boolean isButtonDisabled = !wait.isBtnEnabled(resetBtn);
        Assert.assertTrue(isButtonDisabled, "Search button is enabled for blank input");
    }
}
