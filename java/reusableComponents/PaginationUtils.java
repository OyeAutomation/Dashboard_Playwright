package reusableComponents;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import utils.ElementUtils;

public class PaginationUtils {

    private final Page page;
    private final ElementUtils elementUtils;

    public PaginationUtils(Page page) {
        this.page = page;
        this.elementUtils = new ElementUtils(page);
    }

    public boolean isPaginationVisible(Locator paginationBox) {
        return paginationBox.count() > 0 && paginationBox.isVisible();
    }

    public void resetEntryChange(Locator entryChangeDropdown) {
        entryChangeDropdown.selectOption("10");
    }

    public int getCurrentPageNumber(Locator pageCount) {
        String input = elementUtils.doGetText(pageCount);
        String[] parts = input.split(" ");

        // Safety check to ensure parts array has the expected elements before parsing
        if (parts.length < 2) {
            return 1;
        }

        String str = parts[1];
        int n = 0;
        if (str.length() > 1) {
            String x = str.substring(0, str.length() - 1);
            n = Integer.parseInt(x) + 1;
        } else if (str.length() == 1) {
            n = 1;
        }
        return n;
    }

    public boolean isNextEnabled(Locator nextBtn) {
        return !isDisabled(nextBtn);
    }

    public boolean isPreviousEnabled(Locator prevBtn) {
        return !isDisabled(prevBtn);
    }

    public void clickNextPage(Locator nextBtn, Locator loader) {
        nextBtn.locator("a").click();
        loader.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    public void clickPreviousPage(Locator prevBtn, Locator loader) {
        prevBtn.locator("a").click();
        loader.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    public int lastPageContent(Locator countPage) {
        String input = elementUtils.doGetText(countPage);
        return parseTotalOnPage(input);
    }

    private boolean isDisabled(Locator el) {
        if (!el.isEnabled()) return true;
        if (el.getAttribute("disabled") != null) return true;
        String cls = el.getAttribute("class");
        return cls != null && cls.toLowerCase().contains("disabled");
    }

    private int parseTotalOnPage(String pageCountText) {
        String[] parts = pageCountText.split("\\s+");
        if (parts.length < 4) return 0;
        return Integer.parseInt(parts[3]) - Integer.parseInt(parts[1]) + 1;
    }

    public int selectEntryCountAndParse(Locator locatorEntry, Locator pageCount, int index) {
        locatorEntry.selectOption(new SelectOption().setIndex(index));
        String pageCountText = elementUtils.doGetText(pageCount).trim();
        return parseTotalOnPage(pageCountText);
    }

    public int parseTotalRecords(String pageCountText) {
        String[] parts = pageCountText.split("\\s+");
        if (parts.length < 6) return 0;
        return Integer.parseInt(parts[5]);
    }
}
