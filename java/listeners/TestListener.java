package listeners;

import base.PlaywrightManager;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.microsoft.playwright.Page;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import reports.*;
import utils.ScreenshotUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

public class TestListener implements ITestListener {

    private static final ExtentReports extent =
            ExtentManager.getInstance();

    @Override
    public void onTestStart(ITestResult result) {

        String methodName =
                result.getMethod().getMethodName();

        // Fetch Excel data
        Map<String, String> data =
                ExcelDataManager.getTestData(methodName);

        // Fetch @Test description
        String description = "";

        if (result.getMethod()
                .getConstructorOrMethod()
                .getMethod()
                .getAnnotation(org.testng.annotations.Test.class) != null) {

            description = result.getMethod()
                    .getConstructorOrMethod()
                    .getMethod()
                    .getAnnotation(org.testng.annotations.Test.class)
                    .description();
        }

        // Create test
        String testTitle =
                getTestTitle(methodName, data);

        ExtentTest test =
                extent.createTest(testTitle);

        ExtentTestManager.setTest(test);
        ExtentTestManager.setData(data);

        // Basic info
        test.info("TC_ID: " + methodName);

        if (data != null &&
                data.get("Sub-Module") != null) {

            test.info("Sub-Module: " +
                    data.get("Sub-Module"));
        }

        if (description != null &&
                !description.trim().isEmpty()) {

            test.info("Description: " + description);
        }

        // Expandable details
        if (data != null && !data.isEmpty()) {

            test.info(
                    "<details><summary>View Test Details</summary>"
                            + "<b>Sub-Module:</b> "
                            + safe(data.get("Sub-Module")) + "<br>"

                            + "<b>Priority:</b> "
                            + safe(data.get("Priority")) + "<br>"

                            + "<b>Summary:</b> "
                            + safe(data.get("Summary")) + "<br>"

                            + "<b>Steps:</b><pre>"
                            + safe(data.get("Steps"))
                            + "</pre>"

                            + "<b>Expected:</b> "
                            + safe(data.get("Expected"))

                            + "</details>"
            );
        }
    }

    @Override
    public void onTestSuccess(ITestResult result) {

        ExtentTestManager
                .getTest()
                .pass("Test Passed");

        ExtentTestManager.unload();
    }

    @Override
    public void onTestFailure(ITestResult result) {

        ExtentTest test =
                ExtentTestManager.getTest();

        Throwable error =
                result.getThrowable();

        // Defect classification
        String defectType =
                DefectClassifier.classify(error);

        if (error != null) {
            test.fail(error);
        } else {
            test.fail("Test Failed");
        }

        test.warning(
                "Defect Candidate: " + defectType);

        // Stored test data
        Map<String, String> data =
                ExtentTestManager.getData();

        String tcId =
                result.getMethod().getMethodName();

        String errorMsg =
                (error != null)
                        ? error.toString()
                        : "No Error";

        // Export failure
        try {

            FailureExportUtil.exportFailure(
                    tcId,
                    defectType,
                    errorMsg,
                    getFullLog(result, error),
                    data
            );

        } catch (Exception e) {

            test.warning(
                    "Failure export failed");
        }

        // Screenshot
        Page page =
                extractPage(result);

        String screenshotPath =
                ScreenshotUtils.captureScreenshot(
                        page,
                        tcId
                );

        try {

            if (screenshotPath != null) {

                test.addScreenCaptureFromPath(
                        screenshotPath);

            } else {

                test.warning(
                        "Screenshot unavailable");
            }

        } catch (Exception e) {

            test.warning(
                    "Screenshot attach failed");
        }

        ExtentTestManager.unload();
    }

    @Override
    public void onTestSkipped(
            ITestResult result) {

        ExtentTest test =
                ExtentTestManager.getTest();

        Throwable error =
                result.getThrowable();

        String tcId =
                result.getMethod().getMethodName();

        Map<String, String> data =
                ExtentTestManager.getData();

        if (test == null
                || data == null
                || !tcId.equalsIgnoreCase(
                data.get("TC_ID"))) {

            data =
                    ExcelDataManager.getTestData(tcId);

            test =
                    extent.createTest(
                            getTestTitle(tcId, data));

            ExtentTestManager.setTest(test);
            ExtentTestManager.setData(data);
        }

        if (error != null) {
            test.skip(error);
        } else {
            test.skip("Blocked / Skipped");
        }

        String skipReason =
                (error != null
                        && error.getMessage() != null)
                        ? error.getMessage()
                        : "Blocked / Skipped";

        try {

            FailureExportUtil.exportFailure(
                    tcId,
                    "Skipped / Blocked",
                    skipReason,
                    getFullLog(
                            result,
                            error,
                            "SKIPPED"),
                    data
            );

        } catch (Exception e) {

            test.warning(
                    "Skipped case export failed");
        }

        ExtentTestManager.unload();
    }

    @Override
    public void onFinish(
            ITestContext context) {

        FailureExportUtil.flushFailures();

        extent.flush();

        ExtentTestManager.unload();
    }

    // Playwright page extraction
    private Page extractPage(
            ITestResult result) {

        Object instance =
                result.getInstance();

        if (instance instanceof PlaywrightManager) {

            return ((PlaywrightManager)
                    instance)
                    .getPage();
        }

        return null;
    }

    private String getTestTitle(
            String methodName,
            Map<String, String> data) {

        String subModule =
                data != null
                        ? data.get("Sub-Module")
                        : null;

        if (subModule == null
                || subModule.trim().isEmpty()
                || "-".equals(subModule.trim())) {

            return methodName;
        }

        return methodName
                + " | "
                + subModule;
    }

    private String getFullLog(
            ITestResult result,
            Throwable error) {

        return getFullLog(
                result,
                error,
                "FAILED");
    }

    private String getFullLog(
            ITestResult result,
            Throwable error,
            String status) {

        StringBuilder log =
                new StringBuilder();

        log.append("Suite: ")
                .append(result.getTestContext()
                        .getSuite()
                        .getName())
                .append(System.lineSeparator());

        log.append("Test: ")
                .append(result.getTestContext()
                        .getName())
                .append(System.lineSeparator());

        log.append("Class: ")
                .append(result.getTestClass()
                        .getName())
                .append(System.lineSeparator());

        log.append("Method: ")
                .append(result.getMethod()
                        .getMethodName())
                .append(System.lineSeparator());

        log.append("Status: ")
                .append(status)
                .append(System.lineSeparator());

        log.append("Started: ")
                .append(new Date(
                        result.getStartMillis()))
                .append(System.lineSeparator());

        log.append("Ended: ")
                .append(new Date(
                        result.getEndMillis()))
                .append(System.lineSeparator());

        log.append("Parameters: ")
                .append(Arrays.toString(
                        result.getParameters()))
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        if (error == null) {
            return log.append("No Error")
                    .toString();
        }

        StringWriter sw =
                new StringWriter();

        error.printStackTrace(
                new PrintWriter(sw));

        log.append(sw);

        return log.toString();
    }

    private String safe(String value) {

        return value == null
                ? "N/A"
                : value;
    }
}
