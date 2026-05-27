package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.DashboardPage;
import reusableComponents.PaginationUtils;
import reusableComponents.SearchFilter;
import utils.ConfigReader;
import utils.AuthenticationService;

import java.nio.file.Paths;
import java.sql.SQLException;
import utils.ElementUtils;
import utils.WaitUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static utils.ElementUtils.SQLI;

@Listeners(TestListener.class)
public class DashboardTest extends PlaywrightManager {

    private DashboardPage dashPage;
    private WaitUtils wait;
    private SearchFilter searchFilter;
    private PaginationUtils pg;
    private ElementUtils elementUtils;
    private static final int DEFAULT_PAGE_SIZE = 10;
    public String walletBalance;

    @BeforeClass(alwaysRun = true)
    public void classSetup() throws SQLException, InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        dashPage = new DashboardPage(page);
        wait = new WaitUtils(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        // In your exception catch block or teardown:
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("headless-failure.png")));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        wait.waitForInvisibility(dashPage.getLoader());
        elementUtils.handleOptionalPanel();
        searchFilter = new SearchFilter(page);
        pg = new PaginationUtils(page);
    }

    @Test(priority = 1, description = "Verify clicking a cost center opens View Passbook")
    public void CC_DASH_002() {
        elementUtils.doClick(dashPage.getCostCenterNameLocator());
        boolean isBold = wait.isClassContains(dashPage.getViewPassbookBtn(), "class", "fw-bold");
        Assert.assertTrue(isBold, "Passbook page didn't open");
        elementUtils.doClick(dashPage.getViewPassbookBackBtn());
    }

    @Test(priority = 2, description = "Verify available balance is visible on Dashboard")
    public void CC_DASH_003() {
        boolean isAvailBal = elementUtils.isElementExist(dashPage.getBalanceLocator());
        System.out.println("Balance: " + dashPage.getAvailableBalance());
        Assert.assertTrue(isAvailBal, "Available balance not found");
    }

    @Test(priority = 3, description = "Verify cost center entries are visible on Dashboard")
    public void CC_DASH_004() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        boolean isTable = elementUtils.isElementExist(dashPage.getCcTable());
        Assert.assertTrue(isTable, "Cost Center Data not found in Table");
    }

    @Test(priority = 4, description = "Verify Search button is disabled when no data in search field")
    public void CC_DASH_005() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        boolean isDisabled = searchFilter.blankSearch(dashPage.getFilterSearchField(), dashPage.getFilterSearchBtn());
        Assert.assertTrue(isDisabled, "Search button is enabled for blank input");
    }

    @Test(priority = 5, description = "Verify Search button is disabled when ' '(space) in search field")
    public void CC_DASH_006() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        searchFilter.spaceSearch(dashPage.getFilterSearchField(), dashPage.getFilterSearchBtn());
    }

    @Test(priority = 6, description = "Verify Error toaster message when special char in search field")
    public void CC_DASH_007() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        try {
            String actualToast = searchFilter.specialSearch(
                    dashPage.getFilterSearchField(),
                    dashPage.getFilterSearchBtn(),
                    dashPage.getToastMessage(),
                    "@$", dashPage.getLoader(), dashPage.getNoDataText()
            );
            Assert.assertEquals(actualToast.trim(), "Invalid search input", "Wrong validation message shown");
        } catch (TimeoutError e) {
            Assert.fail("Expected error message not found.");
        }
    }

    @Test(priority = 7, description = "Verify Error toaster message when hash char in search field")
    public void CC_DASH_008() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        try {
            String actualToast = searchFilter.hashError(
                    dashPage.getFilterSearchField(),
                    dashPage.getFilterSearchBtn(),
                    dashPage.getToastMessage(), dashPage.getNoDataText()
            );
            Assert.assertEquals(actualToast.trim(), "Invalid search input",
                    "Toast Error Message dont match Expected Message \"Invalid search input\"");
        } catch (TimeoutError e) {
            Assert.fail("Expected error message not found.");
        }
    }

    @Test(priority = 8, description = "Verify Search works correctly")
    public void CC_DASH_009() {
        elementUtils.clearField(dashPage.getFilterSearchField());

        String value = dashPage.getTableRows().first().locator("td").first().innerText().trim();
        System.out.println("First Cost Center Name: " + value);
        Assert.assertFalse(value.isEmpty(), "Could not read a searchable cost center value from the table.");

        searchFilter.validSearch(
                dashPage.getFilterSearchField(),
                dashPage.getFilterSearchBtn(),
                0,
                dashPage.getTableRows(),
                value
        );
    }

    @Test(priority = 9, dependsOnMethods = "CC_DASH_009", description = "Verify Reset button works")
    public void CC_DASH_010() {
        elementUtils.doClick(dashPage.getResetBtn());
        wait.waitForInvisibility(dashPage.getLoader());

        String afterReset = dashPage.getFilterSearchField().getAttribute("value");
        Assert.assertTrue(afterReset == null || afterReset.trim().isEmpty(), "Reset button did not clear the search field.");
    }

    @Test(priority = 10, description = "Verify Search gives 'No Data Found' when data is invalid")
    public void CC_DASH_011() {
        String errorMsg = searchFilter.invalidSearch(
                dashPage.getFilterSearchField(),
                dashPage.getFilterSearchBtn(),
                dashPage.getNoDataText(),
                "VeryInvalidCostCenter0000001"
        );

        Assert.assertFalse(errorMsg == null || errorMsg.trim().isEmpty(), "Expected error message not found.");
        Assert.assertEquals(errorMsg.trim(), "No Data Found", "Wrong validation message shown");
        elementUtils.doClick(dashPage.getResetBtn());
    }

    @Test(priority = 11, description = "Verify Reset Button is disabled when Search field is empty")
    public void CC_DASH_012() {
        elementUtils.clearField(dashPage.getFilterSearchField());
        boolean isDisable = searchFilter.blankSearch(dashPage.getFilterSearchField(), dashPage.getResetBtn());
        Assert.assertTrue(isDisable, "Reset button is enabled for blank input");
    }

    @Test(priority = 12, description = "Verify page data entry change works for 20 entries")
    public void CC_DASH_013() {
        int displayedCount = pg.selectEntryCountAndParse(dashPage.getDataEntryChange(), dashPage.getPageCount(), 1);
        Assert.assertEquals(displayedCount, 20, "Entry change to 20 did not work");
    }

    @Test(priority = 13, description = "Verify page data entry change works with 30 entries")
    public void CC_DASH_014() {
        int displayedCount = pg.selectEntryCountAndParse(dashPage.getDataEntryChange(), dashPage.getPageCount(), 2);
        Assert.assertEquals(displayedCount, 30, "Entry change to 30 did not work");
    }

    @Test(priority = 14, description = "Verify Pagination is visible")
    public void CC_DASH_015() {
        pg.resetEntryChange(dashPage.getDataEntryChange());
        boolean present = pg.isPaginationVisible(dashPage.getPaginationLocator());
        Assert.assertTrue(present, "Pagination is not visible");
    }

    @Test(priority = 15, dependsOnMethods = "CC_DASH_015", description = "Verify Next changes the page to next page")
    public void CC_DASH_016() {
        elementUtils.doClick(dashPage.getPageNextBtn());
        wait.waitForInvisibility(dashPage.getLoader());

        int currPageNo = pg.getCurrentPageNumber(dashPage.getPageCount());
        Assert.assertEquals(currPageNo, 2, "Next Button did not navigate to page 2");
    }

    @Test(priority = 16, dependsOnMethods = "CC_DASH_016", description = "Verify Previous changes the page to previous")
    public void CC_DASH_017() {
        elementUtils.doClick(dashPage.getPagePrevBtn());
        wait.waitForInvisibility(dashPage.getLoader());

        int currPageNo = pg.getCurrentPageNumber(dashPage.getPageCount());
        Assert.assertEquals(currPageNo, 1, "Prev Button did not navigate back to page 1");
    }

    @Test(priority = 17, description = "Verify Previous is disabled when on page 1")
    public void CC_DASH_018() {
        elementUtils.doClick(dashPage.getBtnAfterPrev());
        boolean isPrevDisabled = !pg.isPreviousEnabled(dashPage.getPagePrevBtn());
        Assert.assertTrue(isPrevDisabled, "Prev Button should be disabled on page 1");
    }

    @Test(priority = 18, description = "Verify Next is disabled and Prev is enabled on the last page")
    public void CC_DASH_019() {
        elementUtils.doClick(dashPage.getBtnBeforeNext());

        Assert.assertTrue(
                pg.isPreviousEnabled(dashPage.getPagePrevBtn()),
                "Prev button should be enabled on last page"
        );

        Assert.assertFalse(
                pg.isNextEnabled(dashPage.getPageNextBtn()),
                "Next button should be disabled on last page"
        );
    }

    @Test(priority = 19, description = "Verify click on last page shows left over data")
    public void CC_DASH_020() {
        int totalRecords = pg.parseTotalRecords(elementUtils.doGetText(dashPage.getPageCount()).trim());
        int expectedLastPageRows = totalRecords % DEFAULT_PAGE_SIZE == 0
                ? DEFAULT_PAGE_SIZE
                : totalRecords % DEFAULT_PAGE_SIZE;

        int lastPageRows = pg.lastPageContent(dashPage.getPageCount());

        Assert.assertEquals(
                lastPageRows,
                expectedLastPageRows,
                "Last Page has incorrect data. Expected leftover rows to match the page remainder."
        );
    }

    @Test(priority = 20, description = "Verify changing page size from 30 to 10 resets pagination to page 1")
    public void CC_DASH_021() {
        Locator dropdown = dashPage.getDataEntryChange();
        dropdown.selectOption("10");

        boolean isPageNoReset = 1 == pg.getCurrentPageNumber(dashPage.getPageCount());
        dropdown.selectOption("10");
        Assert.assertTrue(isPageNoReset, "When No of Entry Change, Page No is not being reset");
    }

    @Test(priority = 21, description = "Verify Cost Center search blocks SQL injection payloads")
    public void CC_DASH_022() {
        Locator searchField = dashPage.getFilterSearchField();
        Locator searchBtn   = dashPage.getFilterSearchBtn();
        Locator loader      = dashPage.getLoader();
        Locator toast       = dashPage.getToastMessage().first();
        Locator notFound    = dashPage.getNotFoundLocator().first();
        Locator resetBtn    = dashPage.getResetBtn();

        for (String payload : SQLI) {
            searchField.fill(payload);
            searchBtn.click();

            try {
                loader.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(3000));
            } catch (com.microsoft.playwright.PlaywrightException ignored) {
                // Loader may disappear too fast or not appear at all
            }

            boolean noDataVisible = isVisible(notFound);
            if (noDataVisible) {
                Assert.assertTrue(true, "Safe no-data state for payload: " + payload);
                continue;
            }

            boolean toastVisible = isVisible(toast);
            if (!toastVisible) {
                Assert.fail("No visible response found for payload: " + payload);
            }

            String msg = safeText(toast).toLowerCase().trim();

            boolean isSafeResponse =
                    msg.contains("invalid") ||
                            msg.contains("error") ||
                            msg.contains("unsafe");

            Assert.assertTrue(isSafeResponse,
                    "CRITICAL: Information Leakage / Unexpected response for payload: "
                            + payload + " => " + msg);
        }

        resetBtn.click();
    }

    private boolean isVisible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (com.microsoft.playwright.PlaywrightException e) {
            return false;
        }
    }

    private String safeText(Locator locator) {
        try {
            String text = locator.textContent();
            return text == null ? "" : text;
        } catch (com.microsoft.playwright.PlaywrightException e) {
            return "";
        }
    }

    @Test(priority = 22, description = "Export Button Downloads file")
    public void CC_DASH_023() {
        String downloadPath = Path.of(ConfigReader.getProperty("download.dir", "target/downloads"))
                .toAbsolutePath().toString();

        File folder = new File(downloadPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        Path downloadedFile = wait.waitForDownload(
                () -> elementUtils.doClick(dashPage.getExportBtn()),
                downloadPath,
                10
        );

        Assert.assertTrue(Files.exists(downloadedFile), "Downloaded file does not exist");
        Assert.assertTrue(downloadedFile.getFileName().toString().contains("Cost Center_export"),
                "Cost Center export file was not downloaded");
    }

    @Test(priority = 23, description = "Export Button downloads filtered data")
    public void CC_DASH_024() throws Exception {
        elementUtils.sendKeys(dashPage.getFilterSearchField(), "bulkvoucher cc");
        elementUtils.doClick(dashPage.getFilterSearchBtn());
        wait.waitForInvisibility(dashPage.getLoader());

        Path downloadedFile = wait.waitForDownload(
                () -> elementUtils.doClick(dashPage.getExportBtn()),
                ConfigReader.getProperty("download.dir", "opt/downloads"),
                10
        );

        Assert.assertTrue(downloadedFile != null && Files.exists(downloadedFile), "Downloaded file does not exist");
        Assert.assertTrue(fileContainsText(downloadedFile, "BulkVoucher CC"), "Filtered data was not exported");
    }

    private void goToLastPage() {
        wait.waitForInvisibility(dashPage.getLoader());

        String pageCountText = elementUtils.doGetText(dashPage.getPageCount()).trim();
        int totalRecords = pg.parseTotalRecords(pageCountText);
        int totalPages = (totalRecords + DEFAULT_PAGE_SIZE - 1) / DEFAULT_PAGE_SIZE;

        int maxClicks = Math.min(totalPages - 1, 1000);
        int clickCount = 0;

        while (pg.isNextEnabled(dashPage.getPageNextBtn()) && clickCount < maxClicks) {
            elementUtils.doClick(dashPage.getPageNextBtn());
            wait.waitForInvisibility(dashPage.getLoader());
            clickCount++;
        }
    }

    private boolean fileContainsText(Path file, String expected) throws IOException {
        String lowerName = file.getFileName().toString().toLowerCase();

        if (lowerName.endsWith(".csv") || lowerName.endsWith(".txt") || lowerName.endsWith(".xml")) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return content.contains(expected);
        }

        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xlsm") || lowerName.endsWith(".zip")) {
            try (ZipFile zipFile = new ZipFile(file.toFile())) {
                for (ZipEntry entry : java.util.Collections.list(zipFile.entries())) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        if (content.contains(expected)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        byte[] bytes = Files.readAllBytes(file);
        return new String(bytes, StandardCharsets.UTF_8).contains(expected);
    }

    @AfterClass(alwaysRun = true)
    public void closeSessionContext() {
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }
}
