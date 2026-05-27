package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class InternationalPage {

    private final Page page;

    @Getter private final Locator indonesiaBtn;
    @Getter private final Locator rechargeWalletBtn;
    @Getter private final Locator rechargeWalletCloseBtn;
    @Getter private final Locator totalAvailableBalance;
    @Getter private final Locator indiaBtn;
    @Getter private final Locator loader;
    @Getter private final Locator toastMessage;
    @Getter private final Locator rechargeWalletPanel;
    @Getter private final Locator rechargeWalletAmountField;

    public InternationalPage(Page page) {
        this.page = page;
        indonesiaBtn = page.locator("#Indonesia_1");
        rechargeWalletBtn = page.locator("#rechargeWallet");
        rechargeWalletCloseBtn = page.locator("p.my-auto.fs-4.c-pointer");
        totalAvailableBalance = page.locator("#totalAvailableBalance");
        indiaBtn = page.locator("#India_0");
        loader = page.locator("div.ngx-spinner-overlay").first();
        toastMessage = page.locator("#toast-container .toast-message").last();
        rechargeWalletPanel = page.locator("div[class='modalContentDiv text-left rightModalAnimationDiv']");
        rechargeWalletAmountField = page.locator("#enterAmountINR");
    }
}
