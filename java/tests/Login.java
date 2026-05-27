package tests;

import base.PlaywrightManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import com.microsoft.playwright.options.WaitForSelectorState;
import page.LoginPage;
import utils.ConfigReader;
import utils.ElementUtils;

public class Login extends PlaywrightManager {

    private ElementUtils elementUtils;
    private LoginPage loginPage;

    @BeforeClass(alwaysRun = true)
    public void setUp() throws InterruptedException {
        PlaywrightManager.acquireClassSlot();
        startBrowser();
        elementUtils = new ElementUtils(page);
        loginPage = new LoginPage(page);
    }

    @BeforeMethod(alwaysRun = true)
    public void resetLoginPage() {
        page.navigate(ConfigReader.getProperty("devUrl"));
        loginPage.getEmailField().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        try {
            closeBrowser();
        } finally {
            PlaywrightManager.releaseClassSlot();
        }
    }

    @Test(priority = 1, description = "Verify Correct Message incase of empty fields")
    public void TC_LOG_001() {
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Please fill all Mandatory fields", "Wrong Toast message");

        System.out.println("Verify Correct Message incase of empty fields VALIDATED!");
    }

    @Test(priority = 2, description = "Verify email field is sent empty and password is filled")
    public void TC_LOG_002() {
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Please fill all Mandatory fields", "Wrong Toast message");

        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Verify email field is sent empty and password is filled");
    }

    @Test(priority = 3, description = "Verify password field is sent empty and email is filled")
    public void TC_LOG_003() {
        elementUtils.sendKeys(loginPage.getEmailField(), "xyz123@benepik.com");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Please fill all Mandatory fields", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        System.out.println("Verify password field is sent empty and email is filled");
    }

    @Test(priority = 4, description = "Invalid Email Format: Missing '@'")
    public void TC_LOG_004() {
        page.waitForTimeout(2000);
        elementUtils.sendKeys(loginPage.getEmailField(), "xyz12");
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Invalid email Format", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Invalid Email Format: Missing '@'");
    }

    @Test(priority = 5, description = "Invalid Email Format: Missing Domain")
    public void TC_LOG_005() {
        page.waitForTimeout(2000);
        elementUtils.sendKeys(loginPage.getEmailField(), "xyz12@benepik");
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Invalid email Format", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Invalid Email Format: Missing Domain");
    }

    @Test(priority = 6, description = "Invalid Email Format: Special Characters in Domain")
    public void TC_LOG_006() {
        page.waitForTimeout(2000);
        elementUtils.sendKeys(loginPage.getEmailField(), "user@dom!ain.com");
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Invalid email Format", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Invalid Email Format: Special Characters in Domain");
    }

    @Test(priority = 7, description = "Show Hide Password button works")
    public void TC_LOG_007() {
        page.waitForTimeout(2000);
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getSeePassword());

        // Fixed Bug: Extracted attribute directly from the POM field instead of nesting it inside a page.locator() call
        Assert.assertEquals(loginPage.getPasswordField().getAttribute("type"),
                "text", "See Password button is not working");

        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Show Hide Password button works");
    }

    @Test(priority = 8, description = "Leading/Trailing White Spaces in Email")
    public void TC_LOG_008() {
        elementUtils.sendKeys(loginPage.getEmailField(), "     xyz123@abc.com   ");
        elementUtils.sendKeys(loginPage.getPasswordField(), "xyz123");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Invalid email Format", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Leading or Trailing White Spaces in Email");
    }

    @Test(priority = 9, description = "Verify 1-min cooldown for continuous OTPs")
    public void TC_LOG_009() {
        PlaywrightManager.runWithOtpLock(() -> {
            elementUtils.sendKeys(loginPage.getEmailField(), ConfigReader.getProperty("loginUsername"));
            elementUtils.sendKeys(loginPage.getPasswordField(), ConfigReader.getProperty("loginPassword"));
            elementUtils.doClick(loginPage.getLoginBtn());

            page.waitForURL(url -> url.contains("#/azf"));
            elementUtils.doClick(loginPage.getOtpBackBtn());
            page.waitForTimeout(2000);

            page.waitForURL(url -> url.contains("#/lamwv"));
            elementUtils.sendKeys(loginPage.getEmailField(), ConfigReader.getProperty("loginUsername"));
            elementUtils.sendKeys(loginPage.getPasswordField(), ConfigReader.getProperty("loginPassword"));
            elementUtils.doClick(loginPage.getLoginBtn());

            Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                    "Wait for 1 minute to generate new OTP", "Wrong Toast message");

            elementUtils.clearField(loginPage.getEmailField());
            elementUtils.clearField(loginPage.getPasswordField());
        });
        System.out.println("Verify 1-min cooldown for continuous OTPs");
    }

    @Test(priority = 10, description = "Verify wrong password toaster message")
    public void TC_LOG_010() {
        page.waitForTimeout(2000);
        elementUtils.sendKeys(loginPage.getEmailField(), ConfigReader.getProperty("log.username"));
        elementUtils.sendKeys(loginPage.getPasswordField(), "wrongpass1");
        elementUtils.doClick(loginPage.getLoginBtn());

        Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                "Invalid credentials 2 more attempt left.", "Wrong Toast message");

        elementUtils.clearField(loginPage.getEmailField());
        elementUtils.clearField(loginPage.getPasswordField());
        System.out.println("Verify wrong password toaster message");
    }

    @Test(priority = 11, description = "Verify account lockout after 3 failed attempts")
    public void TC_LOG_011() {
        for (int attempt = 0; attempt < 2; attempt++) {
            page.waitForTimeout(2000);
            elementUtils.sendKeys(loginPage.getEmailField(), ConfigReader.getProperty("log.username"));
            elementUtils.sendKeys(loginPage.getPasswordField(), "wrongpass1");
            elementUtils.doClick(loginPage.getLoginBtn());
            loginPage.getToasterMessage().first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE));
        }

        try {
            Assert.assertEquals(loginPage.getToasterMessage().first().innerText().trim(),
                    "Your account has been locked, connect with the admin", "Wrong Toast message");
        } catch (Exception e) {
            Assert.fail("Account lockout after 3 failed attempts not handled!", e);
        } finally {
            elementUtils.clearField(loginPage.getEmailField());
            elementUtils.clearField(loginPage.getPasswordField());

            // --- RUN YOUR SQL QUERY HERE ---
            String username = ConfigReader.getProperty("log.username");
            String sqlQuery = "UPDATE `benepik_plus`.`tbl_client_admin` SET `lockAccount` = '0' WHERE `autoId` = " + ConfigReader.getProperty("db.adminId") + ";";
            elementUtils.executeUpdate(sqlQuery);

            System.out.println("Verify account lockout after 3 failed attempts processed.");
        }
    }

}
