package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class DoorConfirmationPage extends BasePage {
    private static final By VAULT = AppiumBy.xpath(
            "//android.widget.ImageView[@content-desc='Vault']");
    private static final By OPEN = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Open']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");
    private static final By FAILED = AppiumBy.xpath(
            "//android.view.View[@content-desc='Failed']");
    private static final By SUCCESS = AppiumBy.xpath(
            "//android.view.View[@content-desc='Success']");
    private static final By VAULT_OPENED = AppiumBy.xpath(
            "//android.view.View[contains(@content-desc, 'Vault opened successfully')]");
    private static final By OK = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='OK']");

    private final WebDriverWait pageWait;

    public DoorConfirmationPage(AndroidDriver driver) {
        super(driver);
        pageWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void finishDoorRequest(boolean openDoor) {
        captureStep("19_Vault_Open_Confirmation");
        click(openDoor ? OPEN : CANCEL);
        if (!openDoor) {
            captureStep("20_Vault_Open_Cancelled");
            return;
        }

        finishDoorResult();
    }

    public void finishDoorResult() {
        DiagnosticsPage diagnostics = new DiagnosticsPage(driver);
        Result result = pageWait.until(ignored -> {
            diagnostics.captureIfVisible();
            diagnostics.returnAfterDoorReceivedDataIfPresent();
            if (diagnostics.receivedDoorData() != null)
                return "Vault: Vault opened".equals(diagnostics.receivedDoorData())
                        ? Result.SUCCESS : Result.FAILED;
            // Connection State can say Success before Received Data arrives.
            if (diagnostics.isDisplayed()) return null;
            if (firstDisplayed(VAULT_OPENED) != null || firstDisplayed(SUCCESS) != null)
                return Result.FAILED; // A generic success popup is not received-data proof.
            if (firstDisplayed(FAILED) != null) return Result.FAILED;
            return null;
        });
        captureStep(result == Result.SUCCESS ? "20_Vault_Open_Success" : "20_Vault_Open_Failed");
        String received = diagnostics.receivedDoorData();
        String reason = "Received Data: " + (received == null ? "not observed" : received);
        System.out.println("[Door result] " + reason);
        AssertionError failure = result == Result.SUCCESS ? null : new AssertionError(
                "Expected Received Data exactly 'Vault: Vault opened'. " + reason);
        try {
            // Back may reveal a result dialog, or return directly to the site screen.
            try {
                WebElement ok = new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ignored -> firstDisplayed(OK));
                clickElement(ok, "Door result OK");
            } catch (org.openqa.selenium.TimeoutException ignored) {
                // No result dialog: continue to logout directly.
            }
            new ProfilePage(driver).logoutAfterDoorResult(reason + ". Execution ended.");
        } catch (RuntimeException | AssertionError logoutError) {
            if (failure == null) throw logoutError;
            failure.addSuppressed(logoutError);
        }
        if (failure != null) throw failure;
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


    private enum Result { SUCCESS, FAILED }
}
