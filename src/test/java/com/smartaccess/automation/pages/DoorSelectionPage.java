package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class DoorSelectionPage extends BasePage {
    private static final By REQUESTING_ACCESS = AppiumBy.xpath(
            "//android.view.View[@content-desc='Requesting Access']");
    private static final By ACTIVE_TRANSACTION = AppiumBy.xpath(
            "//android.view.View[contains(@content-desc, 'Active Transaction')]");
    private static final By NEXT = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Next']");
    private static final By CONFIRM = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Confirm']");
    private static final By WARNING = AppiumBy.xpath(
            "//android.view.View[@content-desc='Warning']");
    private static final By PROCEED = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Proceed']");
    private static final By FRS_SCREEN = AppiumBy.xpath(
            "//android.view.View[@content-desc='Align your face in the middle']");
    private static final By OPEN = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Open']");
    private static final By CANCEL = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='Cancel']");

    private final WebDriverWait pageWait;

    public DoorSelectionPage(AndroidDriver driver) {
        super(driver);
        pageWait = new WebDriverWait(driver, Duration.ofSeconds(60));
    }

    public void selectDoor(String doorName) {
        Assert.assertNotNull(pageWait.until(ignored -> firstDisplayed(REQUESTING_ACCESS)),
                "Requesting Access heading was not displayed.");
        By door = AppiumBy.xpath("//android.widget.ImageView[@content-desc="
                + xpathLiteral(doorName) + "]");
        WebElement doorElement = pageWait.until(ignored -> firstDisplayed(door));
        Assert.assertEquals(doorElement.getAttribute("content-desc"), doorName);
        captureStep("08_Requesting_Access_" + doorName);
        clickElement(doorElement, "DoorSelectionPage click");
    }

    public void selectReasonAndContinue(String reason, String doorName) {
        By reasonLocator = AppiumBy.xpath("//android.widget.ImageView[@content-desc="
                + xpathLiteral(reason) + "]");
        By doorLocator = AppiumBy.xpath("//android.widget.ImageView[@content-desc="
                + xpathLiteral(doorName) + "]");
        for (int attempt = 0; attempt < 2; attempt++) {
            ReasonOutcome outcome = pageWait.until(ignored -> {
                if (firstDisplayed(reasonLocator) != null) return ReasonOutcome.REASON_SCREEN;
                if (firstDisplayed(ACTIVE_TRANSACTION) != null) return ReasonOutcome.ACTIVE_TRANSACTION;
                return null;
            });
            if (outcome == ReasonOutcome.REASON_SCREEN) {
                clickElement(firstDisplayed(reasonLocator), "DoorSelectionPage click");
                captureStep("09_" + reason + "_Access_Reason_Selected");
                click(NEXT);
                return;
            }
            captureStep("09_Stale_Active_Transaction");
            click(CANCEL);
            Assert.assertNotNull(pageWait.until(ignored -> firstDisplayed(REQUESTING_ACCESS)),
                    "Requesting Access screen did not return after cancelling the stale transaction.");
            click(doorLocator);
        }
        Assert.fail("Could not open the access-reason screen after cancelling a stale transaction.");
    }

    public void confirmRequestAndProceedThroughWarning(String reason) {
        By confirmationLocator = AppiumBy.xpath(
                "//android.view.View[contains(@content-desc, \"You've requested access with the "
                        + reason + ".\") and contains(@content-desc, 'Are You ready to proceed?')]");
        WebElement confirmation = pageWait.until(ignored -> firstDisplayed(confirmationLocator));
        String text = confirmation.getAttribute("content-desc");
        Assert.assertTrue(text.contains("You've requested access with the " + reason + ".")
                        && text.contains("Are You ready to proceed?"),
                "Unexpected access confirmation: " + text);
        captureStep("10_Customer_Access_Confirmation");
        click(CONFIRM);

        WarningOutcome outcome = pageWait.until(ignored -> {
            if (firstDisplayed(WARNING) != null) return WarningOutcome.WARNING;
            if (firstDisplayed(FRS_SCREEN) != null) return WarningOutcome.FRS_STARTED;
            return null;
        });
        if (outcome == WarningOutcome.WARNING) {
            captureStep("11_Vault_Access_Warning");
            click(PROCEED);
        }
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

    private static String xpathLiteral(String value) {
        if (!value.contains("'")) return "'" + value + "'";
        if (!value.contains("\"")) return "\"" + value + "\"";
        throw new IllegalArgumentException("Unsupported XPath value: " + value);
    }

    private enum WarningOutcome { WARNING, FRS_STARTED }
    private enum ReasonOutcome { REASON_SCREEN, ACTIVE_TRANSACTION }
}
