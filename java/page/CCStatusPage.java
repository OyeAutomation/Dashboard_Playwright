package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class CCStatusPage {

    private final Page page;

    @Getter private final Locator inactiveTabBtn;
    @Getter private final Locator hoverThreeDot;
    @Getter private final Locator inactivateActivateCCBtn;
    @Getter private final Locator activeTabBtn;
    @Getter private final Locator indonesiaBtn;
    @Getter private final Locator okayStatusChangeBtn;
    @Getter private final Locator backStatusChangeBtn;
    @Getter private final Locator loader;
    @Getter private final Locator toastMessage;
    @Getter private final Locator createCCBtn;
    @Getter private final Locator createCCBackBtn;
    @Getter private final Locator ccNameField;
    @Getter private final Locator ccEntityNameField;
    @Getter private final Locator ccIDField;
    @Getter private final Locator ccContinueBtn;

    public CCStatusPage(Page page) {
        this.page = page;
        inactiveTabBtn = page.locator("#inactiveCostCenter");
        hoverThreeDot = page.locator("i.fa-solid.fa-ellipsis-vertical.fs-2.text-success");
        inactivateActivateCCBtn = page.locator("#activeInactive0");
        activeTabBtn = page.locator("#activeCostCenter");
        indonesiaBtn = page.locator("#Indonesia_1");
        okayStatusChangeBtn = page.locator("#allowPermission");
        backStatusChangeBtn = page.locator("#declinePermission");
        loader = page.locator("div.ngx-spinner-overlay").first();
        toastMessage = page.locator("#toast-container .toast-message").last();
        createCCBtn = page.locator("#createNewCostCenter");
        createCCBackBtn = page.locator("#backButton3");
        ccNameField = page.locator("#ccName");
        ccEntityNameField = page.locator("#entName");
        ccIDField = page.locator("#cccRName");
        ccContinueBtn = page.locator("#createCostcenter");
    }
}
