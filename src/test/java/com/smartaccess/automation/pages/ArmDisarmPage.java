package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;

public class ArmDisarmPage extends BasePage {
    private static final String VAULT_BLOCKED_MESSAGE =
            "Operation not allowed: device is online and armed for configType=VAULT";
    private static final By VAULT_BLOCKED = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + VAULT_BLOCKED_MESSAGE + "']");
    private static final By ARM_DISARM = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[2]/android.view.View[5]");
    private static final By MAIN_DOOR_ALERT = AppiumBy.xpath(
            "//*[contains(@content-desc, 'Please close Main door') " +
                    "or contains(@text, 'Please close Main door')]");
    private static final By DEACTIVATE_MESSAGE = AppiumBy.xpath(
            "//android.view.View[@content-desc='Would you like to deactivate " +
                    "MAFIN-DEMO-HYD-TEST-BPIM5 ARM mode?']");
    private static final By ACTIVATE_MESSAGE = AppiumBy.xpath(
            "//android.view.View[@content-desc='Would you like to activate " +
                    "MAFIN-DEMO-HYD-TEST-BPIM5 ARM mode?']");
    private static final By CONFIRM = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Confirm']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");

    private static final List<By> ANDROID_PERMISSION_BUTTONS = List.of(
            AppiumBy.id("com.android.permissioncontroller:id/permission_allow_foreground_only_button"),
            AppiumBy.id("com.android.permissioncontroller:id/permission_allow_one_time_button"),
            AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button")
    );

    public ArmDisarmPage(AndroidDriver driver) {
        super(driver);
    }

    public boolean deactivateArm(boolean confirmDeactivation) {
        if (!isVaultOperationBlocked()) return false;
        captureStep("08_Vault_Operation_Blocked_While_Armed");
        openArmDisarmFrs().authenticateWithOneRetry();
        chooseArmModeChange(DEACTIVATE_MESSAGE,
                "Would you like to deactivate MAFIN-DEMO-HYD-TEST-BPIM5 ARM mode?",
                confirmDeactivation, "Deactivate");
        return true;
    }

    public boolean activateArm() {
        if (isVaultOperationBlocked()) return false;
        captureStep("08_System_Disarmed_Before_Activation");
        openArmDisarmFrs().authenticateWithOneRetry();
        chooseArmModeChange(ACTIVATE_MESSAGE,
                "Would you like to activate MAFIN-DEMO-HYD-TEST-BPIM5 ARM mode?",
                true, "Activate");
        return true;
    }

    public boolean isVaultOperationBlocked() {
        WebDriverWait messageWait = new WebDriverWait(driver, Duration.ofSeconds(10));
        try {
            return messageWait.until(ignored -> !driver.findElements(VAULT_BLOCKED).isEmpty());
        } catch (Exception ignored) {
            return false;
        }
    }

    private FrsPage openArmDisarmFrs() {
        List<WebElement> mainDoorAlerts = driver.findElements(MAIN_DOOR_ALERT);
        if (!mainDoorAlerts.isEmpty()) {
            captureStep("09_FRS_Blocked_Main_Door_Open");
            List<WebElement> okButtons = driver.findElements(AppiumBy.accessibilityId("OK"));
            if (!okButtons.isEmpty()) okButtons.getFirst().click();
            Assert.fail("FRS cannot start: Please close Main door. Then Retry.");
        }
        captureStep("09_Before_Click_Arm_Disarm");
        click(ARM_DISARM);
        allowAnyVisibleRuntimePermissions();
        captureStep("10_FRS_Screen");
        return new FrsPage(driver);
    }

    private void chooseArmModeChange(By messageLocator, String expectedMessage,
                                     boolean confirmChange, String actionName) {
        WebElement message = wait.until(ignored -> {
            List<WebElement> matches = driver.findElements(messageLocator);
            return matches.isEmpty() ? null : matches.getFirst();
        });
        Assert.assertEquals(message.getAttribute("content-desc"), expectedMessage);
        captureStep("15_" + actionName + "_ARM_Confirmation");
        click(confirmChange ? CONFIRM : CANCEL);
        captureStep("16_" + actionName + "_ARM_" + (confirmChange ? "Confirmed" : "Cancelled"));
    }

    private void allowAnyVisibleRuntimePermissions() {
        for (int prompt = 0; prompt < 8; prompt++) {
            boolean allowed = false;
            for (By button : ANDROID_PERMISSION_BUTTONS) {
                List<WebElement> matches = driver.findElements(button);
                if (!matches.isEmpty() && matches.getFirst().isDisplayed()) {
                    matches.getFirst().click();
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return;
        }
        Assert.fail("More Android permission prompts appeared than expected.");
    }

    private static final class FrsPage extends BasePage {
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

        private FrsPage(AndroidDriver driver) {
            super(driver);
            frsWait = new WebDriverWait(driver, Duration.ofSeconds(60));
        }

        private void authenticateWithOneRetry() {
            submitCapturedFace("11_FRS_First_Photo_Review");
            if (waitForOutcome() == Outcome.FAILED) {
                verifyFailureAndRetry();
                submitCapturedFace("12_FRS_Retry_Photo_Review");
                Assert.assertEquals(waitForOutcome(), Outcome.SUCCESS,
                        "FRS face authentication failed after one retry.");
            }
            verifySuccessAndDismiss();
        }

        private void submitCapturedFace(String screenshotName) {
            WebElement ok = frsWait.until(ignored -> firstDisplayed(PHOTO_OK));
            captureStep(screenshotName);
            ok.click();
        }

        private Outcome waitForOutcome() {
            return frsWait.until(ignored -> {
                if (firstDisplayed(SUCCESS) != null) return Outcome.SUCCESS;
                if (firstDisplayed(FAILED) != null) return Outcome.FAILED;
                return null;
            });
        }

        private void verifyFailureAndRetry() {
            WebElement failure = frsWait.until(ignored -> firstDisplayed(FAILURE_TEXT));
            Assert.assertEquals(failure.getAttribute("content-desc"), FAILURE_MESSAGE);
            captureStep("12_FRS_Face_Not_Recognized");
            click(DIALOG_OK);
            WebElement retake = frsWait.until(ignored -> firstDisplayed(RETAKE));
            retake.click();
            captureStep("12_FRS_Retake_Started");
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
            for (WebElement element : driver.findElements(locator))
                if (element.isDisplayed()) return element;
            return null;
        }

        private enum Outcome { SUCCESS, FAILED }
    }
}
