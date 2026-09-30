package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import com.smartaccess.automation.util.ExecutionStop;

import java.util.List;

public class ProfilePage extends BasePage {
    private static final By PROFILE_BUTTON = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[1]");
    private static final By USER_NAME = AppiumBy.xpath(
            "//android.view.View[@content-desc='Yaswanth Asapu']");
    private static final By LOGOUT_BUTTON = AppiumBy.xpath(
            "//android.widget.ImageView[@content-desc='Logout']");
    private static final By LOGOUT_CONFIRM = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Logout']");

    public ProfilePage(AndroidDriver driver) {
        super(driver);
    }

    public void logoutFromSitesList() {
        logoutFromSitesList("Logged out. Execution ended.");
    }

    public void logoutFromSitesList(String reason) {
        click(PROFILE_BUTTON);
        logoutAndEndExecution(reason);
    }

    public void logoutAndEndExecution(String reason) {
        logout(reason);
        ExecutionStop.afterLogout(reason);
    }

    public void logoutAfterSuccessfulDoorOpen() {
        logoutAfterDoorResult("Vault opened successfully. Logged out. Execution ended.");
    }

    public void logoutAfterDoorResult(String reason) {
        ExecutionStop.requestStop(reason);
        click(PROFILE_BUTTON);
        // Return normally so the completed access flow is reported as passed.
        logout(reason);
    }

    private void logout(String reason) {
        ExecutionStop.requestStop(reason);
        click(LOGOUT_BUTTON);
        // Some builds log out immediately; others show a confirmation dialog.
        LogoutState state = wait.until(ignored -> {
            if (isLoggedOut()) return LogoutState.LOGGED_OUT;
            if (driver.findElements(LOGOUT_CONFIRM).stream().anyMatch(WebElement::isDisplayed))
                return LogoutState.CONFIRMATION;
            return null;
        });
        if (state == LogoutState.CONFIRMATION) click(LOGOUT_CONFIRM);
        wait.until(ignored -> isLoggedOut());
        captureStep("Logged_Out");
    }

    private boolean isLoggedOut() {
        return (driver.findElements(AppiumBy.accessibilityId("Base URL")).stream()
                .anyMatch(WebElement::isDisplayed)
                && driver.findElements(AppiumBy.accessibilityId("Apply")).stream()
                .anyMatch(WebElement::isDisplayed))
                || driver.findElements(AppiumBy.className("android.widget.EditText")).stream()
                .filter(WebElement::isDisplayed).count() >= 2
                || driver.findElements(AppiumBy.accessibilityId("Account ID")).stream()
                .anyMatch(WebElement::isDisplayed);
    }

    private enum LogoutState { LOGGED_OUT, CONFIRMATION }

    public void openVerifyUserAndClickLogout() {
        captureStep("04_Before_Click_Profile");
        click(PROFILE_BUTTON);

        WebElement userName = wait.until(ignored -> {
            List<WebElement> matches = driver.findElements(USER_NAME);
            return matches.isEmpty() ? null : matches.getFirst();
        });
        Assert.assertEquals(userName.getAttribute("content-desc"), "Yaswanth Asapu");
        captureStep("05_Profile_Yaswanth_Asapu");

        logoutAndEndExecution("Profile logout completed. Execution ended.");
    }
}
