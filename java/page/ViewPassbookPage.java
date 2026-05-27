package page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import lombok.Getter;

public class ViewPassbookPage {

    private final Page page;

    @Getter private final Locator vpCostCenterField;
    @Getter private final Locator vpTransactionField;
    @Getter private final Locator vpFromDate;
    @Getter private final Locator vpToDate;
    @Getter private final Locator vpApplyBtn;
    @Getter private final Locator vpFilterResetBtn;
    @Getter private final Locator vpExportBtn;
    @Getter private final Locator viewPassbookBtn;
    @Getter private final Locator viewPassbookBackBtn;
    @Getter private final Locator toastMessage;
    @Getter private final Locator noDataText;
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

    public ViewPassbookPage(Page page) {
        this.page = page;
        vpCostCenterField = page.locator("#costCenterSelect");
        vpTransactionField = page.locator("#transactionSelect");
        vpFromDate = page.locator("#fromDateFilter");
        vpToDate = page.locator("#toDateFilter");
        vpApplyBtn = page.locator("#applyFilter");
        vpFilterResetBtn = page.locator("#resetFilters");
        vpExportBtn = page.locator("#exporttableData");
        viewPassbookBtn = page.locator("#viewPassbook");
        viewPassbookBackBtn = page.locator("span[class='fs-6 c-pointer']");
        toastMessage = page.locator("#toast-container .toast-message").last();
        noDataText = page.locator("h3:has-text('No Data Found')");
        loader = page.locator("div.ngx-spinner-overlay").first();
        paginationLocator = page.locator("nav[aria-label='Pagination']");
        pageNextBtn = page.locator("li[class*='pagination-next']");
        pagePrevBtn = page.locator("li[class*='pagination-previous']");
        btnAfterPrev = page.locator("li[class*='pagination-previous'] + li + li");
        btnBeforeNext = page.locator("//li[contains(@class, 'pagination-next')]/preceding-sibling::li[1]/a");
        pageCount = page.locator("p.text-dark");
        dataEntryChange = page.locator("#tableDataShowEntries");
        filterSearchField = page.locator("#seachPassbook");
        filterSearchBtn = page.locator("#searchText2");
        resetBtn = page.locator("#resetSearch2");
        tableRows = page.locator("table.animate__fadeIn tbody tr").first();
    }
}
