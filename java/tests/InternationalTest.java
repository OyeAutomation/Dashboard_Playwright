package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.InternationalPage;
import utils.ElementUtils;
import utils.WaitUtils;

import java.sql.SQLException;
import utils.AuthenticationService;
import utils.ConfigReader;

@Listeners(TestListener.class)
public class InternationalTest extends PlaywrightManager {

    private InternationalPage intPage;
    private ElementUtils elementUtils;
    private WaitUtils wait;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        // 1. Boot up isolated browser profile per test method execution thread
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        // 2. Initialize thread-safe utility and page object instances
        elementUtils = new ElementUtils(page);
        wait = new WaitUtils(page);
        intPage = new InternationalPage(page);

        // 3. Thread-isolated user authentication routine
        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        elementUtils.handleOptionalPanel();

        // 4. Enforce clean state baseline (Route to India context if not defaulted)
        ensureIndiaTabActive();
    }

    @AfterClass(alwaysRun = true)
    public void closeSessionContext() {
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }

    /**
     * Replaces the old Selenium switchToIndiaTab method.
     * Uses safe Playwright locator attributes checks to handle stateless parallel execution.
     */
    private void ensureIndiaTabActive() {
        try {
            wait.waitForInvisibility(intPage.getLoader());
        } catch (TimeoutError ignored) {
            // Loader not present or collapsed early
        }

        Locator tab = intPage.getIndiaBtn();
        String classAttribute = tab.getAttribute("class");

        if (classAttribute != null && classAttribute.contains("theme_pink_border")) {
            // Target country context is already active for this thread execution
            return;
        }

        elementUtils.doClick(intPage.getIndiaBtn());

        try {
            wait.waitForInvisibility(intPage.getLoader());
        } catch (TimeoutError ignored) {
            // Thread synchronization handled cleanly without blocking engine loops
        }
    }

    @Test(priority = 1, description = "Verify international wallet country switch changes currency across dashboard")
    public void CC_INTL_001() {
        elementUtils.doClick(intPage.getIndonesiaBtn());

        try {
            wait.waitForInvisibility(intPage.getLoader());
        } catch (TimeoutError ignored) {
            // Processing state passed context checkpoint early
        }

        // Native Playwright text retrieval guarantees text sync across mutations
        String rawBalanceText = intPage.getTotalAvailableBalance().innerText().trim();
        String currency = rawBalanceText.split(" ")[0];

        Assert.assertEquals(currency, "IDR", "Internationalization Engine Defect: Currency localized string mapping failed for IDR.");
    }

    @Test(priority = 2, description = "Verify Recharge Wallet option is not available for international wallet")
    public void CC_INTL_002() {
        elementUtils.doClick(intPage.getIndonesiaBtn());

        try {
            wait.waitForInvisibility(intPage.getLoader());
        } catch (TimeoutError ignored) {
            // Thread UI processing state settled cleanly
        }

        elementUtils.doClick(intPage.getRechargeWalletBtn());
        wait.waitForVisibility(intPage.getRechargeWalletPanel());

        // Validate complete omission of UI elements without incurring Selenium element visibility timeouts
        boolean isAmountFieldVisible = intPage.getRechargeWalletAmountField().isVisible();

        // Explicit teardown steps before running assertions to minimize left-over UI component hangs
        if (intPage.getRechargeWalletCloseBtn().isVisible()) {
            elementUtils.doClick(intPage.getRechargeWalletCloseBtn());
        }

        Assert.assertFalse(isAmountFieldVisible, "Security/Business Rule Defect: Recharge options leaked into disabled international wallet domains.");
    }
}
