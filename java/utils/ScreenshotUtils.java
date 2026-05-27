package utils;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenshotUtils {

    public static String captureScreenshot(Page page, String testName) {

        if (page == null) {
            return null;
        }

        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss")
                        .format(new Date());

        String folderPath =
                "test-output/screenshots";

        String fileName =
                testName + "_" + timeStamp + ".png";

        String fullFilePath =
                folderPath + File.separator + fileName;

        try {

            // Create folder if missing
            Path folder = Paths.get(folderPath);

            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }

            // Capture screenshot
            page.screenshot(
                    new Page.ScreenshotOptions()
                            .setPath(Paths.get(fullFilePath))
                            .setFullPage(true)
            );

            // Relative path for Extent Reports
            return "screenshots/" + fileName;

        } catch (IOException | PlaywrightException e) {

            // Browser/page already closed
            return null;
        }
    }
}
