package utils;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class WaitUtils {

    private final Page page;

    public WaitUtils(Page page) {
        this.page = page;
    }

    public void waitForInvisibility(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    public List<Locator> waitForAllVisible(Locator locator) {
        // Wait for at least one instance to avoid returning an empty snapshot list prematurely
        locator.first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        return locator.all();
    }

    public void waitForVisibility(Locator locator) {
        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public boolean isBtnEnabled(Locator locator) {
        return locator.isEnabled();
    }

    public void waitForEitherElementToBeVisible(Locator ele1, Locator ele2) {
        ele1.or(ele2).first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    public boolean isClassContains(Locator locator, String attributeName, String expectedValue) {
        String attributeValue = locator.getAttribute(attributeName);
        // Safety guard: prevent NullPointerException if attribute is missing
        return attributeValue != null && attributeValue.contains(expectedValue);
    }

    public Path waitForDownload(Runnable triggerAction, String downloadDir, int timeoutSec) {
        try {
            Files.createDirectories(Paths.get(downloadDir));

            Download download = page.waitForDownload(
                    new Page.WaitForDownloadOptions()
                            .setTimeout(timeoutSec * 1000L),
                    () -> {
                        page.waitForTimeout(1000); // small stability wait
                        triggerAction.run();
                    }
            );

            // wait until download is fully completed
            download.path();

            Path savePath = Paths.get(downloadDir, download.suggestedFilename());

            download.saveAs(savePath);

            return savePath;

        } catch (IOException | PlaywrightException e) {
            throw new RuntimeException(
                    "Failed to handle or save the file download: " + e.getMessage(),
                    e
            );
        }
    }

    public boolean downloadExists(String path, String fileNamePart, long startedAtMillis) {
        Path targetPath = Paths.get(path);

        if (!Files.exists(targetPath)) {
            return false;
        }

        try (Stream<Path> fileStream = Files.list(targetPath)) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().contains(fileNamePart))
                    .anyMatch(file -> {
                        try {
                            return Files.getLastModifiedTime(file).toMillis() >= startedAtMillis;
                        } catch (IOException e) {
                            return false;
                        }
                    });
        } catch (IOException e) {
            return false;
        }
    }

    public long now() {
        return Instant.now().toEpochMilli();
    }
}
