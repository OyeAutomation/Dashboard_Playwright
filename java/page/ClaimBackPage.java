package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;
import utils.ElementUtils;

public class ClaimBackPage {

    private final Page page;

    @Getter private final Locator addFundsAmountField;
    @Getter private final Locator addFundsContinueBtn;
    @Getter private final Locator addFundsBtn;
    @Getter private final Locator claimBackFundsBtn;
    @Getter private final Locator claimBackFundsPanel;
    @Getter private final Locator claimBackFundsCloseBtn;
    @Getter private final Locator claimBackFundsAmountField;
    @Getter private final Locator claimBackFundsInvoiceNoField;
    @Getter private final Locator claimBackFundsContinueBtn;
    @Getter private final Locator toasterMessage;
    @Getter private final Locator claimBackFundsBackBtn;
    @Getter private final Locator totalWalletBalance;
    @Getter private final Locator loader;
    @Getter private final Locator viewPassbookBtn;
    @Getter private final Locator viewPassbookBackBtn;
    @Getter private final Locator claimBackFundAvailableBalance;

    public ClaimBackPage(Page page) {
        this.page = page;
        addFundsAmountField = page.locator("#amount");
        addFundsContinueBtn = page.locator("#addClaimBackFunds");
        addFundsBtn = page.locator("#addFunds_0");
        claimBackFundsBtn = page.locator("#claimBack_0");
        claimBackFundsPanel = page.locator("div[class='modalContentDiv text-left rightModalAnimationDiv']");
        claimBackFundsCloseBtn = page.locator("p.my-auto.fs-4.c-pointer").first();
        claimBackFundsAmountField = page.locator("#amount");
        claimBackFundsInvoiceNoField = page.locator("#comment");
        claimBackFundsContinueBtn = page.locator("#addClaimBackFunds");
        toasterMessage = page.locator("#toast-container .toast-message").last();
        claimBackFundsBackBtn = page.locator("#backButton2");
        totalWalletBalance = page.locator("#totalAvailableBalance");
        loader = page.locator("div.ngx-spinner-overlay").first();
        viewPassbookBtn = page.locator("#viewPassbook");
        viewPassbookBackBtn = page.locator("span[class='fs-6 c-pointer']");
        claimBackFundAvailableBalance = page.locator("p.text-secondary");
    }

    public boolean isTextPresentInFirstRow(String expectedText) {
        return new ElementUtils(page).isTextPresentInFirstRow(expectedText);
    }
}
