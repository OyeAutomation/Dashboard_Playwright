package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.AddFundsPage;
import reusableComponents.SearchFilter;
import utils.ElementUtils;
import utils.WaitUtils;

import java.sql.SQLException;
import utils.AuthenticationService;
import utils.ConfigReader;

@Listeners(TestListener.class)
public class AddFundsTest extends PlaywrightManager {

    private AddFundsPage afPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;
    private SearchFilter searchFilter;
    private long initialBal;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        afPage = new AddFundsPage(page);
        searchFilter = new SearchFilter(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        wait.waitForInvisibility(afPage.getLoader());
        elementUtils.handleOptionalPanel();
    }

    @BeforeMethod(alwaysRun = true)
    public void panelCases() {
        // Guarantee clean state before capturing balance
        closeAddFundsPanelIfOpen();
        wait.waitForInvisibility(afPage.getLoader());

        initialBal = elementUtils.getCurrentBalance(afPage.getTotalWalletBalance());

        // Setup the specific UI state required for all tests in this class
        afPage.getAddFundsBtn().click();
        afPage.getAddFundsPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        wait.waitForInvisibility(afPage.getLoader());
    }

    @AfterMethod(alwaysRun = true)
    public void panelCasesEnd() {
        closeAddFundsPanelIfOpen();
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        closeAddFundsPanelIfOpen();
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }

    /**
     * Centralized cleanup logic to prevent pointer interception errors.
     */
    private void closeAddFundsPanelIfOpen() {
        if (afPage.getAddFundsPanel().isVisible()) {
            afPage.getAddFundsCloseBtn().click();
            wait.waitForInvisibility(afPage.getAddFundsPanel());
        }
    }

    @Test(priority = 1, description = "Verify Add Funds popup opens")
    public void CC_AF_001() {
        Assert.assertTrue(afPage.getAddFundsPanel().isVisible(), "Add Funds Panel is not Visible");
    }

    @Test(priority = 2, description = "Verify cross icon closes Add Funds popup")
    public void CC_AF_002() {
        afPage.getAddFundsCloseBtn().click();
        wait.waitForInvisibility(afPage.getAddFundsPanel());
        Assert.assertFalse(afPage.getAddFundsPanel().isVisible(), "Add Funds Panel cross button is not working");
    }

    @Test(priority = 3, description = "Verify Add Funds panel back button closes the panel")
    public void CC_AF_003() {
        afPage.getAddFundsBackBtn().click();
        wait.waitForInvisibility(afPage.getAddFundsPanel());
        Assert.assertFalse(afPage.getAddFundsPanel().isVisible(), "Add Funds Panel back button is not working");
    }

    @Test(priority = 4, description = "Verify amount field is mandatory")
    public void CC_AF_004() {
        boolean isDisabled = searchFilter.blankSearch(afPage.getAddFundsAmountField(), afPage.getAddFundsContinueBtn());
        Assert.assertTrue(isDisabled, "Continue button is enabled for blank input");
    }

    @Test(priority = 5, description = "Verify Amount field does not accept spaces-only input")
    public void CC_AF_005() {
        searchFilter.spaceSearch(afPage.getAddFundsAmountField(), afPage.getAddFundsContinueBtn());
    }

    @Test(priority = 6, description = "Verify Add Funds transfers amount from main wallet to cost center")
    public void CC_AF_006() {
        String fund = String.valueOf(elementUtils.randomInt(1000, 500));
        long fundAmount = Long.parseLong(fund);

        elementUtils.sendKeys(afPage.getAddFundsAmountField(), fund);
        afPage.getAddFundsContinueBtn().click();
        wait.waitForInvisibility(afPage.getLoader());

        page.waitForCondition(() ->
                (initialBal - elementUtils.getCurrentBalance(afPage.getTotalWalletBalance())) >= fundAmount
        );

        long afterBal = elementUtils.getCurrentBalance(afPage.getTotalWalletBalance());
        long deducted = initialBal - afterBal;
        Assert.assertTrue(deducted >= fundAmount, "Add Funds transaction calculation balance check failed");

        // Try-catch removed. A failed assertion should fail the test, not just print a warning.
        Assert.assertTrue(elementUtils.isTextPresentInFirstRow(fund), "Fund text payload mismatch inside data table row");
    }

    @Test(priority = 7, description = "Verify Add Funds entry falls in View Passbook")
    public void CC_AF_007() {
        String fund = String.valueOf(elementUtils.randomInt(1000, 10));
        elementUtils.sendKeys(afPage.getAddFundsAmountField(), fund);
        afPage.getAddFundsContinueBtn().click();
        wait.waitForInvisibility(afPage.getLoader());

        afPage.getViewPassbookBtn().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        afPage.getViewPassbookBtn().click();
        wait.waitForInvisibility(afPage.getLoader());

        boolean isInPassbook = elementUtils.isTextPresentInFirstRow(fund);
        afPage.getViewPassbookBackBtn().click();

        Assert.assertTrue(isInPassbook, "Transaction execution failure: added funds missing from View Passbook dashboard rows");
    }

    @Test(priority = 8, description = "Verify Add Funds blocks amount more than available balance")
    public void CC_AF_008() {
        String moreThanAvailable = String.valueOf(elementUtils.getCurrentBalance(afPage.getAddFundAvailableBalance()) + 50);

        elementUtils.sendKeys(afPage.getAddFundsAmountField(), moreThanAvailable);
        afPage.getAddFundsContinueBtn().click();

        wait.waitForVisibility(afPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(afPage.getToasterMessage()).trim();
        Assert.assertEquals(errorMsg, "Entered amount is greater than available balance",
                "Mismatch inside edge exception data toast feedback validation");
    }

    @Test(priority = 9, description = "Verify invoice/purpose is stored in Passbook for Add Funds")
    public void CC_AF_009() {
        String fund = String.valueOf(elementUtils.randomInt(100, 10));
        elementUtils.sendKeys(afPage.getAddFundsAmountField(), fund);
        elementUtils.sendKeys(afPage.getAddFundsInvoiceNoField(), "Test" + fund);
        afPage.getAddFundsContinueBtn().click();
        wait.waitForInvisibility(afPage.getLoader());

        afPage.getViewPassbookBtn().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        afPage.getViewPassbookBtn().click();
        wait.waitForInvisibility(afPage.getLoader());

        boolean isInPassbook = elementUtils.isTextPresentInFirstRow("Test" + fund);
        afPage.getViewPassbookBackBtn().click();

        Assert.assertTrue(isInPassbook, "Data integrity failure: Target transaction custom metadata string missing from Passbook history");
    }

    @Test(priority = 10, description = "Verify Add Funds rejects negative amount")
    public void CC_AF_010() {
        elementUtils.sendKeys(afPage.getAddFundsAmountField(), "-123");
        String value = afPage.getAddFundsAmountField().inputValue();

        Assert.assertEquals(value, "123", "Input structural sanitization failed: UI field context parsed a negative character bounds expression");
    }

    @Test(priority = 11, description = "Verify Add Funds rejects zero amount")
    public void CC_AF_011() {
        elementUtils.sendKeys(afPage.getAddFundsAmountField(), "0");
        afPage.getAddFundsContinueBtn().click();

        wait.waitForVisibility(afPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(afPage.getToasterMessage()).trim();
        Assert.assertEquals(errorMsg, "Amount should be greater than 0.", "System accepted zero transactional bounds logic input parameter value");
    }

    @Test(priority = 12, description = "Verify Add Funds rejects alphanumeric amount")
    public void CC_AF_012() {
        elementUtils.sendKeys(afPage.getAddFundsAmountField(), "12abcd");
        String value = afPage.getAddFundsAmountField().inputValue();

        Assert.assertEquals(value, "12", "Data filter layout breakdown: Alphabetic injection string values remained inside numeric form input variables");
    }

    @Test(priority = 13, description = "Verify Add Funds rejects invalid special characters in invoice/purpose")
    public void CC_AF_013() {
        elementUtils.sendKeys(afPage.getAddFundsInvoiceNoField(), ":/");

        wait.waitForVisibility(afPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(afPage.getToasterMessage()).trim();
        Assert.assertEquals(errorMsg, "Only alphanumeric and / - _ ( ) characters are allowed",
                "System context failed to block prohibited structural characters processing expressions within the metadata field entry");
    }
}
