package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;
import utils.ElementUtils;

public class AddFundsPage {

    private final Page page;

    @Getter private final Locator addFundsBtn;
    @Getter private final Locator addFundsPanel;
    @Getter private final Locator addFundsCloseBtn;
    @Getter private final Locator addFundsAmountField;
    @Getter private final Locator addFundsInvoiceNoField;
    @Getter private final Locator addFundsContinueBtn;
    @Getter private final Locator toasterMessage;
    @Getter private final Locator addFundsBackBtn;
    @Getter private final Locator totalWalletBalance;
    @Getter private final Locator loader;
    @Getter private final Locator viewPassbookBtn;
    @Getter private final Locator viewPassbookBackBtn;
    @Getter private final Locator addFundAvailableBalance;

    public AddFundsPage(Page page) {
        this.page = page;
        addFundsBtn = page.locator("#addFunds_0");
        addFundsPanel = page.locator("div[class='modalContentDiv text-left rightModalAnimationDiv']");
        addFundsCloseBtn = page.locator("p.my-auto.fs-4.c-pointer");
        addFundsAmountField = page.locator("#amount");
        addFundsInvoiceNoField = page.locator("#comment");
        addFundsContinueBtn = page.locator("#addClaimBackFunds");
        toasterMessage = page.locator("#toast-container .toast-message").last();
        addFundsBackBtn = page.locator("#backButton2");
        totalWalletBalance = page.locator("#totalAvailableBalance");
        loader = page.locator("div.ngx-spinner-overlay").first();
        viewPassbookBtn = page.locator("#viewPassbook");
        viewPassbookBackBtn = page.locator("span[class='fs-6 c-pointer']");
        addFundAvailableBalance = page.locator("p.text-secondary");
    }

    public boolean isTextPresentInFirstRow(String expectedText) {
        return new ElementUtils(page).isTextPresentInFirstRow(expectedText);
    }
}
