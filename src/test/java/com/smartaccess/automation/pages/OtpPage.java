package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class OtpPage extends BasePage {
    private static final String OTP_HEADING_TEXT = "Verify Mobile (******6458)";
    private static final By OTP_HEADING = AppiumBy.xpath(
            "//android.view.View[@content-desc='" + OTP_HEADING_TEXT + "']");
    private static final String OTP_FIELD_ROOT =
            "//android.widget.FrameLayout[@resource-id='android:id/content']" +
                    "/android.widget.FrameLayout/android.view.View/android.view.View" +
                    "/android.view.View[1]/android.view.View/android.widget.EditText";
    private static final By[] OTP_FIELDS = {
            AppiumBy.xpath(OTP_FIELD_ROOT + "[1]"), AppiumBy.xpath(OTP_FIELD_ROOT + "[2]"),
            AppiumBy.xpath(OTP_FIELD_ROOT + "[3]"), AppiumBy.xpath(OTP_FIELD_ROOT + "[4]")
    };
    private static final By VERIFY = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Verify']");
    private static final By INVALID_OTP = AppiumBy.xpath(
            "//android.view.View[@content-desc='Invalid OTP']");
    private static final By OPEN = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Open']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");

    private final WebDriverWait pageWait;

    public OtpPage(AndroidDriver driver) {
        super(driver);
        pageWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public boolean verifyManualOtp(String otpMode) {
        Assert.assertEquals(otpMode, "manual", "Only manual OTP mode is supported.");
        WebElement heading = pageWait.until(ignored -> firstDisplayed(OTP_HEADING));
        Assert.assertEquals(heading.getAttribute("content-desc"), OTP_HEADING_TEXT);
        captureStep("17_OTP_Screen_Before_Manual_Entry");
        waitForManualOtpEntry("Enter the four-digit OTP on the connected phone.");
        captureStep("17_Manual_OTP_Entered");
        hideKeyboardIfVisible();
        captureStep("17_OTP_Ready_To_Verify");
        click(VERIFY);

        OtpOutcome outcome = waitForOtpOutcome();
        if (outcome == OtpOutcome.INVALID) {
            captureStep("18_Invalid_OTP");
            System.out.println("[Manual OTP] Invalid OTP. Clear it and enter the new OTP on the phone.");
            WebDriverWait manualWait = new WebDriverWait(driver, Duration.ofMinutes(5));
            manualWait.until(ignored -> !otpFieldsPopulated());
            waitForManualOtpEntry("Re-enter the new four-digit OTP on the connected phone.");
            captureStep("18_Manual_Retry_OTP_Entered");
            hideKeyboardIfVisible();
            captureStep("18_Retry_OTP_Ready_To_Verify");
            click(VERIFY);
            outcome = waitForOtpOutcome();
            Assert.assertNotEquals(outcome, OtpOutcome.INVALID, "The retry OTP was also rejected.");
        }
        return outcome == OtpOutcome.DIAGNOSTICS;
    }

    private void waitForManualOtpEntry(String prompt) {
        System.out.println("[Manual OTP] " + prompt);
        new WebDriverWait(driver, Duration.ofMinutes(5))
                .until(ignored -> otpFieldsPopulated());
    }

    private boolean otpFieldsPopulated() {
        for (By field : OTP_FIELDS) {
            WebElement element = firstDisplayed(field);
            if (element == null) return false;
            String value = element.getText();
            if (value == null || value.isBlank()) value = element.getAttribute("text");
            if (value == null || value.isBlank()) return false;
        }
        return true;
    }

    private OtpOutcome waitForOtpOutcome() {
        DiagnosticsPage diagnostics = new DiagnosticsPage(driver);
        return pageWait.until(ignored -> {
            diagnostics.captureIfVisible();
            if (diagnostics.isDisplayed()) return OtpOutcome.DIAGNOSTICS;
            if (firstDisplayed(INVALID_OTP) != null) return OtpOutcome.INVALID;
            if (firstDisplayed(OPEN) != null || firstDisplayed(CANCEL) != null) {
                captureStep("18_OTP_Accepted_Door_Confirmation");
                return OtpOutcome.ACCEPTED;
            }
            return null;
        });
    }

    private WebElement firstDisplayed(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return element;
            } catch (StaleElementReferenceException ignored) {
                // Flutter rebuilt the semantics tree between lookup and inspection.
            }
        }
        return null;
    }


    private enum OtpOutcome { ACCEPTED, INVALID, DIAGNOSTICS }
}
