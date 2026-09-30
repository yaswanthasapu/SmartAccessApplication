package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/** Offline ARM/DISARM progress and result; does not infer success from sending a command. */
public class BluetoothArmDisarmPage extends BasePage {
    private static final By HEADING = AppiumBy.xpath(
            "//android.view.View[@content-desc='Arming via Bluetooth' or "
                    + "@content-desc='Disarming via Bluetooth']");
    private static final By CONNECTION = AppiumBy.accessibilityId("Connection State");
    private static final By SENDING = AppiumBy.xpath(
            "//android.view.View[contains(translate(@content-desc, "
                    + "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'sending ') "
                    + "and contains(@content-desc, 'command')]");
    private static final By SUCCESS = AppiumBy.xpath(
            "(//android.view.View[@content-desc='Success'])[2]");
    private static final By RECEIVED = AppiumBy.xpath(
            "//android.view.View[@content-desc='ARMED' or @content-desc='DISARMED']");
    private static final By MESSAGE = AppiumBy.xpath(
            "//android.view.View[@content-desc='Site armed successfully' or "
                    + "@content-desc='Site disarmed successfully']");
    private static final By DONE = AppiumBy.accessibilityId("Done");

    public BluetoothArmDisarmPage(AndroidDriver driver) { super(driver); }

    public boolean isVisible() { return visible(HEADING) != null; }

    public void complete(boolean arm) {
        System.out.println("[Bluetooth] Offline site flow detected.");
        captureStep("Offline_Bluetooth_ARM_DISARM");
        Set<String> observed = new HashSet<>();
        new WebDriverWait(driver, Duration.ofSeconds(60)).until(ignored -> {
            record(CONNECTION, observed);
            record(SENDING, observed);
            WebElement received = visible(RECEIVED);
            WebElement message = visible(MESSAGE);
            if (visible(SUCCESS) == null || received == null || message == null) return false;
            record(RECEIVED, observed);
            record(MESSAGE, observed);
            Assert.assertEquals(received.getAttribute("content-desc"), arm ? "ARMED" : "DISARMED",
                    "Bluetooth received state does not match the requested action.");
            Assert.assertEquals(message.getAttribute("content-desc"),
                    arm ? "Site armed successfully" : "Site disarmed successfully");
            return true;
        });
        captureStep("Bluetooth_Command_Success");
        click(DONE);
    }

    private void record(By locator, Set<String> observed) {
        WebElement element = visible(locator);
        if (element == null) return;
        String text = element.getAttribute("content-desc");
        if (observed.add(text)) {
            System.out.println("[Bluetooth] " + text);
            captureStep("Bluetooth_" + text);
        }
    }

    private WebElement visible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return element;
            } catch (StaleElementReferenceException ignored) {
                // Progress can replace the element while it is being inspected.
            }
        }
        return null;
    }
}
