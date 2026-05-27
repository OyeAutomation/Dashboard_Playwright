package tests;

import base.PlaywrightManager;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import listeners.TestListener;
import org.testng.Assert;
import org.testng.annotations.*;
import page.CreateCostCenterPage;
import reusableComponents.SearchFilter;
import utils.ElementUtils;
import utils.WaitUtils;

import java.sql.SQLException;
import utils.AuthenticationService;
import utils.ConfigReader;
import java.util.UUID;

import static utils.ElementUtils.SQLI;

@Listeners(TestListener.class)
public class CreateCostCenterTest extends PlaywrightManager {

    private CreateCostCenterPage ccPage;
    private ElementUtils elementUtils;
    private SearchFilter searchFilter;

    private WaitUtils wait;
    private WaitUtils shortWait;
    private WaitUtils loaderWait;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws SQLException, InterruptedException {
        // 1. Launch a dedicated browser instance for this execution thread
        PlaywrightManager.acquireClassSlot();
        startBrowser();

        // 2. Map thread-safe contextual dependencies
        elementUtils = new ElementUtils(page);
        ccPage = new CreateCostCenterPage(page);
        searchFilter = new SearchFilter(page);

        // 3. Isolated session authentication sequence
        AuthenticationService authService = new AuthenticationService(page);
        boolean[] isLoggedIn = new boolean[1];
        PlaywrightManager.runWithOtpLock(() -> isLoggedIn[0] = authService.performLoginAndOtp(
                ConfigReader.getProperty("log.username"),
                ConfigReader.getProperty("log.password")
        ));
        Assert.assertTrue(isLoggedIn[0], "Thread Authentication Failure: Route blocked before dashboard visibility.");

        // 4. Instantiate isolated wait threshold configurations bound to this specific page profile
        wait = new WaitUtils(page);       // Uses Playwright's native timeouts implicitly or explicit custom lengths
        shortWait = new WaitUtils(page);
        loaderWait = new WaitUtils(page);

        elementUtils.handleOptionalPanel();

        // 5. Execute structural panel workflow initialization steps per method thread
        loaderWait.waitForInvisibility(ccPage.getLoader());
        elementUtils.doClick(ccPage.getCreateCCBtn());
    }

    @BeforeMethod(alwaysRun = true)
    public void openCreateCostCenterPanel() {
        try {
            loaderWait.waitForInvisibility(ccPage.getLoader());
            if (ccPage != null && !ccPage.getCreateCCPanel().isVisible()) {
                elementUtils.doClick(ccPage.getCreateCCBtn());
                wait.waitForVisibility(ccPage.getCreateCCPanel());
            }
        } catch (Exception ignored) {
            // The create panel may already be open or temporarily detached between tests.
        }
    }

    @AfterMethod(alwaysRun = true)
    public void panelCasesEnd() {
        // 1. Ensure any blocking spinner is completely gone first
        if (ccPage != null) {
            loaderWait.waitForInvisibility(ccPage.getLoader());

            Locator closeBtn = ccPage.getCreateCCCloseBtn();

            try {
                // 2. Fall back to standard count check. If it exists in DOM, dispatch JS click.
                if (closeBtn.count() > 0) {
                    closeBtn.evaluate("el => el.click()");
                    System.out.println("[TEARDOWN] Panel closed cleanly via JS evaluation loop.");
                }
            } catch (Exception e) {
                // Catching general Exception to prevent unhandled driver crashes from ruining the suite run
                System.err.println("[TEARDOWN WARNING] Safe panel cleanup bypassed: " + e.getMessage());
            }
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

    @Test(priority = 1, description = "Verify Create New Cost Center panel opens from Dashboard")
    public void CC_CC_001() {
        wait.waitForVisibility(ccPage.getCreateCCPanel());
        Assert.assertTrue(ccPage.getCreateCCPanel().isVisible(), "Create CC Panel is not Visible");
    }

    @Test(priority = 2, description = "Verify Back button closes Create New Cost Center panel")
    public void CC_CC_002() {
        elementUtils.doClick(ccPage.getCreateCCBackBtn());
        wait.waitForInvisibility(ccPage.getCreateCCPanel());
        Assert.assertFalse(ccPage.getCreateCCPanel().isVisible(), "Create CC Panel back button is not working");
    }

    @Test(priority = 3, description = "Verify cross icon closes Create New Cost Center panel")
    public void CC_CC_003() {
        elementUtils.doClick(ccPage.getCreateCCCloseBtn());
        wait.waitForInvisibility(ccPage.getCreateCCPanel());
        Assert.assertFalse(ccPage.getCreateCCPanel().isVisible(), "Create CC Panel close button is not working");
    }

    @Test(priority = 4, description = "Verify Cost Center Name is mandatory")
    public void CC_CC_004() {
        boolean isDisabled = searchFilter.blankSearch(ccPage.getCcNameField(), ccPage.getCcContinueBtn());
        Assert.assertTrue(isDisabled, "Continue button is enabled for blank input parameters configurations");
    }

    @Test(priority = 5, description = "Verify Cost Center Name does not accept spaces-only input")
    public void CC_CC_005() {
        searchFilter.spaceSearch(ccPage.getCcNameField(), ccPage.getCcContinueBtn());
    }

    @Test(priority = 6, description = "Verify Create button creates a new cost center successfully")
    public void CC_CC_006() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        // Playwright handles the visibility checking implicitly, eliminating custom budget calculations loops
        wait.waitForInvisibility(ccPage.getCreateCCPanel());


        Assert.assertTrue(ccPage.isTextPresentInFirstRow(shortId), "Create CC core execution functionality broken");
    }

    @Test(priority = 7, description = "Verify Entity Name entered during Create Cost Center is reflected in listing")
    public void CC_CC_007() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        String entityName = "Test Entity Field";

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcEntityNameField(), entityName);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        wait.waitForInvisibility(ccPage.getCreateCCPanel());
        Assert.assertTrue(ccPage.isTextPresentInFirstRow(entityName), "Create CC Entity Name Entry is not Available in grid mappings");
    }

    @Test(priority = 8, description = "Verify Cost Center Id entered during Create Cost Center is reflected in listing")
    public void CC_CC_008() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        String ccID = "ID_01";

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcIDField(), ccID);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        try {
            // Brief validation check check to catch early toast messages errors
            ccPage.getToastMessage().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(2000));
        } catch (TimeoutError e) {
            // Absence of toaster implies entry validation passed cleanly
        }

        wait.waitForInvisibility(ccPage.getCreateCCPanel());
        Assert.assertTrue(ccPage.isTextPresentInFirstRow(ccID), "Create CC Entity ID Entry is not Available");
    }



    @Test(priority = 9, description = "Verify Description entered during Create Cost Center is saved correctly")
    public void CC_CC_009() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        String descID = "This is a Demo Description";

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcDescriptionField(), descID);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        // Explicitly confirm the entry UI panel dismisses cleanly before validation
        wait.waitForInvisibility(ccPage.getCreateCCPanel());

        // Ensure the underlying grid system updates via a brief polling validation loop
        // Use your local or inherited Playwright page instance directly
        page.waitForCondition(() -> ccPage.isDescriptionPresent(descID));
        Assert.assertTrue(ccPage.isDescriptionPresent(descID), "Create CC Entity Desc Entry is not Available in dashboard panels structures");
    }

    @Test(priority = 10, description = "Verify Description field accepts maximum 100 characters")
    public void CC_CC_010() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        String hundredString = "AIJKLMNOABCDEFGHIJKLMNOABCDEFGHIJKLMNOABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcDescriptionField(), hundredString);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        wait.waitForInvisibility(ccPage.getCreateCCPanel());

        // Use your local or inherited Playwright page instance directly
        page.waitForCondition(() -> ccPage.isDescriptionPresent(hundredString));
        Assert.assertTrue(ccPage.isDescriptionPresent(hundredString), "Unable to pass valid string with precisely 100 characters in Description field container");
    }

    @Test(priority = 11, description = "Verify Description field rejects more than 100 characters")
    public void CC_CC_011() {
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);
        String hundredPlusString = "MoreThanAIJKLMNOABCDEFGHIJKLMNOABCDEFGHIJKLMNOABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcDescriptionField(), hundredPlusString);
        elementUtils.doClick(ccPage.getCcContinueBtn());

        try {
            wait.waitForVisibility(ccPage.getToastMessage());
            Assert.assertEquals(elementUtils.doGetText(ccPage.getToastMessage()).trim(), "Maximum Character limit exceed",
                    "System parsed over-boundary values within form strings properties parameters processing logs");
        } catch (TimeoutError e) {
            Assert.fail("100+ chars in Desc field did not generate an execution validation intercept context fault rules.", e);
        }
    }

    @Test(priority = 12, description = "Verify duplicate cost center name is not allowed")
    public void CC_CC_012() {
        wait.waitForInvisibility(ccPage.getToastMessage());
        elementUtils.sendKeys(ccPage.getCcNameField(), "BulkVoucher CC");
        elementUtils.doClick(ccPage.getCcContinueBtn());

        wait.waitForVisibility(ccPage.getToastMessage());
        Assert.assertEquals(elementUtils.doGetText(ccPage.getToastMessage()).trim(), "Cost center with same name already exists",
                "Duplicate Cost center should not be allowed");
    }

    @Test(priority = 13, description = "Verify Symbolic cost center name is not allowed")
    public void CC_CC_013() {
        wait.waitForInvisibility(ccPage.getToastMessage());
        page.setDefaultTimeout(5000);
        String expectedMsg = "Cost center name with symbol not allowed";
        elementUtils.sendKeys(ccPage.getCcNameField(), UUID.randomUUID().toString().substring(0, 5) + "RAJU@#$");
        elementUtils.doClick(ccPage.getCcContinueBtn());

        String actualMsg = ccPage.getToastMessage().innerText();
        Assert.assertEquals(actualMsg, expectedMsg, "Symbolic Cost center error does not match");
    }

    @Test(priority = 14, description = "Verify CC Name field rejects more than 50 characters")
    public void CC_CC_014() {
        wait.waitForInvisibility(ccPage.getToastMessage());
        page.setDefaultTimeout(5000);
        String expectedMsg = "Cost center name more than 50 chars not allowed";
        elementUtils.sendKeys(ccPage.getCcNameField(), "MNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + UUID.randomUUID().toString().substring(0,5));
        elementUtils.doClick(ccPage.getCcContinueBtn());

        try{
            String actualMsg = ccPage.getToastMessage().innerText();
            Assert.assertEquals(actualMsg, expectedMsg, "Limit Cost center error does not match");
        } catch (TimeoutError e) {
            throw new TimeoutError("CC Name field with more than 50 chars is not rejected",e);
        }
    }

    @Test(priority = 15, description = "Verify Entity Name field rejects more than 50 characters")
    public void CC_CC_015() {
        wait.waitForInvisibility(ccPage.getToastMessage());
        page.setDefaultTimeout(5000);
        String expectedMsg = "Entity name more than 50 chars not allowed";
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcEntityNameField(), "MNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + UUID.randomUUID().toString().substring(0, 2));
        elementUtils.doClick(ccPage.getCcContinueBtn());

        try{
            String actualMsg = ccPage.getToastMessage().innerText();
            Assert.assertEquals(actualMsg, expectedMsg, "Entity Name char exceed error does not match");
        } catch (TimeoutError e) {
            throw new TimeoutError("Entity Name field with more than 50 chars is not rejected",e);
        }
    }

    @Test(priority = 16, description = "Verify CC ID field rejects more than 50 characters")
    public void CC_CC_016() {
        page.setDefaultTimeout(5000);
        String expectedMsg = "CCId name more than 50 chars not allowed";
        String shortId = "TestCase " + UUID.randomUUID().toString().substring(0, 5);

        elementUtils.sendKeys(ccPage.getCcNameField(), shortId);
        elementUtils.sendKeys(ccPage.getCcIDField(), "MNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" + UUID.randomUUID().toString().substring(0, 2));
        elementUtils.doClick(ccPage.getCcContinueBtn());

        try{
            String actualMsg = ccPage.getToastMessage().innerText();
            Assert.assertEquals(actualMsg, expectedMsg, "CC ID char exceed error does not match");
        } catch (TimeoutError e) {
            throw new TimeoutError("CC ID field with more than 50 chars is not rejected",e);
        }
    }

    @Test(priority = 17, description = "Verify Create CC free-text fields block SQL injection payloads")
    public void CC_CC_017() {
        com.microsoft.playwright.Locator[] fields = {
                ccPage.getCcEntityNameField(),
                ccPage.getCcIDField(),
                ccPage.getCcDescriptionField()
        };
        String[] fieldNames = { "EntityName", "CC_ID", "Description" };

        for (int f = 0; f < fields.length; f++) {
            com.microsoft.playwright.Locator field = fields[f];
            String fieldName = fieldNames[f];

            for (int i = 0; i < SQLI.length; i++) {
                String payload = SQLI[i];
                String context = "[Field=" + fieldName + " | PayloadIndex=" + i + " | Payload=" + payload + "]";

                loaderWait.waitForInvisibility(ccPage.getLoader());

                ccPage.getCcNameField().clear();
                elementUtils.sendKeys(ccPage.getCcNameField(), "AutoCC_" + UUID.randomUUID().toString().substring(0, 5));

                loaderWait.waitForInvisibility(ccPage.getLoader());

                field.clear();
                elementUtils.sendKeys(field, payload);

                long clickStart = System.currentTimeMillis();
                elementUtils.doClick(ccPage.getCcContinueBtn());
                long clickDuration = System.currentTimeMillis() - clickStart;

                System.out.println("Executing thread localized payload check -> " + context + " | ClickDuration=" + clickDuration + "ms");

                boolean isErrorVisible = false;
                try {
                    // Micro timeout context configuration checking targeted alerts elements structures visibility states
                    ccPage.getToastMessage().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(1000));
                    isErrorVisible = true;
                } catch (Exception e) {
                    // Normal execution path: absence of toast elements implies silent entry parameters sanitation
                }

                if (isErrorVisible) {
                    String msg = elementUtils.doGetText(ccPage.getToastMessage()).toLowerCase().trim();
                    System.out.println("Toast Intercepted -> " + context + " | Message=" + msg);

                    if (i >= 3 && i < 7) {
                        Assert.assertFalse(msg.contains("syntax error near '"), "Information Leakage / Context Exposure detected: " + context);
                    }
                    Assert.assertFalse(msg.contains("sql") || msg.contains("syntax"), "Security Boundary Defect: Unchecked SQL parsing trace leaked to UI -> " + context);
                } else {
                    System.out.println("No Toast Feedback Captured -> " + context);

                    if (i >= 1 && i < 3) {
                        // Safe multi-element verification using lazy locators resolution elements counts structures metrics
                        int rowCount = (int) ccPage.getNoDataText().count();
                        System.out.println("Empty Grid State Counter -> " + context + " | Count=" + rowCount);
                        Assert.assertEquals(rowCount, 0, "Security Leak: Arbitrary query manipulation caused internal visibility mutations -> " + context);
                    }

                    if (payload.toLowerCase().contains("sleep")) {
                        System.out.println("Blind Delay Diagnostic Execution Metrics -> " + context + " | Delta=" + clickDuration);
                    }

                    // Ensure interface panel execution hasn't locked down thread lifecycle states
                    shortWait.waitForInvisibility(ccPage.getCreateCCPanel());
                    Assert.assertFalse(ccPage.getCreateCCPanel().isVisible(), "UI Hang / Continuous Processing Lockup discovered -> " + context);

                    elementUtils.doClick(ccPage.getCreateCCBtn());
                }
            }
        }
    }
}
