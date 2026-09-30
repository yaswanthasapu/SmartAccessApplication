package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class LoginPage extends BasePage {
    private static final By USERNAME = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(0)");
    private static final By PASSWORD = AppiumBy.androidUIAutomator(
            "new UiSelector().className(\"android.widget.EditText\").instance(1)");
    private static final By PROCEED = AppiumBy.accessibilityId("Proceed");
    private static final By TRY_AGAIN = AppiumBy.androidUIAutomator(
            "new UiSelector().description(\"Try Again\")");

    public LoginPage(AndroidDriver driver) { super(driver); }

    public void login(String username, String password) {
        enterText(USERNAME, username);
        logStep("Username entered");
        hideKeyboardIfVisible();
        enterText(PASSWORD, password);
        logStep("Password entered (value hidden)");
        hideKeyboardIfVisible();
        captureStep("03_Before_Login_Proceed_Click");
        clickElement(waitForClickability(PROCEED), "LoginPage click");
        logStep("Login submitted");
        failWhenTryAgainAppears();
    }

    private void failWhenTryAgainAppears() {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        try {
            WebElement tryAgain = shortWait.until(ExpectedConditions.visibilityOfElementLocated(TRY_AGAIN));
            captureStep("04_Login_Failed_Try_Again_Popup");
            clickElement(tryAgain, "LoginPage click");
            Assert.fail("Login failed: the Try Again popup appeared.");
        } catch (org.openqa.selenium.TimeoutException ignored) {
            // No failure popup appeared; continue to module selection.
        }
    }
}
