package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import java.util.List;

public class PanicPage extends BasePage {
    private static final String CONFIRMATION_TEXT = "Are you sure you want to activate Smart Access ?";

    private static final By PANIC_BUTTON = AppiumBy.xpath(
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View/android.view.View/android.view.View[2]/android.widget.ImageView[1]");
    private static final By CONFIRMATION_MESSAGE = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + CONFIRMATION_TEXT + "']");
    private static final By SMART_ACCESS = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Smart Access']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");

    public PanicPage(AndroidDriver driver) {
        super(driver);
    }

    public void respondToPanicConfirmation(boolean activateSmartAccess) {
        captureStep("08_Before_Click_Panic");
        click(PANIC_BUTTON);

        WebElement message = wait.until(ignored -> {
            List<WebElement> matches = driver.findElements(CONFIRMATION_MESSAGE);
            return matches.isEmpty() ? null : matches.getFirst();
        });
        Assert.assertEquals(message.getAttribute("content-desc"), CONFIRMATION_TEXT);
        captureStep("09_Panic_Confirmation");

        click(activateSmartAccess ? SMART_ACCESS : CANCEL);
        captureStep(activateSmartAccess
                ? "10_Panic_Smart_Access_Selected"
                : "10_Panic_Cancelled");
    }
}
