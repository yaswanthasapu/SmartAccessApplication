package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class ProfileRecoveryPage extends BasePage {
    private static final By PROFILE_FROM_FRS = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View");
    private static final By REVOKED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Revoked']");
    private static final By APPROVED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Approved']");
    private static final By ADD_IMAGE = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[2]/android.widget.ImageView[1]");
    private static final By CAMERA_CAPTURE = AppiumBy.xpath("//android.widget.Button");
    private static final By SUBMIT_CAPTURE = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[2]");
    private static final By SUBMITTED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Profile submitted for approval']");
    private static final By OK = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='OK']");
    private static final By CLOSE_PROFILE = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[1]");
    private static final By LOGOUT_IMAGE = AppiumBy.xpath(
            "//android.widget.ImageView[@content-desc='Logout']");
    private static final By LOGOUT_CONFIRM = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Logout']");

    private final WebDriverWait recoveryWait;

    public ProfileRecoveryPage(AndroidDriver driver) {
        super(driver);
        recoveryWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public boolean recoverRevokedProfile(String profileUserName, String approvedAction) {
        openProfile(profileUserName);
        WebElement status = recoveryWait.until(ignored -> firstDisplayed(REVOKED));
        Assert.assertEquals(status.getAttribute("content-desc"), "Revoked");
        captureStep("12_Profile_Status_Revoked");

        click(ADD_IMAGE);
        WebElement capture = recoveryWait.until(ignored -> firstClickable(CAMERA_CAPTURE));
        clickElement(capture, "ProfileRecoveryPage click");
        captureStep("13_Profile_Selfie_Captured");
        click(SUBMIT_CAPTURE);

        Assert.assertNotNull(recoveryWait.until(ignored -> firstDisplayed(SUBMITTED)),
                "Profile submission confirmation was not displayed.");
        captureStep("14_Profile_Submitted_For_Approval");
        click(OK);
        click(CLOSE_PROFILE);

        openProfile(profileUserName);
        WebElement approved = waitForApprovedStatus();
        if (approved != null) {
            captureStep("15_Profile_Status_Approved");
            if ("logout".equalsIgnoreCase(approvedAction)) logout();
            else click(CLOSE_PROFILE);
            return true;
        }

        captureStep("15_Profile_Not_Approved");
        logout();
        return false;
    }

    private void openProfile(String profileUserName) {
        click(PROFILE_FROM_FRS);
        By userName = AppiumBy.xpath(
                "//android.view.View[@content-desc=" + xpathLiteral(profileUserName) + "]");
        Assert.assertNotNull(recoveryWait.until(ignored -> firstDisplayed(userName)),
                "Profile screen did not display the configured user name.");
    }

    private WebElement waitForApprovedStatus() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ignored -> firstDisplayed(APPROVED));
        } catch (org.openqa.selenium.TimeoutException ignored) {
            return null;
        }
    }

    private void logout() {
        new ProfilePage(driver).logoutAndEndExecution("Logged out after profile recovery. Execution ended.");
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : driver.findElements(locator))
            if (element.isDisplayed()) return element;
        return null;
    }

    private WebElement firstClickable(By locator) {
        for (WebElement element : driver.findElements(locator))
            if (element.isDisplayed() && element.isEnabled()) return element;
        return null;
    }

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        throw new IllegalArgumentException("Unsupported XPath value: " + value);
    }
}
