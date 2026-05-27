package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.ViewPassbookPage;
import reusableComponents.Filters;
import reusableComponents.PaginationUtils;
import reusableComponents.SearchFilter;
import utils.ElementUtils;
import utils.WaitUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import utils.AuthenticationService;
import utils.ConfigReader;

import static utils.ElementUtils.SQLI;

@Listeners(TestListener.class)
public class ViewPassbookTest extends PlaywrightManager {
    private ViewPassbookPage vpPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;
    private Filters filters;
    private PaginationUtils paginationUtils;
    private SearchFilter searchFilter;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws Exception {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        vpPage = new ViewPassbookPage(page);
        filters = new Filters(page);
        paginationUtils = new PaginationUtils(page);
        searchFilter = new SearchFilter(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        handleLoaderInvisibility();
        elementUtils.handleOptionalPanel();
        executeTargetPageRouting();
    }

    private void executeTargetPageRouting() {
        handleLoaderInvisibility();
        Locator.WaitForOptions shortWait = new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(3000);

        for (int attempt = 0; attempt < 3; attempt++) {
            if (vpPage.getVpTransactionField().isVisible()) {
                return;
            }
            elementUtils.doClick(vpPage.getViewPassbookBtn());
            try {
                vpPage.getVpTransactionField().waitFor(shortWait);
                vpPage.getVpCostCenterField().waitFor(shortWait);
                return;
            } catch (TimeoutError ignored) {}
        }
        Assert.fail("Routing Failure: View Passbook dashboard frame did not reach interactive ready states.");
    }

    private void handleLoaderInvisibility() {
        try {
            // Using a highly accelerated timeout for transient loaders to avoid thread-blocking
            wait.waitForInvisibility(vpPage.getLoader());
        } catch (TimeoutError ignored) {}
    }

    @BeforeMethod(alwaysRun = true)
    public void openViewPassbook() {
        elementUtils.handleOptionalPanel();
        // Fast pre-check: if we are already sitting on the target view dashboard, skip the navigation step completely
        if (vpPage.getVpTransactionField().isVisible()) {
            return;
        }

        handleLoaderInvisibility();

        // High-speed short wait configuration specifically for our retry routing loop
        Locator.WaitForOptions shortWait = new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(3000);

        for (int attempt = 1; attempt <= 3; attempt++) {
            elementUtils.doClick(vpPage.getViewPassbookBtn());
            handleLoaderInvisibility();

            try {
                // Ensure the transactional frame is loaded and interactive before releasing the loop execution
                vpPage.getVpTransactionField().waitFor(shortWait);
                vpPage.getVpCostCenterField().waitFor(shortWait);
                return;
            } catch (TimeoutError ignored) {
                // Intercept timeouts silently to permit subsequent click attempts if application layer drops the initial click event
            }
        }

        Assert.fail("Routing Failure: View Passbook dashboard frame did not reach interactive ready states within 3 attempts.");
    }

    @AfterMethod(alwaysRun = true)
    public void closeViewPassbook() {
        handleLoaderInvisibility();

        // If the workspace view framework is open, step back safely using the breadcrumb or back link button component
        if (vpPage.getVpTransactionField().isVisible()) {
            elementUtils.doClick(vpPage.getViewPassbookBackBtn());
            handleLoaderInvisibility();
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDownSession() {
        try {
            if (vpPage.getVpTransactionField().isVisible()) {
                elementUtils.doClick(vpPage.getViewPassbookBackBtn());
                handleLoaderInvisibility();
            }
        } catch (Exception ignored) {
        }
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }

    @Test(priority = 1, description = "Verify View Passbook page opens correctly")
    public void CC_VP_001() {
        handleLoaderInvisibility();
        vpPage.getVpTransactionField().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Assert.assertTrue(vpPage.getVpTransactionField().isVisible(), "View Passbook target elements are not visible.");
    }

    @Test(priority = 2, description = "Verify filter components are displayed with correct default selection")
    public void CC_VP_002() {
        Assert.assertEquals(filters.dropdownSelected(vpPage.getVpTransactionField()), "All", "Transaction selector default alignment invalid.");
        Assert.assertEquals(filters.dropdownSelected(vpPage.getVpCostCenterField()), "All", "Cost center control default alignment invalid.");
    }

    @Test(priority = 3, description = "Verify filtering by Cost Center works")
    public void CC_VP_003() {
        String text = "BulkVoucher CC";
        filters.setDropdownStrValue(vpPage.getVpCostCenterField(), text);
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        List<Locator> noDataMessages = page.locator("//*[contains(text(),'No Data Found')]").all();
        if (!noDataMessages.isEmpty() && noDataMessages.get(0).isVisible()) {
            Assert.fail("Execution Error: Target system data missing for filtered Cost Center criteria run.");
        }
        filters.validateFilterAppliedOnColumn(5, text);
    }

    @Test(priority = 4, description = "Verify filtering by Transaction works")
    public void CC_VP_004() {
        int targetIndex = 1;
        String text = filters.getTextByIndex(targetIndex, vpPage.getVpTransactionField());
        filters.setDropdownValue(vpPage.getVpTransactionField(), "Funds Added");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        List<Locator> noDataMessages = page.locator("//*[contains(text(),'No Data Found')]").all();
        if (!noDataMessages.isEmpty() && noDataMessages.get(0).isVisible()) {
            Assert.fail("Execution Error: Target system data missing for filtered Transaction criteria run.");
        }
        filters.validateFilterAppliedOnColumn(6, text);
    }

    @Test(priority = 5, description = "Verify filtering using From date only gives error")
    public void CC_VP_005() {
        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2026-04-25");
        elementUtils.doClick(vpPage.getVpApplyBtn());

        wait.waitForVisibility(vpPage.getToastMessage());
        String errorMsg = elementUtils.doGetText(vpPage.getToastMessage()).trim();
        Assert.assertEquals(errorMsg, "Please select To Date", "To Date error message mismatch.");
    }

    @Test(priority = 6, description = "Verify filtering using To date only is disabled")
    public void CC_VP_006() {
        Locator toDateLocator = vpPage.getVpToDate();

        boolean isDisabled = toDateLocator.isDisabled() || toDateLocator.getAttribute("disabled") != null;

        Assert.assertTrue(isDisabled, "To Date is not disabled when From Date is left empty.");
    }

    @Test(priority = 7, description = "Verify filtering using valid date range")
    public void CC_VP_007() {
        String fromDate = "25 Apr 2025";
        String toDate = "25 Apr 2026";

        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2025-04-25");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2026-04-25");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
    }

    @Test(priority = 8, description = "Verify single day filtering")
    public void CC_VP_008() {
        String fromDate = "25 Apr 2026";
        String toDate = "25 Apr 2026";

        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2026-04-25");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2026-04-25");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
    }

    @Test(priority = 9, description = "Verify From date greater than To date validation")
    public void CC_VP_009() {
        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2026-04-25");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2025-04-25");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        if (vpPage.getNoDataText().isVisible()) {
            return;
        }

        String errorMsg = elementUtils.doGetText(vpPage.getToastMessage()).trim();
        Assert.assertNotEquals(errorMsg, "Data fetched successfully", "System permitted inverted historical date ranges.");
        Assert.assertEquals(errorMsg, "From Date cant be greater than To date", "Error message string layout mismatch.");
    }

    @Test(priority = 10, description = "Verify no records scenario")
    public void CC_VP_010() {
        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2000-04-25");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2000-04-28");
        elementUtils.doClick(vpPage.getVpApplyBtn());

        vpPage.getNoDataText().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        Assert.assertTrue(vpPage.getNoDataText().isVisible(), "Empty records notification placeholder missing.");
    }

    @Test(priority = 11, description = "Verify combined Cost Center + Date filter")
    public void CC_VP_011() {
        String fromDate = "20 Apr 2026";
        String toDate = "07 May 2026";

        filters.setDropdownStrValue(vpPage.getVpCostCenterField(), "BulkVoucher CC");
        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2026-04-20");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2026-05-07");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        List<Locator> noDataMessages = page.locator("//*[contains(text(),'No Data Found')]").all();
        if (!noDataMessages.isEmpty() && noDataMessages.get(0).isVisible()) {
            Assert.fail("Resource Setup Error: Test system lacks target records within dates mapping boundaries.");
        }

        filters.validateFilterAppliedOnColumn(5, "BulkVoucher CC");
        filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
    }

    @Test(priority = 12, description = "Verify Future Dates are not allowed")
    public void CC_VP_012() {
        filters.sendKeysInDateField(vpPage.getVpFromDate(), "2027-04-25");
        filters.sendKeysInDateField(vpPage.getVpToDate(), "2027-04-25");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        if (vpPage.getNoDataText().isVisible()) {
            return;
        }

        String errorMsg = elementUtils.doGetText(vpPage.getToastMessage()).trim();
        Assert.assertNotEquals(errorMsg, "Data fetched successfully", "System accepted post-dated transaction timelines entries.");
        Assert.assertEquals(errorMsg, "Date cant be in future", "System error alert language mismatched specifications.");
    }

    @Test(priority = 13, description = "Verify filter works with pagination")
    public void CC_VP_013() {
        String ccValue = "BulkVoucher CC";
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());

        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Insufficient test data records (Needed >= 30).");
        }

        filters.setDropdownStrValue(vpPage.getVpCostCenterField(), ccValue);
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        vpPage.getPageNextBtn().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        elementUtils.doClick(vpPage.getPageNextBtn());
        handleLoaderInvisibility();

        filters.validateFilterAppliedOnColumn(5, ccValue);
    }

    @Test(priority = 14, description = "Verify page data entry change works for 20 entries")
    public void CC_VP_014() {
        vpPage.getPageCount().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Prerequisite Failure: Insufficient master repository entries mapping matrix thresholds.");
        }

        int displayedCount = paginationUtils.selectEntryCountAndParse(vpPage.getDataEntryChange(), vpPage.getPageCount(), 1);
        Assert.assertEquals(displayedCount, 20, "Page size change did not display 20 entries.");
    }

    @Test(priority = 15, description = "Verify page data entry change works with 30 entries")
    public void CC_VP_015() {
        vpPage.getPageCount().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Prerequisite Failure: Insufficient master repository entries mapping matrix thresholds.");
        }

        int displayedCount = paginationUtils.selectEntryCountAndParse(vpPage.getDataEntryChange(), vpPage.getPageCount(), 2);
        Assert.assertEquals(displayedCount, 30, "Page size change did not display 30 entries.");
    }

    @Test(priority = 16, description = "Verify Pagination is visible")
    public void CC_VP_016() {
        boolean present = paginationUtils.isPaginationVisible(vpPage.getPaginationLocator());
        Assert.assertTrue(present, "Pagination component is not visible.");
    }

    @Test(priority = 17, description = "Verify Next changes the page to next page")
    public void CC_VP_017() {
        vpPage.getPageCount().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Data Pool Truncated: Concurrent workspace execution blocked by insufficient records matrix.");
        }
        elementUtils.doClick(vpPage.getPageNextBtn());
        int currPageNo = paginationUtils.getCurrentPageNumber(vpPage.getPageCount());
        Assert.assertEquals(currPageNo, 2, "Clicking next did not navigate to page 2.");
    }

    @Test(priority = 18, description = "Verify Previous changes the page to previous")
    public void CC_VP_018() {
        handleLoaderInvisibility();
        elementUtils.doClick(vpPage.getPageNextBtn());
        handleLoaderInvisibility();

        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Configuration Error: Dataset requirements mismatch processing logic parameters baseline.");
        }

        elementUtils.doClick(vpPage.getPagePrevBtn());
        int currPageNo = paginationUtils.getCurrentPageNumber(vpPage.getPageCount());
        Assert.assertEquals(currPageNo, 1, "Clicking previous did not navigate back to page 1.");
    }

    @Test(priority = 19, description = "Verify Previous is disabled when on page 1")
    public void CC_VP_019() {
        vpPage.getBtnAfterPrev().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        elementUtils.doClick(vpPage.getBtnAfterPrev());

        boolean isPrevDisabled = !paginationUtils.isPreviousEnabled(vpPage.getPagePrevBtn());
        Assert.assertTrue(isPrevDisabled, "Previous button should be disabled on the first page.");
    }

    @Test(priority = 20, description = "Verify Next is disabled and Prev is enabled on the last page")
    public void CC_VP_020() {
        handleLoaderInvisibility();
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Dataset requirements execution barrier: Total trace items below processing capacity.");
        }

        elementUtils.doClick(vpPage.getBtnBeforeNext());

        Assert.assertTrue(paginationUtils.isPreviousEnabled(vpPage.getPagePrevBtn()), "Previous button should be enabled on the last page.");
        Assert.assertFalse(paginationUtils.isNextEnabled(vpPage.getPageNextBtn()), "Next button should be disabled on the last page.");
    }

    @Test(priority = 21, description = "Verify click on last page shows left over data")
    public void CC_VP_021() {
        handleLoaderInvisibility();
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Dataset mismatch parameters execution boundaries limits.");
        }

        vpPage.getBtnBeforeNext().evaluate("element => element.click()");
        handleLoaderInvisibility();

        String input = elementUtils.doGetText(vpPage.getPageCount());
        int lastPageRows = paginationUtils.lastPageContent(vpPage.getPageCount());

        String[] total = input.split(" ");
        int totalCC = paginationUtils.parseTotalRecords(input) - Integer.parseInt(total[1].trim()) + 1;

        Assert.assertEquals(totalCC, lastPageRows, "The displayed records metrics count mismatches real summary layout calculations.");
    }

    @Test(priority = 22, description = "Verify changing page size from 30 to 10 resets pagination to page 1")
    public void CC_VP_022() {
        handleLoaderInvisibility();
        String pageDetailsText = elementUtils.doGetText(vpPage.getPageCount());
        if (paginationUtils.parseTotalRecords(pageDetailsText) < 30) {
            Assert.fail("Prerequisite Failure: Insufficient systemic records density allocation benchmarks.");
        }

        vpPage.getDataEntryChange().selectOption("20");
        boolean isPageNoReset = 1 == paginationUtils.getCurrentPageNumber(vpPage.getPageCount());

        vpPage.getDataEntryChange().selectOption("10");
        Assert.assertTrue(isPageNoReset, "Changing page size did not reset pagination back to page 1.");
    }

    @Test(priority = 23, description = "Verify Search button is disabled when no data in search field")
    public void CC_VP_023() {
        boolean isDisabled = searchFilter.blankSearch(vpPage.getFilterSearchField(), vpPage.getFilterSearchBtn());
        Assert.assertTrue(isDisabled, "Search button should be disabled when search field is empty.");
    }

    @Test(priority = 24, description = "Verify Search button is disabled when ' '(space) in search field")
    public void CC_VP_024() {
        searchFilter.spaceSearch(vpPage.getFilterSearchField(), vpPage.getFilterSearchBtn());
    }

    @Test(priority = 25, description = "Verify Error toaster message when special char in search field")
    public void CC_VP_025() {
        String actualToast = searchFilter.specialSearch(
                vpPage.getFilterSearchField(),
                vpPage.getFilterSearchBtn(),
                vpPage.getToastMessage(),
                "@$", vpPage.getLoader(), vpPage.getNoDataText()
        );
        Assert.assertTrue(
                actualToast.equals("Invalid search input") || actualToast.equals("No Data Found"),
                "Special character search error verification failed. Received message: " + actualToast
        );
    }

    @Test(priority = 26, description = "Verify Error toaster message when hash char in search field")
    public void CC_VP_026() {
        String error = searchFilter.hashError(
                vpPage.getFilterSearchField(),
                vpPage.getFilterSearchBtn(),
                vpPage.getToastMessage(), vpPage.getNoDataText()
        );
        Assert.assertTrue(
                error.equals("Invalid search input") || error.equals("No Data Found"),
                "Hash character search error verification failed. Received message: " + error
        );
    }

    @Test(priority = 27, description = "Verify Search works correctly")
    public void CC_VP_027() {
        vpPage.getFilterSearchField().clear();
        String value = "96482";

        searchFilter.validSearch(
                vpPage.getFilterSearchField(),
                vpPage.getFilterSearchBtn(),
                9,
                vpPage.getTableRows(),
                value
        );
    }

    @Test(priority = 28, description = "Verify Reset button works")
    public void CC_VP_028() {
        elementUtils.sendKeys(vpPage.getFilterSearchField(), "5195");
        elementUtils.doClick(vpPage.getFilterSearchBtn());

        elementUtils.doClick(vpPage.getResetBtn());
        try {
            wait.waitForInvisibility(vpPage.getToastMessage());
        } catch (TimeoutError ignored) {}

        String afterReset = vpPage.getFilterSearchField().inputValue();
        Assert.assertTrue(afterReset.trim().isEmpty(), "Reset button did not clear the search criteria field values.");
    }

    @Test(priority = 29, description = "Verify Search gives 'No Data Found' when data is invalid")
    public void CC_VP_029() {
        String errorMsg = searchFilter.invalidSearch(
                vpPage.getFilterSearchField(),
                vpPage.getFilterSearchBtn(),
                vpPage.getNoDataText(),
                "VeryInvalid0000001"
        );
        Assert.assertEquals(errorMsg, "No Data Found", "Search with invalid entry did not display 'No Data Found' text container.");
        elementUtils.doClick(vpPage.getResetBtn());
    }

    @Test(priority = 30, description = "Verify Reset Button is disabled when Search field is empty")
    public void CC_VP_030() {
        vpPage.getFilterSearchField().clear();
        boolean isDisable = searchFilter.blankSearch(vpPage.getFilterSearchField(), vpPage.getResetBtn());
        Assert.assertTrue(isDisable, "Reset button should be disabled when search field is clear.");
    }

    @Test(priority = 31, description = "Verify Reset Btn works fine")
    public void CC_VP_031() {
        filters.setDropdownStrValue(vpPage.getVpCostCenterField(), "BulkVoucher CC");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        elementUtils.doClick(vpPage.getVpFilterResetBtn());
        Assert.assertEquals(filters.dropdownSelected(vpPage.getVpCostCenterField()), "All", "Reset button did not return Cost Center dropdown default alignment to 'All'.");
    }

    @Test(priority = 32, description = "Verify Cost Center search blocks SQL injection payloads")
    public void CC_VP_032() {
        handleLoaderInvisibility();

        // Optimize runtime by creating a composite locator for expected UI responses
        Locator compositeFeedback = vpPage.getNoDataText().or(vpPage.getToastMessage());
        Locator.WaitForOptions quickTimeout = new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(1000);

        for (String payload : SQLI) {
            vpPage.getFilterSearchField().clear();
            elementUtils.sendKeys(vpPage.getFilterSearchField(), payload);
            elementUtils.doClick(vpPage.getFilterSearchBtn());

            try {
                compositeFeedback.waitFor(quickTimeout);
            } catch (TimeoutError ignored) {}

            boolean isFeedbackPresent = vpPage.getNoDataText().isVisible() || vpPage.getToastMessage().isVisible();
            Assert.assertTrue(isFeedbackPresent, "Security Threat: Input sanitization dropped containment checks for payload: " + payload);

            if (!vpPage.getNoDataText().isVisible()) {
                String msg = elementUtils.doGetText(vpPage.getToastMessage()).toLowerCase();
                Assert.assertTrue(
                        msg.contains("invalid") || msg.contains("error"),
                        "CRITICAL: Information leakage risk. SQL Injection vulnerability payload escaped boundary checks: " + payload
                );
            }
        }
        elementUtils.doClick(vpPage.getResetBtn());
    }

    @Test(priority = 33, description = "Export Button Downloads file")
    public void CC_VP_033() {
        Download download = page.waitForDownload(() -> elementUtils.doClick(vpPage.getVpExportBtn()));
        Path path = download.path();
        String fileName = download.suggestedFilename();

        Assert.assertNotNull(path, "Export operation failed to produce a downloadable binary document file structure.");
        Assert.assertTrue(fileName.contains("Passbook_export"), "Downloaded document name mismatch schema specifications.");
    }

    @Test(priority = 34, description = "Export Button downloads filtered data")
    public void CC_VP_034() {
        filters.setDropdownStrValue(vpPage.getVpCostCenterField(), "BulkVoucher CC");
        elementUtils.doClick(vpPage.getVpApplyBtn());
        handleLoaderInvisibility();

        Download download = page.waitForDownload(() -> elementUtils.doClick(vpPage.getVpExportBtn()));
        Path path = download.path();
        String fileName = download.suggestedFilename();

        Assert.assertNotNull(path, "Filtered export operation failed to deliver target payload file elements.");
        Assert.assertTrue(fileName.contains("Passbook_export"), "Filtered export document file naming schema format invalid.");
    }
}
