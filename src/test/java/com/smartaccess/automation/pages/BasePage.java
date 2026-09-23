package com.smartaccess.automation.pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public abstract class BasePage {
    protected final AndroidDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    protected void enterText(By locator, String value) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        element.click();
        element.clear();
        element.sendKeys(value);
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected WebElement waitForClickability(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void hideKeyboardIfVisible() {
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // The keyboard was already hidden.
        }
    }

    protected void captureStep(String stepName) {
        String safeName = stepName.replaceAll("[^A-Za-z0-9_.-]", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path output = Path.of("screenshots", "steps").toAbsolutePath().normalize();
        try {
            Files.createDirectories(output);
            Path source = driver.getScreenshotAs(OutputType.FILE).toPath();
            Files.copy(source, output.resolve(safeName + "-" + timestamp + ".png"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {
            // A screenshot failure must not hide the actual test result.
        }
    }
}
