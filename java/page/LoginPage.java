package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;
import utils.ConfigReader;

public class LoginPage {

    private final Page page;

    @Getter private final Locator emailField;
    @Getter private final Locator forgotPassword;
    @Getter private final Locator seePassword;
    @Getter private final Locator otpBackBtn;
    @Getter private final Locator viaSMSBtn;
    @Getter private final Locator viaEmailBtn;
    @Getter private final Locator otpTimer;
    @Getter private final Locator loader;
    @Getter private final Locator toasterMessage;
    @Getter private final Locator passwordField;
    @Getter private final Locator loginBtn;

    public LoginPage(Page page) {
        this.page = page;
        emailField = page.locator("input[name='email']");
        forgotPassword = page.locator("#forgot_password");
        seePassword = page.locator("#showHidePassword, img#showHidePassword, [aria-label*='password' i], [title*='password' i]").first();
        otpBackBtn = page.locator("a.c-pointer.text-decoration-none");
        viaSMSBtn = page.locator("#resend_sms");
        viaEmailBtn = page.locator("#resend_call");
        otpTimer = page.locator("#timer_string");
        loader = page.locator("#showLoader");
        toasterMessage = page.locator("#toast-container .toast-message").last();
        passwordField = page.locator("#password-input");
        loginBtn = page.locator("button.glow-on-hover");
    }

    public void login(String email, String password) {
        int waitMs = ConfigReader.getInt("login.wait.timeout.ms", 30000);
        emailField.waitFor(new Locator.WaitForOptions().setTimeout(waitMs));

        emailField.fill(email);
        passwordField.fill(password);
        loginBtn.click();
    }
}
