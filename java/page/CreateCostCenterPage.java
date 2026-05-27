package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;
import utils.ElementUtils;

import java.util.List;

public class CreateCostCenterPage {

    private final Page page;

    @Getter private final Locator createCCBtn;
    @Getter private final Locator createCCPanel;
    @Getter private final Locator createCCCloseBtn;
    @Getter private final Locator createCCBackBtn;
    @Getter private final Locator ccNameField;
    @Getter private final Locator ccEntityNameField;
    @Getter private final Locator ccIDField;
    @Getter private final Locator ccDescriptionField;
    @Getter private final Locator ccContinueBtn;
    @Getter private final Locator loader;
    @Getter private final Locator toastMessage;
    @Getter private final Locator noDataText;

    public CreateCostCenterPage(Page page) {
        this.page = page;
        createCCBtn = page.locator("#createNewCostCenter");
        createCCPanel = page.locator("div[class='modalContentDiv rightModalAnimationDiv']");
        createCCCloseBtn = page.locator("p.fs-4.my-auto.c-pointer");
        createCCBackBtn = page.locator("#backButton3");
        ccNameField = page.locator("#ccName");
        ccEntityNameField = page.locator("#entName");
        ccIDField = page.locator("#cccRName");
        ccDescriptionField = page.locator("#descCC");
        ccContinueBtn = page.locator("#createCostcenter");
        loader = page.locator("div.ngx-spinner-overlay").first();
        toastMessage = page.locator("#toast-container .toast-message").last();
        noDataText = page.locator("//h3[contains(text(),'No Data Found')]");
    }

    public boolean isTextPresentInFirstRow(String expectedText) {
        return new ElementUtils(page).isTextPresentInFirstRow(expectedText);
    }

    public boolean isDescriptionPresent(String expected) {
        Locator tooltips = page.locator(".cdk-describedby-message-container div[role='tooltip']");
        List<Locator> elements = tooltips.all();

        for (Locator el : elements) {
            String text = el.textContent();
            if (text != null && text.contains(expected)) {
                return true;
            }
        }
        return false;
    }
}
