package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class DashboardPage {

    private final Page page;

    @Getter private final Locator viewPassbookBackBtn;
    @Getter private final Locator viewPassbookBtn;
    @Getter private final Locator costCenterNameLocator;
    @Getter private final Locator ccTable;
    @Getter private final Locator tableRows;
    @Getter private final Locator errorToaster;
    @Getter private final Locator filterSearchField;
    @Getter private final Locator filterSearchBtn;
    @Getter private final Locator exportBtn;
    @Getter private final Locator balanceLocator;
    @Getter private final Locator resetBtn;
    @Getter private final Locator notFoundLocator;
    @Getter private final Locator noDataText;
    @Getter private final Locator dataEntryChange;
    @Getter private final Locator pageCount;
    @Getter private final Locator createCCBtn;
    @Getter private final Locator ccName;
    @Getter private final Locator finalCCBtn;
    @Getter private final Locator loader;
    @Getter private final Locator toastMessage;
    @Getter private final Locator paginationLocator;
    @Getter private final Locator pageNextBtn;
    @Getter private final Locator pagePrevBtn;
    @Getter private final Locator btnAfterPrev;
    @Getter private final Locator btnBeforeNext;

    public DashboardPage(Page page) {
        this.page = page;
        viewPassbookBackBtn = page.locator("h6[class*='mb-0']");
        viewPassbookBtn = page.locator("#viewPassbook");
        costCenterNameLocator = page.locator("span[class='link']");
        ccTable = page.locator("table.animate__fadeIn");
        tableRows = page.locator("table.animate__fadeIn tbody tr");
        errorToaster = page.locator("#toast-container");
        filterSearchField = page.locator("#costCenterSearch");
        filterSearchBtn = page.locator("#searchText");
        exportBtn = page.locator("#exportData");
        balanceLocator = page.locator("#totalAvailableBalance");
        resetBtn = page.locator("#resetSearchFilter");
        notFoundLocator = page.locator("h3.text-center");
        noDataText = page.locator("h3:has-text('No Data Found')");
        dataEntryChange = page.locator("#showtableDataEntries3");
        pageCount = page.locator("p.text-dark");
        createCCBtn = page.locator("#createNewCostCenter");
        ccName = page.locator("#ccName");
        finalCCBtn = page.locator("#createCostcenter");
        loader = page.locator("motion.div.ngx-spinner-overlay, div.ngx-spinner-overlay");
        toastMessage = page.locator("#toast-container .toast-message").last();
        paginationLocator = page.locator("nav[aria-label='Pagination']");
        pageNextBtn = page.locator("li[class*='pagination-next']");
        pagePrevBtn = page.locator("li[class*='pagination-previous']");
        btnAfterPrev = page.locator("li[class*='pagination-previous'] + li + li");
        btnBeforeNext = page.locator("//li[contains(@class, 'pagination-next')]/preceding-sibling::li[1]/a");
    }

    public String getAvailableBalance() {
        return balanceLocator.innerText();
    }
}
