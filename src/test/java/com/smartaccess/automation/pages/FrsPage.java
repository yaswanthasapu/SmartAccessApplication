package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.function.Supplier;

public final class FrsPage extends BasePage {
    private static final String FAILURE_MESSAGE = "Face not recognized. Please ensure your face is " +
            "clearly visible and matches your registered profile.";
    private static final String SUCCESS_MESSAGE = "Face authentication completed successfully";

    private static final By PHOTO_OK = AppiumBy.xpath(
            "//android.widget.ImageView[@content-desc='OK']");
    private static final By RETAKE = AppiumBy.xpath(
            "//android.widget.ImageView[@content-desc='Retake']");
    private static final By FAILED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Failed']");
    private static final By FAILURE_TEXT = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + FAILURE_MESSAGE + "']");
    private static final By SUCCESS = AppiumBy.xpath(
            "//android.view.View[@content-desc='Success']");
    private static final By SUCCESS_TEXT = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + SUCCESS_MESSAGE + "']");
    private static final By DIALOG_OK = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='OK']");

    private final WebDriverWait frsWait;

    public FrsPage(AndroidDriver driver) {
        super(driver);
        // FRS requires a real person to align their face and blink, so allow
        // enough time for manual interaction on a physical device.
        frsWait = new WebDriverWait(driver, Duration.ofMinutes(3));
    }

    public void authenticateWithOneRetry() {
        submitCapturedFace("11_FRS_First_Photo_Review");
        if (waitForOutcome() == Outcome.FAILED) {
            verifyFailureAndRetry();
            submitCapturedFace("12_FRS_Retry_Photo_Review");
            Assert.assertEquals(waitForOutcome(), Outcome.SUCCESS,
                    "FRS face authentication failed after one retry.");
        }
        verifySuccessAndDismiss();
    }

    public boolean authenticateWithProfileRecovery(Supplier<Boolean> profileRecovery) {
        submitCapturedFace("11_FRS_First_Photo_Review");
        if (waitForOutcome() == Outcome.FAILED) {
            verifyFailureAndDismiss();
            profileRecovery.get();
            return false;
        }
        verifySuccessAndDismiss();
        return true;
    }

    private void submitCapturedFace(String screenshotName) {
        WebElement ok = frsWait.until(ignored -> firstDisplayed(PHOTO_OK));
        captureStep(screenshotName);
        clickElement(ok, "FrsPage click");
    }

    private Outcome waitForOutcome() {
        return frsWait.until(ignored -> {
            if (firstDisplayed(SUCCESS) != null) return Outcome.SUCCESS;
            if (firstDisplayed(FAILED) != null) return Outcome.FAILED;
            return null;
        });
    }

    private void verifyFailureAndRetry() {
        verifyFailureAndDismiss();
        WebElement retake = frsWait.until(ignored -> firstDisplayed(RETAKE));
        clickElement(retake, "FrsPage click");
        captureStep("12_FRS_Retake_Started");
    }

    private void verifyFailureAndDismiss() {
        WebElement failure = frsWait.until(ignored -> firstDisplayed(FAILURE_TEXT));
        Assert.assertEquals(failure.getAttribute("content-desc"), FAILURE_MESSAGE);
        captureStep("12_FRS_Face_Not_Recognized");
        click(DIALOG_OK);
    }

    private void verifySuccessAndDismiss() {
        Assert.assertNotNull(firstDisplayed(SUCCESS), "FRS success heading was not displayed.");
        WebElement successText = frsWait.until(ignored -> firstDisplayed(SUCCESS_TEXT));
        Assert.assertEquals(successText.getAttribute("content-desc"), SUCCESS_MESSAGE);
        captureStep("13_FRS_Face_Authentication_Success");
        click(DIALOG_OK);
        captureStep("14_After_FRS_Success_OK");
    }

    private WebElement firstDisplayed(By locator) {
        new AlertPage(driver).dismissDoorAlertIfPresent();
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return element;
            } catch (StaleElementReferenceException ignored) {
                // Flutter rebuilt the semantics tree between lookup and inspection.
            }
        }
        return null;
    }

    private enum Outcome { SUCCESS, FAILED }
}
