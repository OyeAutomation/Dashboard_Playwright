package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.RechargeWalletPage;
import utils.ElementUtils;
import utils.WaitUtils;
import utils.AuthenticationService;
import utils.ConfigReader;

import java.sql.SQLException;

@Listeners(TestListener.class)
public class RechargeWalletTest extends PlaywrightManager {

    private RechargeWalletPage rwPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;
    private long baselineWalletBalance;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        rwPage = new RechargeWalletPage(page);

        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        try {
            wait.waitForInvisibility(rwPage.getLoader());
        } catch (TimeoutError ignored) {}

        elementUtils.handleOptionalPanel();

        String startBalance = rwPage.getAvailableBalance();
        String numericString = startBalance.replaceAll("[^0-9]", "");
        baselineWalletBalance = Long.parseLong(numericString);
    }

    @BeforeMethod
    public void panelCases() {
        elementUtils.handleOptionalPanel();
        // Removed the always-on wallet button click here to avoid an extra UI action
        // before every test. Each test already opens the panel when needed.
    }

    @AfterMethod
    public void panelCasesEnd() {
        try {
            if (rwPage.getRechargeWalletCloseBtn().isVisible()) {
                elementUtils.doClick(rwPage.getRechargeWalletCloseBtn());
            }
        } catch (PlaywrightException ignored) {
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

    @Test(priority = 1, description = "Verify Recharge Wallet button opens recharge panel")
    public void CC_RW_001() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        rwPage.getRechargeWalletPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        Assert.assertTrue(rwPage.getRechargeWalletPanel().isVisible(), "Recharge Wallet Panel failed configuration expansion mapping states.");
    }

    @Test(priority = 2, description = "Verify Recharge Wallet Panel Close button closes the panel")
    public void CC_RW_002() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        rwPage.getRechargeWalletPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        elementUtils.doClick(rwPage.getRechargeWalletCloseBtn());

        try {
            rwPage.getRechargeWalletPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(5000));
        } catch (TimeoutError e) {
            Assert.fail("Recharge Panel frame failed closure transitions baseline timelines limits.");
        }
        Assert.assertFalse(rwPage.getRechargeWalletPanel().isVisible(), "Recharge Wallet Panel persistent after execution of termination signals.");
    }

    @Test(priority = 3, description = "Validate amount field with blank input")
    public void CC_RW_003() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        elementUtils.clearField(rwPage.getRechargeWalletAmountField());
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        wait.waitForVisibility(rwPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(rwPage.getToasterMessage()).trim();
        Assert.assertEquals(errorMsg, "Please enter a valid amount", "Toaster data text payload mismatched baseline specifications parameters.");
    }

    @Test(priority = 4, description = "Validate amount field with value 0")
    public void CC_RW_004() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "0");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        wait.waitForVisibility(rwPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(rwPage.getToasterMessage()).trim();
        Assert.assertEquals(errorMsg, "Please enter a valid amount", "System verification checks accepted non-operational zero values.");
    }

    @Test(priority = 5, description = "Validate amount field rejects negative value")
    public void CC_RW_005() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "-123");
        String value = rwPage.getRechargeWalletAmountField().inputValue();

        Assert.assertEquals(
                value,
                "123",
                "Data integrity failure: Input form accepted arithmetic inversion signs directly."
        );
    }

    @Test(priority = 6, description = "Validate amount field rejects decimal value if decimals are not allowed")
    public void CC_RW_006() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        rwPage.getRechargeWalletPanel().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));

        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "199.99");
        String value = rwPage.getRechargeWalletAmountField().inputValue();
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        Assert.assertEquals(value, "19999", "Floating point notations permitted inside explicit round currency inputs filters.");
    }

    @Test(priority = 7, description = "Validate amount cant be greater than Rs.500000")
    public void CC_RW_013() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "95222222");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        wait.waitForVisibility(rwPage.getToasterMessage());
        String errorMsg = elementUtils.doGetText(rwPage.getToasterMessage()).trim();

        Assert.assertEquals(errorMsg, "Maximum payment limit per transaction is ₹500000.", "Transaction safety threshold boundary validation failed to engage.");
    }

    @Test(priority = 8, description = "Validate amount field rejects characters")
    public void CC_RW_007() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "12abcd");
        String value = rwPage.getRechargeWalletAmountField().inputValue();

        Assert.assertEquals(value, "12", "Input normalization tracking failed to strip literal text strings configurations parameters.");
    }

    @Test(priority = 9, description = "Validate amount field rejects spaces")
    public void CC_RW_008() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());

        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "  ");
        String value = rwPage.getRechargeWalletAmountField().inputValue();

        Assert.assertEquals(value, "", "Space characters allowed to bypass field format sanitation rules paths.");
    }

    @Test(priority = 10, description = "Verify extra charges and total are displayed correctly for valid amount")
    public void CC_RW_009() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "1999");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        int actualAmount = Integer.parseInt(elementUtils.doGetText(rwPage.getConfirmRechargeAmount()).replaceAll("[^0-9]", ""));
        String chargesStr = elementUtils.doGetText(rwPage.getConfirmRechargeChargesPercent());
        int charge = Integer.parseInt(chargesStr.split(" ")[0].replaceAll("[^0-9]", ""));
        double finalAmount = Double.parseDouble(elementUtils.doGetText(rwPage.getConfirmRechargeFinalAmount()).replaceAll("[^0-9.]", ""));

        double calculatedFinalCharges = actualAmount + (actualAmount * ((double) charge / 100));
        Assert.assertEquals(finalAmount, calculatedFinalCharges, "Mathematical billing configuration error: Fee mapping logic miscalculated output totals.");
    }

    @Test(priority = 11, description = "Verify Back Button works in confirm recharge panel")
    public void CC_RW_010() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "1999");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        elementUtils.doClick(rwPage.getConfirmRechargeBackBtn());

        Assert.assertTrue(rwPage.getRechargeWalletAmountField().isVisible(), "Panel workflow tracking error: Step reversion control dropped core user inputs frame completely.");
    }

    @Test(priority = 12, description = "Verify charges recalculate when amount changes")
    public void CC_RW_011() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "1999");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());
        elementUtils.doClick(rwPage.getConfirmRechargeBackBtn());

        elementUtils.clearField(rwPage.getRechargeWalletAmountField());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "2688");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        int actualAmount = Integer.parseInt(elementUtils.doGetText(rwPage.getConfirmRechargeAmount()).replaceAll("[^0-9]", ""));
        String chargesStr = elementUtils.doGetText(rwPage.getConfirmRechargeChargesPercent());
        int charge = Integer.parseInt(chargesStr.split(" ")[0].replaceAll("[^0-9]", ""));
        double finalAmount = Double.parseDouble(elementUtils.doGetText(rwPage.getConfirmRechargeFinalAmount()).replaceAll("[^0-9.]", ""));

        double calculatedFinalCharges = actualAmount + (actualAmount * ((double) charge / 100));
        System.out.println("Calculated Final Charges: " + calculatedFinalCharges);
        System.out.println("Displayed Final Amount: " + finalAmount);
        Assert.assertEquals(finalAmount, calculatedFinalCharges, "Charges dont recalculate");
    }

    @Test(priority = 13, description = "Verify valid recharge redirects to payment gateway")
    public void CC_RW_012() {
        elementUtils.doClick(rwPage.getRechargeWalletBtn());
        elementUtils.sendKeys(rwPage.getRechargeWalletAmountField(), "25000");
        elementUtils.doClick(rwPage.getRechargeWalletContinueBtn());

        elementUtils.doClick(rwPage.getConfirmRechargeContinueBtn());

        if (rwPage.getToasterMessage().isVisible()) {
            Assert.fail("Failed to open Razorpay: " + rwPage.getToasterMessage().innerText());
        }

        try {
            // 1. Target the explicit V2 container found in image_7d7588.png
            Locator razorpayContainer = page.locator("#razorpay-checkout-v2-container");

            // 2. Dynamically wait for it to render instead of using a flaky iframe check
            razorpayContainer.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
            System.out.println("Razorpay Opens for Valid Recharge");

            // 3. Kill the panel instantly via keyboard escape sequence
            page.keyboard().press("Escape");

            // 4. Verify it closed successfully
            razorpayContainer.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));

        } catch (PlaywrightException e) {
            // Fallback catch block to execute your existing cleanup strategy
            rwPage.killRazorpayIfPresent();
            Assert.fail("Razorpay panel failed to initialize or close cleanly: " + e.getMessage());
        }
    }

    @Test(priority = 14, description = "Verify failed payment does not update wallet balance")
    public void CC_RW_014() {
        String currBalanceStr = rwPage.getAvailableBalance();
        String numericString = currBalanceStr.replaceAll("[^0-9]", "");
        long currBalance = Long.parseLong(numericString);

        Assert.assertEquals(currBalance, baselineWalletBalance, "Security/Business Rule Breach: Unverified checkout flows caused variance inside secure balance entries.");
    }
}
