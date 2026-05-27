package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import page.RechargeHistoryPage;
import reusableComponents.Filters;
import reusableComponents.PaginationUtils;
import reusableComponents.SearchFilter;
import utils.ElementUtils;
import utils.WaitUtils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;
import utils.AuthenticationService;
import utils.ConfigReader;

import static utils.ElementUtils.SQLI;

@Listeners(TestListener.class)
public class RechargeHistoryTest extends PlaywrightManager {

    private RechargeHistoryPage rhPage;
    private ElementUtils elementUtils;
    private Filters filters;
    private PaginationUtils pageUtils;
    private SearchFilter searchFilter;
    private WaitUtils wait;
    private static final int DEFAULT_PAGE_SIZE = 10;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        rhPage = new RechargeHistoryPage(page);
        filters = new Filters(page);
        pageUtils = new PaginationUtils(page);
        searchFilter = new SearchFilter(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];

        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));

        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        elementUtils.handleOptionalPanel();

        try {
            wait.waitForInvisibility(rhPage.getLoader());
        } catch (TimeoutError ignored) {
        }

        elementUtils.doClick(rhPage.getRechargeHistoryBtn());

        try {
            wait.waitForInvisibility(rhPage.getLoader());
        } catch (TimeoutError ignored) {
        }
    }

    @AfterClass(alwaysRun = true)
    public void closeSessionContext() {
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void openRechargeHistory() {
        elementUtils.handleOptionalPanel();
        try {
            wait.waitForInvisibility(rhPage.getLoader());

            if (rhPage != null && !rhPage.getRechargeHistoryBackBtn().isVisible()) {
                elementUtils.doClick(rhPage.getRechargeHistoryBtn());
                wait.waitForInvisibility(rhPage.getLoader());
            }
        } catch (Exception ignored) {
        }
    }

    @AfterMethod(alwaysRun = true)
    public void closeRechargeHistory() {
        try {
            wait.waitForInvisibility(rhPage.getLoader());

            if (rhPage != null && rhPage.getRechargeHistoryBackBtn().isVisible()) {
                elementUtils.doClick(rhPage.getRechargeHistoryBackBtn());
                wait.waitForInvisibility(rhPage.getLoader());
            }
        } catch (Exception ignored) {
        }
    }

    private void waitUntilExportEnabledOrSkip() {
        try {
            wait.waitForInvisibility(rhPage.getLoader());
        } catch (TimeoutError ignored) {
        }

        Locator exportBtn = rhPage.getExportBtn();
        exportBtn.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(60000));

        for (int i = 0; i < 120; i++) {
            try {
                wait.waitForInvisibility(rhPage.getLoader());
            } catch (TimeoutError ignored) {
            }

            if (exportBtn.isEnabled()) {
                return;
            }

            page.waitForTimeout(500);
        }

        throw new SkipException("Export control stayed disabled (no exportable recharge rows in this environment).");
    }

    @Test(priority = 1, description = "Verify Recharge History page opens correctly")
    public void CC_RH_001() {
        wait.waitForInvisibility(rhPage.getLoader());

        Assert.assertTrue(
                rhPage.getRechargeHistoryBackBtn().isVisible(),
                "Recharge History page did not open."
        );
    }

    @Test(priority = 2, description = "Verify filter components are displayed")
    public void CC_RH_002() {
        Assert.assertEquals(filters.dropdownSelected(rhPage.getRechargeTypeField()), "All", "Filter Dropdown baseline mapping incorrect.");
    }

    @Test(priority = 3, description = "Verify filtering by Recharge Type")
    public void CC_RH_003() {
        for (int i = 1; i < 4; i++) {
            String text = filters.getTextByIndex(i, rhPage.getRechargeTypeField());
            filters.setDropdownValue(rhPage.getRechargeTypeField(), String.valueOf(i));
            elementUtils.doClick(rhPage.getFilterApplyBtn());
            wait.waitForInvisibility(rhPage.getLoader());

            if (rhPage.getNoDataText().isVisible()) {
                System.out.println("No transactional entry variations found for subset criteria matching: " + text);
                continue;
            }
            wait.waitForInvisibility(rhPage.getLoader());
            filters.validateFilterAppliedOnColumn(6, text);
        }
    }

    @Test(priority = 4, description = "Verify filtering using From date only gives error")
    public void CC_RH_004() {
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2026-04-25");
        elementUtils.doClick(rhPage.getFilterApplyBtn());

        wait.waitForVisibility(rhPage.getToastMessage());
        String errorMsg = elementUtils.doGetText(rhPage.getToastMessage()).trim();
        Assert.assertEquals(errorMsg, "Please select To Date", "Toast validation feedback text mismatched for single-ended boundary inputs.");
    }

    @Test(priority = 5, description = "Verify filtering using To date only is disabled")
    public void CC_RH_005() {
        boolean isDisabled = rhPage.getToDateFilter().isDisabled();
        Assert.assertTrue(isDisabled, "Data Integrity Fault: To-Date control exposed before active chronological start anchor provided.");
    }

    @Test(priority = 6, description = "Verify filtering using valid date range")
    public void CC_RH_006() {
        String fromDate = "25 Apr 2025";
        String toDate = "25 Apr 2026";

        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2025-04-25");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2026-04-25");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
    }

    @Test(priority = 7, description = "Verify single day filtering")
    public void CC_RH_007() {
        String fromDate = "25 Apr 2026";
        String toDate = "25 Apr 2026";

        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2026-04-25");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2026-04-25");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
    }

    @Test(priority = 8, description = "Verify From date greater than To date validation")
    public void CC_RH_008() {
        // 1. Establish the inverted chronological state
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2026-04-25");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2025-04-25");
        elementUtils.doClick(rhPage.getFilterApplyBtn());

        try {
            // 2. Wait explicitly for the mandatory validation toaster
            rhPage.getToastMessage().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000)); // 5 seconds is plenty for a local client validation check

            String errorMsg = elementUtils.doGetText(rhPage.getToastMessage()).trim();

            // Assert that the success state was NOT reached
            if (errorMsg.equalsIgnoreCase("Data fetched successfully")) {
                Assert.fail("CRITICAL BUG: Application allowed data fetching even though 'From Date' is greater than 'To Date'.");
            }

            // 3. Verify the validation text quality
            String lower = errorMsg.toLowerCase();
            Assert.assertTrue(
                    lower.contains("greater") && lower.contains("date"),
                    "Validation error displayed but lacked structural clarity. Got: " + errorMsg);

        } catch (TimeoutError e) {
            // 4. REMOVED THE SILENT RETURN.
            // If the toast doesn't appear, the interface failed to validate the input. Period.
            Assert.fail("TEST FAILED: UI failed to display a validation error toaster within 5 seconds when dates were inverted.");
        }
    }

    @Test(priority = 9, description = "Verify no records scenario")
    public void CC_RH_009() {
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2000-04-25");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2000-04-28");
        elementUtils.doClick(rhPage.getFilterApplyBtn());

        wait.waitForVisibility(rhPage.getNoDataText());
        Assert.assertTrue(rhPage.getNoDataText().isVisible(), "Empty query criteria results message container absent.");
    }

    @Test(priority = 10, description = "Verify combined Recharge Type + Date filter")
    public void CC_RH_010() {
        String fromDate = "20 Apr 2026";
        String toDate = "22 Apr 2026";

        filters.setDropdownValue(rhPage.getRechargeTypeField(), "1");
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2026-03-20");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2026-04-28");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        if (rhPage.getNoDataText().isVisible()) {
            Assert.fail("Test data set absent: execution context requires population matrix variations for target evaluation steps.");
        } else {
            filters.validateFilterAppliedOnColumn(6, "Recharge");
            filters.verifyDateFilter(fromDate, toDate, page.locator("//table/tbody/tr").first(), 2);
        }
    }

    @Test(priority = 11, description = "Verify Reset clears all filters")
    public void CC_RH_011() {
        filters.setDropdownValue(rhPage.getRechargeTypeField(), "1");
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2026-04-20");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2026-04-22");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        elementUtils.doClick(rhPage.getFilterResetBtn());
        Assert.assertEquals(filters.dropdownSelected(rhPage.getRechargeTypeField()), "All", "Form state parameters persistence failure after reset operation.");
    }

    @Test(priority = 12, description = "Verify future date handling")
    public void CC_RH_012() {
        filters.sendKeysInDateField(rhPage.getFromDateFilter(), "2027-04-25");
        filters.sendKeysInDateField(rhPage.getToDateFilter(), "2027-04-25");
        elementUtils.doClick(rhPage.getFilterApplyBtn());

        try {
            rhPage.getToastMessage().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
            String errorMsg = elementUtils.doGetText(rhPage.getToastMessage()).trim();

            if (errorMsg.equals("Data fetched successfully")) {
                Assert.fail("TEST FAILED: Validation for 'Date in Future' passed backend criteria layers unexpectedly.");
            }
            String lower = errorMsg.toLowerCase();
            Assert.assertTrue(lower.contains("future") || lower.contains("future date"),
                    "UI Error Message payload mismatch. Got: " + errorMsg);
        } catch (TimeoutError e) {
            if (rhPage.getNoDataText().isVisible() || rhPage.getTableRows().count() == 0) {
                return;
            }
            Assert.fail("TEST FAILED: Unhandled boundary breach: No structural error context toaster returned for entries in future tracking timelines.");
        }
    }

    @Test(priority = 13, description = "Verify filter works with pagination")
    public void CC_RH_013() {
        elementUtils.doClick(rhPage.getFilterResetBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        String pageCountText = rhPage.getPageCount().innerText().trim();
        System.out.println(pageCountText);// "Showing 1 to 10 of 31"
        String pageDetailsText = pageCountText.split("of")[1].trim();   // "31"
        System.out.println(pageDetailsText);

        // FIX: Use native Java parsing instead of the broken utility method
        int totalRecords = Integer.parseInt(pageDetailsText);

        if (totalRecords < 30) {
            throw new SkipException("Context criteria dropped: Underpopulated historical records grid structure cannot validate pagination shifts.");
        }

        filters.setDropdownValue(rhPage.getRechargeTypeField(), "Recharge");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        // Clean execution path without the dangerous try-catch mask
        elementUtils.doClick(rhPage.getPageNextBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        filters.validateFilterAppliedOnColumn(6, "Recharge");
        Assert.assertEquals(filters.dropdownSelected(rhPage.getRechargeTypeField()), "Recharge",
                "The selected dropdown option reset after changing pages.");
    }

    @Test(priority = 14, description = "Verify page data entry change works for 20 entries")
    public void CC_RH_014() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Underpopulated layout structure prevents target framework sizing shifts checking iterations.");
        }
        int displayedCount = pageUtils.selectEntryCountAndParse(rhPage.getDataEntryChange(), rhPage.getPageCount(), 1);
        Assert.assertEquals(displayedCount, 20, "Table sizing modification profile parameters trace configuration adjustments sequence mismatch.");
    }

    @Test(priority = 15, description = "Verify page data entry change works with 30 entries")
    public void CC_RH_015() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Insufficent record depth structural models bounds.");
        }
        int displayedCount = pageUtils.selectEntryCountAndParse(rhPage.getDataEntryChange(), rhPage.getPageCount(), 2);
        Assert.assertEquals(displayedCount, 30, "Sizing adjustment processing boundary rule criteria array match mismatch execution traces.");
    }

    @Test(priority = 16, description = "Verify Pagination is visible")
    public void CC_RH_016() {
        boolean present = pageUtils.isPaginationVisible(rhPage.getPaginationLocator());
        Assert.assertTrue(present, "Functional component structure mismatch: grid navigation layout tracking arrays hidden.");
    }

    @Test(priority = 17, description = "Verify Next changes the page to next page")
    public void CC_RH_017() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Record length metric parameters dropped pagination indexing steps loops checks validation iterations.");
        }
        elementUtils.doClick(rhPage.getPageNextBtn());
        int currPageNo = pageUtils.getCurrentPageNumber(rhPage.getPageCount());
        Assert.assertEquals(currPageNo, 2, "Action routing failed: operational step sequence left current tracking pointer on baseline page indexes.");
    }

    @Test(priority = 18, description = "Verify Previous changes the page to previous")
    public void CC_RH_018() {
        elementUtils.doClick(rhPage.getPageNextBtn());
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Query depth limitation constraints tracking matrix patterns drops checks.");
        }
        elementUtils.doClick(rhPage.getPagePrevBtn());
        int currPageNo = pageUtils.getCurrentPageNumber(rhPage.getPageCount());
        Assert.assertEquals(currPageNo, 1, "Backtrack control routing component malfunction: execution pointer stuck on historical page layers arrays.");
    }

    @Test(priority = 19, description = "Verify Previous is disabled when on page 1")
    public void CC_RH_019() {
        elementUtils.doClick(rhPage.getBtnAfterPrev());
        boolean isPrevDisabled = !pageUtils.isPreviousEnabled(rhPage.getPagePrevBtn());
        Assert.assertTrue(isPrevDisabled, "Navigation failure: core validation bounds permits inverse page indexing under baseline coordinates values.");
    }

    @Test(priority = 20, description = "Verify Next is disabled and Prev is enabled on the last page")
    public void CC_RH_020() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Test requirements boundary unmet: historical logs parameters matrix count bounds limits execution patterns.");
        }

        int totalRecords = pageUtils.parseTotalRecords(elementUtils.doGetText(rhPage.getPageCount()).trim());
        elementUtils.doClick(rhPage.getBtnBeforeNext());

        Assert.assertTrue(pageUtils.isPreviousEnabled(rhPage.getPagePrevBtn()), "Inverted boundary execution context drop tracking limits parameters.");
        Assert.assertFalse(pageUtils.isNextEnabled(rhPage.getPageNextBtn()), "Forward contextual routing links active despite index matching tail terminal data thresholds.");
    }

    @Test(priority = 21, description = "Verify click on last page shows left over data")
    public void CC_RH_021() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Structural data depths limitations dropped evaluation validation targets execution loops tracing checks.");
        }

        elementUtils.doClick(rhPage.getBtnBeforeNext());
        wait.waitForInvisibility(rhPage.getLoader());

        String input = elementUtils.doGetText(rhPage.getPageCount());
        int rowsFromPaginationRange = pageUtils.lastPageContent(rhPage.getPageCount());
        int rowsInTable = rhPage.getTableRows().count();

        Assert.assertEquals(
                rowsInTable,
                rowsFromPaginationRange,
                "Last-page grid row count should match pagination range (" + input + ").");
    }

    @Test(priority = 22, description = "Verify changing page size from 30 to 10 resets pagination to page 1")
    public void CC_RH_022() {
        String pageDetailsText = rhPage.getPageCount().innerText().trim();
        if (pageUtils.parseTotalRecords(pageDetailsText) < 30) {
            throw new SkipException("Matrix populations dropped checks validations workflows configuration loops logs profiles steps.");
        }

        rhPage.getDataEntryChange().selectOption(new SelectOption().setValue("20"));
        boolean isPageNoReset = 1 == pageUtils.getCurrentPageNumber(rhPage.getPageCount());
        rhPage.getDataEntryChange().selectOption(new SelectOption().setValue("10"));

        Assert.assertTrue(isPageNoReset, "Layout transformation dynamic parameter mapping dropped active page assignment configurations index indicators back into foundational arrays.");
    }

    @Test(priority = 23, description = "Verify Search button is disabled when no data in search field")
    public void CC_RH_023() {
        boolean isDisabled = searchFilter.blankSearch(rhPage.getFilterSearchField(), rhPage.getFilterSearchBtn());
        Assert.assertTrue(isDisabled, "Operational button components structure enabled logic validations tracking with empty input payload configurations context steps.");
    }

    @Test(priority = 24, description = "Verify Search button is disabled when ' '(space) in search field")
    public void CC_RH_024() {
        searchFilter.spaceSearch(rhPage.getFilterSearchField(), rhPage.getFilterSearchBtn());
    }

    @Test(priority = 25, description = "Verify Error toaster message when special char in search field")
    public void CC_RH_025() {
        try {
            String actualToast = searchFilter.specialSearch(
                    rhPage.getFilterSearchField(),
                    rhPage.getFilterSearchBtn(),
                    rhPage.getToastMessage(),
                    "@$",rhPage.getLoader(),rhPage.getNoDataText()
            );
            Assert.assertTrue(
                    "Invalid search input".equalsIgnoreCase(actualToast.trim())
                            || "No Data Found".equalsIgnoreCase(actualToast.trim()),
                    "Expected invalid-input toast or empty-state message; got: " + actualToast);
        } catch (TimeoutError e) {
            Assert.fail("Validation intercept tracing: system component dropped dynamic notification error triggers before timeline timeout limit threshold values reached.");
        }
    }

    @Test(priority = 26, description = "Verify Error toaster message when hash char in search field")
    public void CC_RH_026() {
        try {
            String error = searchFilter.hashError(
                    rhPage.getFilterSearchField(),
                    rhPage.getFilterSearchBtn(),
                    rhPage.getToastMessage(),
                    rhPage.getNoDataText()
            );
            Assert.assertTrue(
                    "Invalid search input".equalsIgnoreCase(error.trim())
                            || "No Data Found".equalsIgnoreCase(error.trim()),
                    "Expected invalid-input toast or empty-state message; got: " + error);
        } catch (TimeoutError e) {
            Assert.fail("The internal monitoring layers missed alert state initialization tracking criteria metrics benchmarks patterns workflows.");
        }
    }

    @Test(priority = 27, description = "Verify Search works correctly")
    public void CC_RH_027() {
        rhPage.getFilterSearchField().clear();
        wait.waitForInvisibility(rhPage.getLoader());

        List<Locator> seedRows = rhPage.getTableRows().all();
        Assert.assertFalse(seedRows.isEmpty(), "Need at least one recharge row to derive a search term.");

        List<Locator> seedCells = seedRows.get(0).locator("td").all();
        Assert.assertTrue(seedCells.size() > 11, "Table column layout mismatch for search column index.");

        String value = seedCells.get(11).innerText().trim();
        Assert.assertFalse(value.isEmpty(), "Search column cell is empty.");

        searchFilter.validSearch(
                rhPage.getFilterSearchField(),
                rhPage.getFilterSearchBtn(), 11,
                rhPage.getTableRows(),
                value
        );
    }

    @Test(priority = 28, description = "Verify Reset button works")
    public void CC_RH_028() {
        elementUtils.sendKeys(rhPage.getFilterSearchField(), "5195");
        elementUtils.doClick(rhPage.getFilterSearchBtn());

        elementUtils.doClick(rhPage.getResetBtn());
        try {
            wait.waitForInvisibility(rhPage.getToastMessage());
        } catch (TimeoutError ignored) {}

        String afterReset = rhPage.getFilterSearchField().inputValue();
        Assert.assertTrue(afterReset == null || afterReset.trim().isEmpty(),
                "Clear fields components structure processing routine failed parsing input tracking data mapping resets properties.");
    }

    @Test(priority = 29, description = "Verify Search gives 'No Data Found' when data is invalid")
    public void CC_RH_029() {
        try {
            String errorMsg = searchFilter.invalidSearch(
                    rhPage.getFilterSearchField(),
                    rhPage.getFilterSearchBtn(),
                    rhPage.getNoDataText(),
                    "VeryInvalid0000001"
            );
            Assert.assertEquals(errorMsg, "No Data Found", "Data listing grids structural fallback failure message details string matching patterns mismatch execution states.");
        } catch (TimeoutError e) {
            Assert.fail("Interface validation error placeholder indicators tracking failed structural layout rendering metrics arrays triggers checking paths.");
        }
        elementUtils.doClick(rhPage.getResetBtn());
    }

    @Test(priority = 30, description = "Verify Reset Button is disabled when Search field is empty")
    public void CC_RH_030() {
        rhPage.getFilterSearchField().clear();
        boolean isDisable = searchFilter.blankSearch(rhPage.getFilterSearchField(), rhPage.getResetBtn());
        Assert.assertTrue(isDisable, "Action control interface optimization error: empty variables context permitted state executions triggers checks rules paths properties reset parameters.");
    }

    @Test(priority = 31, description = "Verify Recharge History document column download works")
    public void CC_RH_031() {
        Locator pdfDoc = page.locator("//table//tr//img[contains(@src,'pdficon')]");
        wait.waitForInvisibility(rhPage.getLoader());

        if (pdfDoc.count() > 0) {
            // Safe, deterministic parallel-ready Playwright download trapping model strategy
            Download download = page.waitForDownload(() -> {
                pdfDoc.first().click();
            });
            Assert.assertTrue(download.path().toString().length() > 0, "Resource transmission protocol failed: Binary data mapping payload dropped completely during transport workflows context pipeline layers.");
        } else {
            throw new SkipException("Context drop checking criteria steps: target population models profiles records missing dynamic binary links fields.");
        }
    }

    @Test(priority = 32, description = "Verify Cost Center search blocks SQL injection payloads")
    public void CC_RH_032() {
        // SoftAssert allows the loop to continue running even if a payload fails an assertion or times out
        SoftAssert softAssert = new SoftAssert();

        for (String payload : SQLI) {
            System.out.println("Current Testing Payload: ===> " + payload);

            try {
                // 1. Ensure the UI is clean before typing the payload
                rhPage.getFilterSearchField().clear();
                rhPage.getLoader().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(3000));

                // 2. Inject and execute the payload
                elementUtils.sendKeys(rhPage.getFilterSearchField(), payload);
                elementUtils.doClick(rhPage.getFilterSearchBtn());

                // 3. Wait for the loader to clear with a dedicated timeout limit
                rhPage.getLoader().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(5000));

                // 4. Check for toast messages and validate them
                if (rhPage.getToastMessage().isVisible()) {
                    String msg = elementUtils.doGetText(rhPage.getToastMessage()).toLowerCase().trim();

                    // Assert condition safely recorded into the softAssert object
                    softAssert.assertTrue(
                            msg.contains("invalid") || msg.contains("error") || msg.contains("unsafe"),
                            "[TOAST FAILURE] Payload: [" + payload + "] triggered an unhandled response message: \"" + msg + "\""
                    );
                    System.out.println("Toast Captured for Payload [" + payload + "]: " + msg);
                }

            } catch (TimeoutError e) {
                // 5. Catch hanging loaders / delay issues specifically
                String timeoutErrorMessage = "[DELAY FAILURE] Application hung/timed out (5000ms exceeded) on Payload: [" + payload + "]. Possible Time-Based SQLi or unhandled server crash.";

                softAssert.fail(timeoutErrorMessage);
                System.err.println(timeoutErrorMessage);

                // Administrative UI recovery: Reload the page so the next payload isn't blocked by a stuck loader
                page.reload();
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);

            } finally {
                // 6. Attempt a reset clean-up sequence for the next loop iteration
                try {
                    rhPage.getLoader().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(2000));
                    if (rhPage.getResetBtn().isVisible()) {
                        elementUtils.doClick(rhPage.getResetBtn());
                    }
                } catch (Exception ignored) {
                    // If reset fails because the page state is corrupt, the next iteration's fresh clear/reload steps will handle it
                }
            }
        }

        // CRITICAL: This collates all intercepted failures (Toasts & Timeouts) and publishes them to the TestNG report
        softAssert.assertAll();
    }


    @Test(priority = 33, description = "Export Button Downloads file")
    public void CC_RH_033() {
        waitUntilExportEnabledOrSkip();

        Download download = page.waitForDownload(() -> elementUtils.doClick(rhPage.getExportBtn()));

        String fileName = download.suggestedFilename();
        Assert.assertTrue(
                fileName.toLowerCase().contains("recharge_history_export") || fileName.toLowerCase().contains("recharge"),
                "Data management report system delivery verification breakdown: dynamic file signature structural matching name patterns missing identifiers labels metadata values configurations checks tracks. Found: " + fileName
        );
    }

    @Test(priority = 34, description = "Export Button downloads filtered data")
    public void CC_RH_034() throws Exception {
        String selectedRechargeType = filters.getTextByIndex(1, rhPage.getRechargeTypeField());
        filters.setDropdownValue(rhPage.getRechargeTypeField(), "1");
        elementUtils.doClick(rhPage.getFilterApplyBtn());
        wait.waitForInvisibility(rhPage.getLoader());

        Assert.assertFalse(
                rhPage.getNoDataText().isVisible(),
                "No Recharge History rows available for recharge type: " + selectedRechargeType
        );
        filters.validateFilterAppliedOnColumn(6, selectedRechargeType);

        waitUntilExportEnabledOrSkip();

        Download download = page.waitForDownload(() -> {
            elementUtils.doClick(rhPage.getExportBtn());
        });

        Path path = download.path();
        Assert.assertNotNull(path, "Filtered Recharge History export payload transmission failed across execution tunnel.");

        File exportFile = path.toFile();
        assertExcelColumnEquals(exportFile, selectedRechargeType);
    }

    private void assertExcelColumnEquals(File exportFile, String expectedValue) throws Exception {
        org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
        int checkedRows = 0;

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(exportFile)) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int columnIndex = findExportRechargeTypeColumn(sheet, formatter);

            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue; // Skip header schema row
                }

                org.apache.poi.ss.usermodel.Cell cell = row.getCell(columnIndex, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) {
                    continue;
                }

                String actualValue = formatter.formatCellValue(cell).trim();
                if (actualValue.isBlank()) {
                    continue;
                }

                checkedRows++;
                Assert.assertEquals(
                        actualValue,
                        expectedValue,
                        "Data Integrity Error: Downloaded document data does not align with criteria filter at row row index sequence: " + (row.getRowNum() + 1)
                );
            }
        }

        Assert.assertTrue(
                checkedRows > 0,
                "Structural Parsing Failure: Downloaded report package does not contain data items rows underneath targeted column header models."
        );
    }

    private int findExportRechargeTypeColumn(org.apache.poi.ss.usermodel.Sheet sheet, org.apache.poi.ss.usermodel.DataFormatter formatter) {
        org.apache.poi.ss.usermodel.Row header = sheet.getRow(0);
        Assert.assertNotNull(header, "Schema Error: Extracted report structure completely lacks functional header rows configuration parameters.");

        for (org.apache.poi.ss.usermodel.Cell cell : header) {
            String value = formatter.formatCellValue(cell).trim();
            if (value.equalsIgnoreCase("Transaction Type") || value.equalsIgnoreCase("Recharge Type")) {
                return cell.getColumnIndex();
            }
        }

        Assert.fail("Schema Mapping Exception: Targeted identifier labels [Transaction Type / Recharge Type] are missing inside parsed metadata payload boundaries.");
        return -1;
    }
}
