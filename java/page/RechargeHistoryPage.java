package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class RechargeHistoryPage {

    private final Page page;

    @Getter private final Locator rechargeHistoryBtn;
    @Getter private final Locator exportBtn;
    @Getter private final Locator rechargeTypeField;
    @Getter private final Locator fromDateFilter;
    @Getter private final Locator toDateFilter;
    @Getter private final Locator filterApplyBtn;
    @Getter private final Locator filterResetBtn;
    @Getter private final Locator tableRowLocator;
    @Getter private final Locator noDataText;
    @Getter private final Locator rechargeHistoryBackBtn;
    @Getter private final Locator toastMessage;
    @Getter private final Locator loader;
    @Getter private final Locator paginationLocator;
    @Getter private final Locator pageNextBtn;
    @Getter private final Locator pagePrevBtn;
    @Getter private final Locator btnAfterPrev;
    @Getter private final Locator btnBeforeNext;
    @Getter private final Locator pageCount;
    @Getter private final Locator dataEntryChange;
    @Getter private final Locator filterSearchField;
    @Getter private final Locator filterSearchBtn;
    @Getter private final Locator resetBtn;
    @Getter private final Locator tableRows;

    public RechargeHistoryPage(Page page) {
        this.page = page;
        rechargeHistoryBtn = page.locator("#viewRechargeHistory");
        exportBtn = page.locator("//button[normalize-space()='Export']");
        // Numeric HTML ids are invalid CSS (#255); use attribute selector.
        rechargeTypeField = page.locator("[id=\"255\"]");
        fromDateFilter = page.locator("#fromInvoiceDataFilter");
        toDateFilter = page.locator("#toDateInvoiceFilter");
        filterApplyBtn = page.locator("//button[contains(text(), 'Apply')]");
        filterResetBtn = page.locator("//button[contains(text(), 'Reset')]");
        tableRowLocator = page.locator("//table/tbody/tr");
        noDataText = page.locator("//h3[contains(text(), 'No Data Found')]");
        rechargeHistoryBackBtn = page.locator("span[class='fs-6 c-pointer']");
        toastMessage = page.locator("#toast-container .toast-message").last();
        loader = page.locator("div.ngx-spinner-overlay");
        paginationLocator = page.locator("nav[aria-label='Pagination']");
        pageNextBtn = page.locator("li[class*='pagination-next']");
        pagePrevBtn = page.locator("li[class*='pagination-previous']");
        btnAfterPrev = page.locator("li[class*='pagination-previous'] + li + li");
        btnBeforeNext = page.locator("//li[contains(@class,'pagination-next')]/preceding-sibling::li[1]//a");
        pageCount = page.locator("p.text-dark:has-text('of')");
        dataEntryChange = page.locator("#showtableDataEntries2");
        filterSearchField = page.locator("#searchRecharge");
        filterSearchBtn = page.locator("#searchText3");
        resetBtn = page.locator("#resetSearch3");
        tableRows = page.locator("table.animate__fadeIn tbody tr");
    }
}
