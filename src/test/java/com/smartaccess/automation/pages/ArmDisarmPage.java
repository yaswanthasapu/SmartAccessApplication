package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public class ArmDisarmPage extends BasePage {
    private Boolean verifiedBluetoothState;
    private static final String VAULT_BLOCKED_MESSAGE =
            "Operation not allowed: device is online and armed for configType=VAULT";
    private static final By VAULT_BLOCKED = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + VAULT_BLOCKED_MESSAGE + "' or "
                    + "@content-desc='Branch is in ARMED state, cannot perform transaction']");
    private static final By ARM_DISARM = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[2]/android.view.View[5]");
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

    public boolean deactivateArmWithProfileRecovery(boolean confirmDeactivation,
                                                     Supplier<Boolean> profileRecovery) {
        if (!isVaultOperationBlocked()) return true;
        captureStep("08_Vault_Operation_Blocked_While_Armed");
        boolean authenticated = openArmDisarmFrs().authenticateWithProfileRecovery(profileRecovery);
        if (!authenticated) return false;
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

    /** Wait for positive screen evidence; absence of an ARM warning is not DISARM proof. */
    public boolean waitForKnownArmState() {
        String state = new WebDriverWait(driver, Duration.ofSeconds(60)).until(ignored -> {
            if (driver.findElements(VAULT_BLOCKED).stream().anyMatch(WebElement::isDisplayed))
                return "ARMED";
            if (driver.findElements(AppiumBy.accessibilityId("Requesting Access")).stream()
                    .anyMatch(WebElement::isDisplayed)
                    && driver.findElements(AppiumBy.xpath("//android.widget.ImageView[@content-desc='Vault']"))
                    .stream().anyMatch(WebElement::isDisplayed)) return "DISARMED";
            return null;
        });
        logStep("Site state verified: " + state);
        return "ARMED".equals(state);
    }

    public void verifyArmState(boolean armed) {
        if (Boolean.valueOf(armed).equals(verifiedBluetoothState)) {
            logStep("State already verified by Bluetooth result: " + (armed ? "ARMED" : "DISARMED"));
            return;
        }
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(ignored -> {
            BluetoothArmDisarmPage bluetooth = new BluetoothArmDisarmPage(driver);
            if (bluetooth.isVisible()) {
                bluetooth.complete(armed);
                verifiedBluetoothState = armed;
                return true; // Done returns to dashboard; do not wait for site controls there.
            }
            By success = AppiumBy.accessibilityId(armed
                    ? "Site armed successfully" : "Site disarmed successfully");
            if (driver.findElements(success).stream().anyMatch(WebElement::isDisplayed)) {
                captureStep("ARM_DISARM_Result_Verified");
                click(AppiumBy.accessibilityId("OK"));
                return true;
            }
            boolean blocked = driver.findElements(VAULT_BLOCKED).stream().anyMatch(WebElement::isDisplayed);
            boolean doorVisible = driver.findElements(AppiumBy.xpath(
                    "//android.widget.ImageView[@content-desc='Vault']")).stream().anyMatch(WebElement::isDisplayed);
            return armed ? blocked : !blocked && doorVisible;
        });
        captureStep(armed ? "Site_ARMED_Verified" : "Site_DISARMED_Verified");
    }

    private FrsPage openArmDisarmFrs() {
        verifiedBluetoothState = null;
        new AlertPage(driver).dismissDoorAlertIfPresent();
        captureStep("09_Before_Click_Arm_Disarm");
        click(ARM_DISARM);
        allowAnyVisibleRuntimePermissions();
        captureStep("10_FRS_Screen");
        return new FrsPage(driver);
    }

    private void chooseArmModeChange(By messageLocator, String expectedMessage,
                                     boolean confirmChange, String actionName) {
        BluetoothArmDisarmPage bluetooth = new BluetoothArmDisarmPage(driver);
        String nextScreen = wait.until(ignored -> {
            if (bluetooth.isVisible()) return "bluetooth";
            if (driver.findElements(messageLocator).stream().anyMatch(WebElement::isDisplayed))
                return "confirmation";
            return null;
        });
        if ("bluetooth".equals(nextScreen)) {
            bluetooth.complete("Activate".equals(actionName));
            verifiedBluetoothState = "Activate".equals(actionName);
            return;
        }
        WebElement message = wait.until(ignored -> {
            new AlertPage(driver).dismissDoorAlertIfPresent();
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
                    clickElement(matches.getFirst(), "ArmDisarmPage click");
                    allowed = true;
                    break;
                }
            }
            if (!allowed) return;
        }
        Assert.fail("More Android permission prompts appeared than expected.");
    }

}
