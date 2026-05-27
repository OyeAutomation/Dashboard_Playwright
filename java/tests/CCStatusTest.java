package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.AddFundsPage;
import page.CCStatusPage;
import page.DashboardPage;
import utils.ElementUtils;
import utils.WaitUtils;

import java.sql.SQLException;
import utils.AuthenticationService;
import utils.ConfigReader;

@Listeners(TestListener.class)
public class CCStatusTest extends PlaywrightManager {

    /** Partial name — UAT lists this CC as "BulkVoucher CC1" (type-2 bulk voucher). */
    private static final String BULK_VOUCHER_SEARCH = "BulkVoucher";

    private CCStatusPage statPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;
    private DashboardPage dashPage;
    private AddFundsPage addFundsPage;
    private String lastCreatedCcName;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        statPage = new CCStatusPage(page);
        dashPage = new DashboardPage(page);
        addFundsPage = new AddFundsPage(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        elementUtils.handleOptionalPanel();
        wait.waitForInvisibility(statPage.getLoader());
        ensureActiveTab();
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
    public void switchToTargetTab() {
        clearSearchFilter();
        ensureActiveTab();
    }

    @AfterMethod(alwaysRun = true)
    public void resetDashboardState() {
        try {
            clearSearchFilter();
            ensureActiveTab();
        } catch (Exception ignored) {
            // Page may be closing after the final test in the class.
        }
    }

    @Test(priority = 1, description = "Verify cost center cannot be inactivated when balance exists")
    public void CC_STAT_001() {
        Locator targetRow = dashPage.getTableRows().first();
        targetRow.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(8000));
        fundCostCenterRow(targetRow, "50");
        openInactivateDialogForRow(targetRow);

        String errorMsg = waitForToastText("claim back the amount");
        Assert.assertEquals(errorMsg, "Please claim back the amount before inactivating.",
                "Mismatch inside automated rule validation toaster payload text processing");
    }

    @Test(priority = 2, description = "Verify inactive option works and redirects to inactive tab")
    public void CC_STAT_002() {
        String suffix = String.valueOf(elementUtils.randomInt(9999, 1000));
        lastCreatedCcName = "Test" + suffix;

        elementUtils.doClick(statPage.getCreateCCBtn());
        elementUtils.sendKeys(statPage.getCcNameField(), lastCreatedCcName);
        elementUtils.sendKeys(statPage.getCcEntityNameField(), "Entity" + suffix);
        elementUtils.sendKeys(statPage.getCcIDField(), "CC" + suffix);
        elementUtils.doClick(statPage.getCcContinueBtn());
        wait.waitForInvisibility(statPage.getLoader());

        searchCostCenter(lastCreatedCcName);
        openInactivateDialogForRow(dashPage.getTableRows().first());
        wait.waitForInvisibility(statPage.getLoader());

        assertTabSelected(statPage.getInactiveTabBtn(),
                "The inactive context focus tracking switch did not transfer to active UI CSS elements");
    }

    @Test(priority = 3, description = "Verify Active/Inactive tab switch works")
    public void CC_STAT_003() {
        elementUtils.doClick(statPage.getInactiveTabBtn());
        wait.waitForInvisibility(statPage.getLoader());

        assertTabSelected(statPage.getInactiveTabBtn(),
                "Navigating tabs did not switch the active presentation classes dynamically");
    }

    @Test(priority = 4, description = "Verify bulk voucher cost center with type 2 cannot be deactivated")
    public void CC_STAT_004() {
        searchCostCenter(BULK_VOUCHER_SEARCH);
        openInactivateDialogForRow(dashPage.getTableRows().first());

        String errorMsg = waitForToastText("cannot inactive this cost center");
        Assert.assertEquals(errorMsg, "You cannot inactive this cost center",
                "Incorrect toast alert returned for Bulk Voucher processing entity operations rules");
    }

    @Test(priority = 5, description = "Verify Master Admin can activate previously inactivated cost center")
    public void CC_STAT_005() {
        Assert.assertNotNull(lastCreatedCcName, "CC_STAT_002 must create a cost center before activation can be verified.");

        elementUtils.doClick(statPage.getInactiveTabBtn());
        wait.waitForInvisibility(statPage.getLoader());
        searchCostCenter(lastCreatedCcName);
        openInactivateDialogForRow(dashPage.getTableRows().first());
        wait.waitForInvisibility(statPage.getLoader());

        assertTabSelected(statPage.getActiveTabBtn(),
                "State Restoration Fault: Processing target entity back to operational context failed to re-focus active tab");
    }

    private boolean isTabSelected(Locator tab) {
        if (!tab.isVisible()) {
            return false;
        }
        String classes = tab.getAttribute("class");
        return classes != null && classes.contains("text-white");
    }

    private void ensureActiveTab() {
        if (isTabSelected(statPage.getActiveTabBtn())) {
            return;
        }
        elementUtils.doClick(statPage.getActiveTabBtn());
        wait.waitForInvisibility(statPage.getLoader());
    }

    private void assertTabSelected(Locator tab, String message) {
        page.waitForCondition(() -> isTabSelected(tab),
                new com.microsoft.playwright.Page.WaitForConditionOptions().setTimeout(8000));
        Assert.assertTrue(isTabSelected(tab), message);
    }

    private void clearSearchFilter() {
        if (dashPage.getResetBtn().isEnabled()) {
            elementUtils.doClick(dashPage.getResetBtn());
        } else {
            elementUtils.clearField(dashPage.getFilterSearchField());
        }
        wait.waitForInvisibility(statPage.getLoader());
    }

    private void searchCostCenter(String value) {
        elementUtils.clearField(dashPage.getFilterSearchField());
        elementUtils.sendKeys(dashPage.getFilterSearchField(), value);
        elementUtils.doClick(dashPage.getFilterSearchBtn());
        wait.waitForInvisibility(statPage.getLoader());

        Locator firstRow = dashPage.getTableRows().first();
        firstRow.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(8000));

        String rowText = firstRow.innerText().toLowerCase();
        Assert.assertTrue(rowText.contains(value.toLowerCase()),
                "First row does not contain '" + value + "'. Actual: " + firstRow.innerText());
    }

    private void fundCostCenterRow(Locator row, String amount) {
        row.locator("text=Add Funds").click();
        addFundsPage.getAddFundsPanel().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(8000));
        elementUtils.sendKeys(addFundsPage.getAddFundsAmountField(), amount);
        elementUtils.doClick(addFundsPage.getAddFundsContinueBtn());
        wait.waitForInvisibility(statPage.getLoader());
        if (addFundsPage.getAddFundsPanel().isVisible()) {
            addFundsPage.getAddFundsCloseBtn().click();
            wait.waitForInvisibility(addFundsPage.getAddFundsPanel());
        }
    }

    private void openInactivateDialogForRow(Locator row) {
        Locator rowMenu = row.locator("i.fa-solid.fa-ellipsis-vertical.fs-2.text-success");
        elementUtils.hoverAndClick(rowMenu, statPage.getInactivateActivateCCBtn());
        elementUtils.doClick(statPage.getOkayStatusChangeBtn());
    }

    private String waitForToastText(String expectedFragment) {
        Locator toast = statPage.getToastMessage()
                .filter(new Locator.FilterOptions().setHasText(expectedFragment));
        toast.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(8000));
        return toast.innerText().trim();
    }
}
