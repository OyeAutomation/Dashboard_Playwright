package reports;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ReportCleanupUtil {

    private static final Path TEST_OUTPUT = Path.of("test-output");
    private static final Path SCREENSHOTS = TEST_OUTPUT.resolve("screenshots");
    private static final int REPORTS_TO_KEEP = Integer.getInteger("reportRetention", 5);
    private static final int SCREENSHOTS_TO_KEEP = Integer.getInteger("screenshotRetention", 50);

    private ReportCleanupUtil() {
    }

    public static void cleanupOldExecutionArtifacts() {
        cleanupByPrefix(TEST_OUTPUT, "FinalExecutionReport_", ".html", REPORTS_TO_KEEP);
        cleanupByPrefix(TEST_OUTPUT, "FailedCases_", ".xlsx", REPORTS_TO_KEEP);
        cleanupByPrefix(SCREENSHOTS, "", ".png", SCREENSHOTS_TO_KEEP);
    }

    private static void cleanupByPrefix(Path directory, String prefix, String suffix, int keepCount) {
        if (keepCount < 0 || !Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.startsWith(prefix) && fileName.endsWith(suffix);
                    })
                    .sorted(Comparator.comparingLong(ReportCleanupUtil::lastModifiedMillis).reversed())
                    .toList();

            for (int i = keepCount; i < files.size(); i++) {
                deleteQuietly(files.get(i));
            }
        } catch (IOException e) {
            System.out.println("Report cleanup skipped for " + directory + ": " + e.getMessage());
        }
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.out.println("Could not delete old execution artifact " + path + ": " + e.getMessage());
        }
    }
}
