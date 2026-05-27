package reports;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ExtentManager {

    private static ExtentReports extent;

    public static ExtentReports getInstance() {

        if (extent == null) {

            ReportCleanupUtil.cleanupOldExecutionArtifacts();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());

            String reportPath = "test-output/FinalExecutionReport_" + timestamp + ".html";

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);

            // ===== UI CONFIG =====
            spark.config().setDocumentTitle("Automation Execution Report");
            spark.config().setReportName("Defect Candidate Dashboard");
            spark.config().setTheme(Theme.DARK); // DARK looks cleaner
            spark.config().setTimeStampFormat("dd MMM yyyy HH:mm:ss");
            spark.config().setCss(
                    ".badge-primary { background-color: #1e90ff; }" +
                            ".badge-success { background-color: #28a745; }" +
                            ".badge-danger { background-color: #dc3545; }"
            );
            spark.config().setJs(
                    "document.body.style.zoom='90%';"
            );

            spark.config().setTimelineEnabled(true);

            extent = new ExtentReports();
            extent.attachReporter(spark);

            // ===== SYSTEM INFO =====
            extent.setSystemInfo("Tester", "Bottle");
            extent.setSystemInfo("Project", "PWA Automation");
            extent.setSystemInfo("Module", "Recharge / Passbook");
            extent.setSystemInfo("Environment", "QA / UAT");
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
            extent.setSystemInfo("Execution Time", timestamp);
        }

        return extent;
    }
}
