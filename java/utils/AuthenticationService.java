package utils;

import base.PlaywrightManager;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import page.LoginPage;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class AuthenticationService {

    private Page page;
    private final ElementUtils elementUtils;
    private final LoginPage loginPage;

    private static final Pattern OTP_PAGE_URL =
            Pattern.compile(".*#/azf.*");

    private static final Pattern DASHBOARD_URL =
            Pattern.compile(".*#/xiurnaipx.*");

    private static final String COOLDOWN_MESSAGE =
            "Wait for 1 minute to generate new OTP";

    public AuthenticationService(Page page) {
        this.page = page;
        this.elementUtils = new ElementUtils(page);
        this.loginPage = new LoginPage(page);
    }

    public boolean performLoginAndOtp(String username, String password) {
        try {
            System.out.println("[AUTH] Logging in as: " + username);

            // Retry login up to 3 times in case of transient failures
            int loginAttempts = 3;
            com.microsoft.playwright.TimeoutError lastError = null;
            for (int i = 1; i <= loginAttempts; i++) {
                try {
                    loginPage.login(username, password);
                    lastError = null;
                    break; // Success
                } catch (com.microsoft.playwright.TimeoutError e) {
                    lastError = e;
                    System.err.println("[AUTH] Login attempt " + i + " failed. Retrying...");
                    if (i < loginAttempts) {
                        try {
                            page.reload();
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
            if (lastError != null) {
                throw lastError;
            }

            waitForOtpScreen();

            if (isOtpCooldownActive()) {
                System.out.println("[AUTH] OTP cooldown detected. Retrying after wait...");
                handleOtpCooldown(username, password);
                waitForOtpScreen();
            }

            String adminId = ConfigReader.getProperty("db.adminId");
            String otp = fetchOtpWithRetry(adminId, 10, 2000);

            if (otp != null) {
                System.out.println("[AUTH] OTP fetched: " + otp);
                fillOtp(otp);
                submitOtp();
                waitForDashboard();
                System.out.println("[AUTH] Auto OTP success");
                return true;
            }

            System.out.println("[AUTH] Auto OTP failed. Waiting for manual OTP...");
            waitForDashboard();
            System.out.println("[AUTH] Manual OTP success");
            return true;

        } catch (Exception e) {
            System.err.println("[AUTH] Authentication failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void waitForOtpScreen() {
        page.waitForURL(
                OTP_PAGE_URL,
                new Page.WaitForURLOptions().setTimeout(30000)
        );
    }

    private void waitForDashboard() {
        long timeoutMs = ConfigReader.getInt("otp.manual.timeout.seconds", 120) * 1000L;
        long deadline = System.currentTimeMillis() + timeoutMs;

        // Happy path: dashboard opens in the same page.
        try {
            if (page != null && !page.isClosed()) {
                page.waitForURL(DASHBOARD_URL, new Page.WaitForURLOptions().setTimeout(timeoutMs));
                PlaywrightManager.updateCurrentPage(page);
                return;
            }
        } catch (PlaywrightException ignored) {
            // Continue with cross-page fallback.
        }

        // Fallback: OTP submit may close the original page and continue in another page/tab.
        BrowserContext ctx = null;
        try {
            if (page != null) {
                ctx = page.context();
            }
        } catch (PlaywrightException ignored) {
        }
        if (ctx == null) {
            ctx = PlaywrightManager.currentContext();
        }
        if (ctx == null) {
            throw new RuntimeException("Dashboard wait failed: browser context unavailable after OTP submit.");
        }

        while (System.currentTimeMillis() < deadline) {
            for (Page p : ctx.pages()) {
                if (p == null || p.isClosed()) {
                    continue;
                }
                try {
                    String url = p.url();
                    if (url != null && DASHBOARD_URL.matcher(url).matches()) {
                        this.page = p;
                        PlaywrightManager.updateCurrentPage(p);
                        return;
                    }
                } catch (PlaywrightException ignored) {
                }
            }
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new RuntimeException("Timed out waiting for dashboard URL on any open page in the current context.");
    }

    private boolean isOtpCooldownActive() {
        try {
            Locator toaster = loginPage.getToasterMessage();
            toaster.waitFor(new Locator.WaitForOptions()
                    .setTimeout(ConfigReader.getInt("otp.cooldown.toast.timeout.seconds", 2) * 1000L));

            String text = toaster.textContent();
            return text != null && text.trim().equals(COOLDOWN_MESSAGE);
        } catch (Exception e) {
            return false;
        }
    }

    private void handleOtpCooldown(String username, String password) throws InterruptedException {
        int cooldownMs = ConfigReader.getInt("otp.cooldown.retry.sleep.ms", 60000);
        Thread.sleep(cooldownMs);
        page.reload();
        loginPage.login(username, password);
    }

    private String fetchOtpWithRetry(String adminId, int attempts, long sleepMs)
            throws SQLException, InterruptedException {

        for (int i = 1; i <= attempts; i++) {
            String otp = elementUtils.getLatestOtpFromDb(adminId);

            if (otp != null && otp.matches("\\d{6}")) {
                return otp;
            }

            System.out.println("[AUTH] OTP not ready yet. Attempt " + i + "/" + attempts);
            Thread.sleep(sleepMs);
        }

        return null;
    }

    private void fillOtp(String otp) {
        if (otp == null || !otp.matches("\\d{6}")) {
            throw new IllegalArgumentException("OTP must be exactly 6 digits");
        }

        for (int i = 0; i < 6; i++) {
            String selector = "#num" + (i + 1);
            page.locator(selector).waitFor(new Locator.WaitForOptions().setTimeout(5000));
            page.locator(selector).fill(String.valueOf(otp.charAt(i)));
        }

        System.out.println("[AUTH] OTP entered");
    }

    private void submitOtp() {
        loginPage.getLoginBtn().click();
    }
}
