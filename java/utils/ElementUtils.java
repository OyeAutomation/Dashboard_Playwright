package utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.sql.*;
import java.util.Random;

public class ElementUtils {

    private final Page page;
    private final Locator optionalPanelCloseBtn;
    private final Locator optionalPanel;
    private final Locator loaderLocator;

    public ElementUtils(Page page) {
        this.page = page;
        this.optionalPanelCloseBtn = page.locator("button.theme_button");
        this.optionalPanel = page.locator("div.customModalDiv");
        this.loaderLocator = page.locator("div.ngx-spinner-overlay");
    }

    public static final String[] SQLI = {
            "' OR 1=1 --", "\" OR 1=1 --", "'", "\")",
            "' UNION SELECT @@version, NULL --", "' AND 1=1/0 --",
            "' OR (SELECT 1 FROM (SELECT(SLEEP(5)))a) --", "'; SELECT 1; --",
            "SLEEP(5) /*' or SLEEP(5) or '\" or SLEEP(5) or \"*/"
    };

    /**
     * Click a locator with retry logic for stale or intercepted elements
     */
    public void doClick(Locator locator) {
        Locator target = locator.first();

        // Playwright automatically waits up to its global timeout for actionability.
        // If it fails here, your application genuinely has an overlay blocking the user path.
        try {
            target.click();
        } catch (PlaywrightException e) {
            throw new AssertionError("Actionability check failed. Element is blocked or detached: " + locator.toString(), e);
        }
    }

    /**
     * Hover over the target locator and perform a sequential click action
     */
    public void hoverAndClick(Locator hoverLocator, Locator clickLocator) {
        for (int i = 0; i < 5; i++) {
            try {
                hoverLocator.first().hover();
                clickLocator.first().click();
                return;
            } catch (PlaywrightException e) {
                try {
                    loaderLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
                } catch (Exception ignored) {}
            }
        }
        hoverLocator.first().hover();
        clickLocator.first().click(new Locator.ClickOptions().setForce(true));
    }

    /**
     * Send keys to a locator via fill interaction with wait conditions
     */
    public void sendKeys(Locator locator, String value) {
        Locator target = locator.first();

        try {
            // Wait for element to be visible before attempting to fill (optimized timeout)
            target.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            // Clear existing content and fill with new value
            target.fill(value);
        } catch (PlaywrightException e) {
            try {
                // Retry: Wait for loader to be hidden and try again
                loaderLocator.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(3000));
                target.fill(value);
            } catch (Exception ex) {
                throw new AssertionError("Unable to send keys to element: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Clear the target input field area with wait conditions
     */
    public void clearField(Locator locator) {
        Locator target = locator.first();

        try {
            // Wait for element to be visible before attempting to clear (optimized timeout)
            target.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            target.clear();
        } catch (PlaywrightException e) {
            try {
                // Retry: Wait for loader to be hidden and try again
                loaderLocator.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(3000));
                target.clear();
            } catch (Exception ex) {
                throw new AssertionError("Unable to clear field: " + e.getMessage(), ex);
            }
        }
    }

    /**
     * Get inner text value from a specific locator
     */
    public String doGetText(Locator locator) {
        locator.waitFor(
                new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15000)
        );

        return locator.innerText().trim();
    }

    /**
     * Get field execution value attribute safely from locator context
     */
    public String getFieldText(Locator locator) {
        return locator.inputValue();
    }

    /**
     * Verify if the structural locator element state resolves to enabled
     */
    public boolean isBtnEnabled(Locator locator) {
        return locator.isEnabled();
    }

    /**
     * Validate if element matches criteria constraints and is visible inside DOM context tree
     */
    public boolean isElementExist(Locator locator) {
        return locator.count() > 0;
    }

    /**
     * Check visibility thresholds for systemic cooldown notifications using locator tracks
     */
    public boolean isOtpCooldownToasterVisible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate random integer distribution values bounded by minimum and maximum thresholds
     */
    public int randomInt(int max, int min) {
        Random rand = new Random();
        return rand.nextInt(max - min + 1) + min;
    }

    /**
     * Resolve layout balances strings and parse cleanly back to numeric type values
     */
    public long getCurrentBalance(Locator locator) {
        String startBalance = doGetText(locator);
        String numericString = startBalance.replaceAll("[^0-9]", "");
        return Long.parseLong(numericString);
    }

    /**
     * Direct query sequence execution framework mapping secure token attributes from master db log
     */
    public String getLatestOtpFromDb(String adminId) throws SQLException {
        String dbUrl = ConfigReader.getProperty("db.url");
        String dbUsername = ConfigReader.getProperty("db.username");
        String dbPassword = ConfigReader.getProperty("db.password");
        String query = "SELECT otp FROM tbl_client_otp WHERE adminId = ? AND status = 1 ORDER BY createdDate DESC LIMIT 1";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, adminId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("otp");
                }
            }
        }
        return null;
    }

    /**
     * Verify tracking values distributions against primary row coordinates components
     */
    public boolean isTextPresentInFirstRow(String expectedText) {
        if (expectedText == null || expectedText.trim().isEmpty()) {
            return false;
        }

        String cleanedExpected = expectedText.trim();
        Locator firstRow = page.locator("//table/tbody/tr[1]");

        try {
            // Wait for the row container to be present and visible
            firstRow.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));

            // Grab the complete plain text inside that row
            String rowText = firstRow.innerText();

            /*
             * Robust boundary logic:
             * Instead of relying on \b (which breaks on characters like ₹, +, -),
             * we look for the number as long as it isn't directly preceded or followed
             * by another digit. This prevents matching 2026 or 2079, but allows +20, ₹20, 20.00, etc.
             */
            String regex = "(?<!\\d)" + java.util.regex.Pattern.quote(cleanedExpected) + "(?!\\d)";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);

            boolean matchFound = pattern.matcher(rowText).find();

            if (!matchFound) {
                // Log what was actually present to make debugging simple if it fails for other reasons
                System.err.println("Text matching failed. Expected pattern matching for: '" + cleanedExpected + "'");
                System.err.println("Actual text content in row 1 was: [" + rowText.replace("\n", " | ") + "]");
            }

            return matchFound;

        } catch (com.microsoft.playwright.TimeoutError e) {
            System.err.println("First row failed to appear or stabilize within the timeout window.");
            return false;
        } catch (Exception e) {
            System.err.println("Unexpected execution failure checking row data: " + e.getMessage());
            return false;
        }
    }
    /**
     * Enforce systemic dropdown configurations option resets
     */
    public void resetEntryChange(Locator locator) {
        locator.selectOption("10");
    }

    /**
     * Intercept framing block cascades and process target execution calls cleanly
     */
    public void safeClick(Locator locator, Locator disturbingFrameLocator) {
        for (int i = 0; i < 3; i++) {
            try {
                doClick(locator);
                break;
            } catch (PlaywrightException e) {
                try {
                    disturbingFrameLocator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(3000));
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Intercept and manage transient informational model panel layer closures
     */
    public void handleOptionalPanel() {
        for (int i = 0; i < 10; i++) {
            try {
                if (optionalPanel.isVisible()) {

                    optionalPanelCloseBtn.first().waitFor(
                            new Locator.WaitForOptions()
                                    .setState(WaitForSelectorState.VISIBLE)
                                    .setTimeout(2000)
                    );

                    try {
                        optionalPanelCloseBtn.first().click();
                    } catch (PlaywrightException e) {
                        optionalPanelCloseBtn.first().click(
                                new Locator.ClickOptions().setForce(true)
                        );
                    }

                    page.waitForTimeout(500);

                    if (!optionalPanel.isVisible()) {
                        return;
                    }
                }

                page.waitForTimeout(500);

            } catch (Exception ignored) {
            }
        }
    }
        public static void executeUpdate(String query) {
            try (Connection conn = DriverManager.getConnection(ConfigReader.getProperty("db.url"), ConfigReader.getProperty("db.username"), ConfigReader.getProperty("db.password"));
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(query);
                System.out.println("SQL Query executed successfully.");
            } catch (Exception e) {
                System.err.println("Database query failed: " + e.getMessage());
        }
    }

}
