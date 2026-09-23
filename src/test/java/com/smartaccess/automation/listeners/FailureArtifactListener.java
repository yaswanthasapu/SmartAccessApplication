package com.smartaccess.automation.listeners;

import com.smartaccess.automation.config.ConfigLoader;
import com.smartaccess.automation.tests.BaseTest;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FailureArtifactListener implements ITestListener {
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Override
    public void onTestFailure(ITestResult result) {
        if (!(result.getInstance() instanceof BaseTest test) || test.getDriver() == null) return;
        AndroidDriver driver = test.getDriver();
        String name = result.getTestClass().getRealClass().getSimpleName() + "-"
                + result.getMethod().getMethodName() + "-" + LocalDateTime.now().format(STAMP);
        Path output = ConfigLoader.projectRoot().resolve("screenshots");
        try {
            Files.createDirectories(output);
            Path screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(screenshot, output.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(output.resolve(name + ".xml"), driver.getPageSource(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // Artifact capture must not hide the original test failure.
        }
    }
}
