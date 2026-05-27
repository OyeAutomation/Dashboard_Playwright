package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.ClaimBackPage;
import reusableComponents.SearchFilter;
import utils.ElementUtils;
import utils.WaitUtils;

import java.sql.SQLException;
import utils.AuthenticationService;
import utils.ConfigReader;

@Listeners(TestListener.class)
public class ClaimBackTest extends PlaywrightManager {

    private ClaimBackPage cbPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;
    private SearchFilter searchFilter;
    private long initialBal;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        // 1. Boot up independent isolated context
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        // 2. Map contextual object states to target thread context
        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        cbPage = new ClaimBackPage(page);
        searchFilter = new SearchFilter(page);

        // 3. Thread isolated login routine
        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");
        elementUtils.handleOptionalPanel();
        wait.waitForInvisibility(cbPage.getLoader());

        // 4. Parallel-Safe Pre-requisite State Setup: Inject foundation funds for the current thread execution context
        elementUtils.doClick(cbPage.getAddFundsBtn());
        elementUtils.sendKeys(cbPage.getAddFundsAmountField(), "1000");
        elementUtils.doClick(cbPage.getAddFundsContinueBtn());
        wait.waitForInvisibility(cbPage.getLoader());

        // 5. Gather isolated runtime baseline metrics and open target operational slider
        initialBal = elementUtils.getCurrentBalance(cbPage.getTotalWalletBalance());
        elementUtils.doClick(cbPage.getClaimBackFundsBtn());
        wait.waitForVisibility(cbPage.getClaimBackFundsPanel());
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
    public void openClaimBackPanel() {
        try {
            if (cbPage != null && !cbPage.getClaimBackFundsPanel().isVisible()) {
                elementUtils.doClick(cbPage.getClaimBackFundsBtn());
                wait.waitForVisibility(cbPage.getClaimBackFundsPanel());
            }
        } catch (Exception ignored) {
            // The panel may already be open or the page may still be settling from the previous test.
        }
    }

    @AfterMethod(alwaysRun = true)
    public void panelCasesEnd() {
        try {
            if (cbPage != null && cbPage.getClaimBackFundsCloseBtn().isVisible()) {
                // Bypass elementUtils.doClick and use a direct JavaScript click
                // This ignores whether the element is animating, moving, or unstable
                cbPage.getClaimBackFundsCloseBtn().dispatchEvent("click");
            }
        } catch (Throwable ignored) {
            // Catching Throwable ensures both AssertionsErrors and Exceptions are caught
        }
    }

    @Test(priority = 1, description = "Verify Claim Back Funds popup opens")
    public void CC_CB_001() {
        Assert.assertTrue(cbPage.getClaimBackFundsPanel().isVisible(), "Claim Back Funds Panel is not Visible");
    }

    @Test(priority = 2, description = "Verify cross icon closes Claim Back Funds popup")
    public void CC_CB_002() {
        elementUtils.doClick(cbPage.getClaimBackFundsCloseBtn());
        wait.waitForInvisibility(cbPage.getClaimBackFundsPanel());
        Assert.assertFalse(cbPage.getClaimBackFundsPanel().isVisible(), "Claim Back Funds Panel cross button is not working");
    }

    @Test(priority = 3, description = "Verify back button closes Claim Back Funds panel")
    public void CC_CB_003() {
        elementUtils.doClick(cbPage.getClaimBackFundsBackBtn());
        wait.waitForInvisibility(cbPage.getClaimBackFundsPanel());
        Assert.assertFalse(cbPage.getClaimBackFundsPanel().isVisible(), "Claim Back Funds Panel back button is not working");
    }

    @Test(priority = 4, description = "Verify amount field is mandatory")
    public void CC_CB_004() {
        boolean isDisabled = searchFilter.blankSearch(cbPage.getClaimBackFundsAmountField(), cbPage.getClaimBackFundsContinueBtn());
        Assert.assertTrue(isDisabled, "Continue button is enabled for blank input");
    }

    @Test(priority = 5, description = "Verify Amount field does not accept spaces-only input")
    public void CC_CB_005() {
        searchFilter.spaceSearch(cbPage.getClaimBackFundsAmountField(), cbPage.getClaimBackFundsContinueBtn());
    }

    @Test(priority = 6, description = "Verify Claim Back Funds transfers amount from cost center to main wallet")
    public void CC_CB_006() {
        String fund = String.valueOf(elementUtils.randomInt(100, 10));
        long expectedBalance = initialBal + Long.parseLong(fund);

        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), fund);
        elementUtils.doClick(cbPage.getClaimBackFundsContinueBtn());
        wait.waitForInvisibility(cbPage.getLoader());

        // Thread safe balance polling logic replaces Selenium's custom WebDriverWait block
        page.waitForCondition(() ->
                elementUtils.getCurrentBalance(cbPage.getTotalWalletBalance()) == expectedBalance
        );

        long actualBalance = elementUtils.getCurrentBalance(cbPage.getTotalWalletBalance());
        Assert.assertEquals(actualBalance, expectedBalance, "Claim Back Funds balance update validation failed");
    }

    @Test(priority = 7, description = "Verify Claim Back Funds entry falls in View Passbook")
    public void CC_CB_007() {
        String fund = String.valueOf(elementUtils.randomInt(100, 10));
        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), fund);
        elementUtils.doClick(cbPage.getClaimBackFundsContinueBtn());
        wait.waitForInvisibility(cbPage.getLoader());

        try {
            cbPage.getViewPassbookBtn().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            elementUtils.doClick(cbPage.getViewPassbookBtn());
            wait.waitForInvisibility(cbPage.getLoader());

            boolean isInPassbook = elementUtils.isTextPresentInFirstRow(fund);
            elementUtils.doClick(cbPage.getViewPassbookBackBtn());
            Assert.assertTrue(isInPassbook, "Claimed amount payload entry not verified in View Passbook data mapping rows");
        } catch (TimeoutError e) {
            Assert.fail("Component transition timeout: unable to process passbook dashboard views context structures", e);
        }
    }

    @Test(priority = 8, description = "Verify Claim Back Funds blocks amount more than available balance")
    public void CC_CB_008() {
        String moreThanAvailable = String.valueOf(elementUtils.getCurrentBalance(cbPage.getClaimBackFundAvailableBalance()) + 50);

        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), moreThanAvailable);
        elementUtils.doClick(cbPage.getClaimBackFundsContinueBtn());

        try {
            wait.waitForVisibility(cbPage.getToasterMessage());
            String errorMsg = elementUtils.doGetText(cbPage.getToasterMessage()).trim();
            Assert.assertEquals(errorMsg, "Amount greater than available amount.",
                    "Toast notification error string payload message text validation mismatched");
        } catch (TimeoutError e) {
            Assert.fail("Constraint Engine Defect: System permitted execution workflows for values exceeding context boundary caps", e);
        }
    }

    @Test(priority = 9, description = "Verify invoice/purpose is stored in Passbook for Claim Back Funds")
    public void CC_CB_009() {
        String fund = String.valueOf(elementUtils.randomInt(100, 10));
        String targetedInvoice = "Test" + fund;

        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), fund);
        elementUtils.sendKeys(cbPage.getClaimBackFundsInvoiceNoField(), targetedInvoice);
        elementUtils.doClick(cbPage.getClaimBackFundsContinueBtn());
        wait.waitForInvisibility(cbPage.getLoader());

        try {
            cbPage.getViewPassbookBtn().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            elementUtils.doClick(cbPage.getViewPassbookBtn());
            wait.waitForInvisibility(cbPage.getLoader());

            // Automated inline table lookup loop execution context validation using string mapping strings
            page.waitForCondition(() -> page.locator("//table/tbody/tr/td").allInnerTexts().stream()
                    .anyMatch(text -> text.trim().contains(targetedInvoice))
            );

            boolean isInPassbook = page.locator("//table/tbody/tr/td").allInnerTexts().stream()
                    .anyMatch(text -> text.trim().contains(targetedInvoice));

            elementUtils.doClick(cbPage.getViewPassbookBackBtn());
            Assert.assertTrue(isInPassbook, "Data validation missing: invoice purpose tracker target identifier absent inside history table rows");
        } catch (TimeoutError e) {
            Assert.fail("Core user journey execution thread dropped before completion of data mapping checks", e);
        }
    }

    @Test(priority = 10, description = "Verify Claim Back Funds rejects negative amount")
    public void CC_CB_010() {
        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), "-123");
        String value = cbPage.getClaimBackFundsAmountField().inputValue();

        Assert.assertEquals(value, "123", "Sanitization component dropped negative input character constraints parameter logic");
    }

    @Test(priority = 11, description = "Verify Claim Back Funds rejects zero amount")
    public void CC_CB_011() {
        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), "0");
        elementUtils.doClick(cbPage.getClaimBackFundsContinueBtn());

        try {
            wait.waitForVisibility(cbPage.getToasterMessage());
            String errorMsg = elementUtils.doGetText(cbPage.getToasterMessage()).trim();
            Assert.assertEquals(errorMsg, "Amount should be greater then 0.", "Wrong validation parsing empty numeric boundaries criteria input logs");
        } catch (TimeoutError e) {
            Assert.fail("Business rule execution bypass: transaction processor interface initialized parameters utilizing zero amounts", e);
        }
    }

    @Test(priority = 12, description = "Verify Claim Back Funds rejects alphanumeric amount")
    public void CC_CB_012() {
        elementUtils.sendKeys(cbPage.getClaimBackFundsAmountField(), "12abcd");
        String value = cbPage.getClaimBackFundsAmountField().inputValue();

        Assert.assertEquals(value, "12", "UI layer verification layout didn't filter out trailing characters within text values fields entries");
    }

    @Test(priority = 13, description = "Verify Add Funds rejects invalid special characters in invoice/purpose")
    public void CC_CB_013() {
        wait.waitForInvisibility(cbPage.getToasterMessage());
        elementUtils.sendKeys(cbPage.getClaimBackFundsInvoiceNoField(), ":/");

        try {
            wait.waitForVisibility(cbPage.getToasterMessage());
            String errorMsg = elementUtils.doGetText(cbPage.getToasterMessage()).trim();
            Assert.assertEquals(errorMsg, "Only alphanumeric and / - _ ( ) characters are allowed",
                    "System validation parameters miscalculated malformed characters processing boundaries verification criteria error feedback triggers");
        } catch (TimeoutError e) {
            Assert.fail("Invalid characters escape error handling missed structural pattern validation intercept tracking filters", e);
        }
    }
}
