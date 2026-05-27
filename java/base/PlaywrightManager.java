package base;

import com.microsoft.playwright.*;
import lombok.Getter;
import utils.ConfigReader;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class PlaywrightManager {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;

    @Getter
    protected Page page;

    // Per-thread browser state
    private static final ThreadLocal<Page> PAGE_THREAD = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT_THREAD = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER_THREAD = new ThreadLocal<>();
    private static final ThreadLocal<Playwright> PLAYWRIGHT_THREAD = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CLASS_SLOT_ACQUIRED = ThreadLocal.withInitial(() -> false);

    // Max 3 active test classes at once
    private static final Semaphore CLASS_SLOT_LIMIT = new Semaphore(3, true);

    // Use this to serialize OTP/login setup
    private static final ReentrantLock OTP_LOCK = new ReentrantLock(true);
    private static final AtomicBoolean SESSION_STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_IN = new AtomicBoolean(false);

    public static void acquireClassSlot() throws InterruptedException {
        CLASS_SLOT_LIMIT.acquire();
        CLASS_SLOT_ACQUIRED.set(true);
    }

    public static void releaseClassSlot() {
        if (Boolean.TRUE.equals(CLASS_SLOT_ACQUIRED.get())) {
            CLASS_SLOT_LIMIT.release();
            CLASS_SLOT_ACQUIRED.remove();
        }
    }

    public static void runWithOtpLock(Runnable action) {
        OTP_LOCK.lock();
        try {
            action.run();
        } finally {
            OTP_LOCK.unlock();
        }
    }

    public void startBrowser() {
        boolean headless = ConfigReader.getBoolean("headless", false);
        String browserName = ConfigReader.getProperty("browser", "chrome");

        playwright = Playwright.create();
        PLAYWRIGHT_THREAD.set(playwright);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setArgs(List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--start-maximized"
                ));

        switch (browserName.toLowerCase()) {
            case "chrome":
                browser = playwright.chromium().launch(launchOptions.setChannel("chrome"));
                break;
            case "chromium":
                browser = playwright.chromium().launch(launchOptions);
                break;
            case "firefox":
                browser = playwright.firefox().launch(launchOptions);
                break;
            case "webkit":
                browser = playwright.webkit().launch(launchOptions);
                break;
            default:
                throw new IllegalArgumentException("Unsupported browser: " + browserName);
        }
        BROWSER_THREAD.set(browser);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setAcceptDownloads(true)
                .setIgnoreHTTPSErrors(true);

        if (headless) {
            contextOptions.setViewportSize(1920, 1080);
        } else {
            contextOptions.setViewportSize(null);
        }

        context = browser.newContext(contextOptions);
        CONTEXT_THREAD.set(context);

        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));

        page = context.newPage();
        PAGE_THREAD.set(page);

        page.setDefaultTimeout(10000);
        int pageLoadMs = ConfigReader.getInt("pageLoad.timeout.ms", -1);
        if (pageLoadMs < 0) {
            pageLoadMs = ConfigReader.getInt("pageLoad.timeout.seconds", 60) * 1000;
        }
        page.setDefaultNavigationTimeout(pageLoadMs);
        page.navigate(ConfigReader.getProperty("devUrl"));
    }

    public void closeBrowser() {
        Page currentPage = page != null ? page : PAGE_THREAD.get();
        BrowserContext currentContext = context != null ? context : CONTEXT_THREAD.get();
        Browser currentBrowser = browser != null ? browser : BROWSER_THREAD.get();
        Playwright currentPlaywright = playwright != null ? playwright : PLAYWRIGHT_THREAD.get();

        try {
            if (currentContext != null) {
                String tracePath = "test-output/traces/trace_" + System.currentTimeMillis() + ".zip";
                currentContext.tracing().stop(new Tracing.StopOptions().setPath(Path.of(tracePath)));
            }
        } catch (Exception ignored) {
        }

        try {
            if (currentPage != null) {
                currentPage.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (currentContext != null) {
                currentContext.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (currentBrowser != null) {
                currentBrowser.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (currentPlaywright != null) {
                currentPlaywright.close();
            }
        } catch (Exception ignored) {
        }

        PAGE_THREAD.remove();
        CONTEXT_THREAD.remove();
        BROWSER_THREAD.remove();
        PLAYWRIGHT_THREAD.remove();

        page = null;
        context = null;
        browser = null;
        playwright = null;
    }

    public synchronized void startBrowserOnce() {
        if (SESSION_STARTED.compareAndSet(false, true)) {
            startBrowser();
        } else {
            this.playwright = currentPlaywright();
            this.browser = currentBrowser();
            this.context = currentContext();
            this.page = currentPage();
        }
    }

    public synchronized void closeBrowserOnce() {
        if (SESSION_STARTED.compareAndSet(true, false)) {
            closeBrowser();
            LOGGED_IN.set(false);
        }
    }

    public static boolean isLoggedIn() {
        return LOGGED_IN.get();
    }

    public static void markLoggedIn() {
        LOGGED_IN.set(true);
    }

    public void resetToHomeScreen() {
        if (page == null) {
            page = currentPage();
        }
        if (page != null && !page.isClosed()) {
            page.navigate(ConfigReader.getProperty("devUrl") + "#/xiurnaipx");
        }
    }

    public static Page currentPage() {
        return PAGE_THREAD.get();
    }

    public static void updateCurrentPage(Page newPage) {
        PAGE_THREAD.set(newPage);
    }

    public static BrowserContext currentContext() {
        return CONTEXT_THREAD.get();
    }

    public static Browser currentBrowser() {
        return BROWSER_THREAD.get();
    }

    public static Playwright currentPlaywright() {
        return PLAYWRIGHT_THREAD.get();
    }
}
