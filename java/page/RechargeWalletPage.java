package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class RechargeWalletPage {

    private final Page page;

    @Getter private final Locator rechargeWalletBtn;
    @Getter private final Locator rechargeWalletPanel;
    @Getter private final Locator rechargeWalletCloseBtn;
    @Getter private final Locator rechargeWalletAmountField;
    @Getter private final Locator rechargeWalletContinueBtn;
    @Getter private final Locator toastLocator;
    @Getter private final Locator toasterMessage;
    @Getter private final Locator confirmRechargeContinueBtn;
    @Getter private final Locator confirmRechargeBackBtn;
    @Getter private final Locator confirmRechargeAmount;
    @Getter private final Locator confirmRechargeChargesPercent;
    @Getter private final Locator confirmRechargeFinalAmount;
    @Getter private final Locator paymentGatewayRazorpay;
    @Getter private final Locator closePaymentGateway;
    @Getter private final Locator confirmClosePaymentGateway;
    @Getter private final Locator loader;
    @Getter private final Locator errorToaster;
    @Getter private final Locator balanceLocator;
    @Getter private final Locator btnBeforeNext;
    @Getter private final Locator razorpayCheckoutFrame;
    @Getter private final Locator walletBtn;

    public RechargeWalletPage(Page page) {

        this.page = page;
        walletBtn = page.getByTestId("wallet");
        razorpayCheckoutFrame = page.locator("#razorpay-checkout-v2-container");
        rechargeWalletBtn = page.locator("#rechargeWallet");
        rechargeWalletPanel = page.locator(
                "div.modalContentDiv.text-left.rightModalAnimationDiv"
        );
        rechargeWalletCloseBtn = page.locator("p.my-auto.fs-4.c-pointer");
        rechargeWalletAmountField = page.locator("#enterAmountINR");
        rechargeWalletContinueBtn = page.locator("#continue1");
        toastLocator = page.locator("#toast-container");
        toasterMessage = page.locator("#toast-container .toast-message").last();
        confirmRechargeContinueBtn = page.locator("#continue2");
        confirmRechargeBackBtn = page.locator("#backButton");
        confirmRechargeAmount = page.locator(
                "//*[contains(text(),'Recharge Amount')]/following-sibling::div[1]"
        );
        confirmRechargeChargesPercent = page.locator(
                "//*[contains(text(),'Charges')]/following-sibling::div[1]"
        );
        confirmRechargeFinalAmount = page.locator(
                "//*[contains(text(),'Total Amount')]/following-sibling::div[1]"
        );
        paymentGatewayRazorpay = page.locator("#razorpay-checkout-v2-container");
        closePaymentGateway = page.locator(
                "button[data-testid='checkout-close']"
        );
        confirmClosePaymentGateway = page.locator(
                "button[data-testid='confirm-positive']"
        );
        loader = page.locator("div.ngx-spinner-overlay").first();
        errorToaster = page.locator("#toast-container");
        balanceLocator = page.locator("#totalAvailableBalance");
        btnBeforeNext = page.locator(
                "//li[contains(@class,'pagination-next')]/preceding-sibling::li[1]/a"
        );
    }

    public void sendValueToField(Locator field, Locator btn, String value) {
        field.fill(value);
        btn.click();
    }

    public String getAvailableBalance() {
        return balanceLocator.innerText().trim();
    }

    public void killRazorpayIfPresent() {
        page.evaluate("() => {" +
                "document.querySelectorAll('iframe.razorpay-checkout-frame').forEach(el => el.remove());" +
                "document.querySelectorAll('[id*=\"razorpay\"]').forEach(el => el.remove());" +
                "document.querySelectorAll('[class*=\"razorpay\"]').forEach(el => el.remove());" +
                "}");
    }

    public void jsClick(Locator locator) {
        locator.scrollIntoViewIfNeeded();
        locator.click(new Locator.ClickOptions().setForce(true));
    }
}
