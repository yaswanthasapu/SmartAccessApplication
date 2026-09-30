package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import java.util.List;

/** Captures connection progress and leaves completed door diagnostics. */
public class DiagnosticsPage extends BasePage {
    private static final By HEADING = AppiumBy.xpath(
            "//*[contains(@content-desc, 'Diagnostics') or @text='Diagnostics']");
    private boolean captured;
    private boolean returnedFromDoorResult;
    private String receivedDoorData;
    private static final By RECEIVED_LABEL = AppiumBy.xpath(
            "//*[contains(@content-desc, 'Received Data') or contains(@text, 'Received Data')]");
    private static final By RECEIVED_VALUE = AppiumBy.xpath(
            "//android.view.View[@content-desc='Received Data']/following-sibling::*[1]");
    private static final By BACK = AppiumBy.className("android.widget.Button");

    public DiagnosticsPage(AndroidDriver driver) { super(driver); }

    public boolean isDisplayed() { return isVisible(HEADING); }

    public void returnAfterDoorReceivedDataIfPresent() {
        if (returnedFromDoorResult || !isVisible(HEADING)) return;
        String receivedData = receivedDataValue();
        if (receivedData == null) return;
        receivedDoorData = receivedData;
        captureStep("Diagnostics_Received_Data: " + receivedData);
        System.out.println("[Diagnostics] Received Data: " + receivedData);
        List<WebElement> buttons = driver.findElements(BACK).stream()
                .filter(WebElement::isDisplayed).filter(WebElement::isEnabled).toList();
        if (buttons.size() != 1) {
            throw new IllegalStateException("Diagnostics Back locator matched " + buttons.size()
                    + " visible enabled buttons; refusing an ambiguous click.");
        }
        clickElement(buttons.getFirst(), "Diagnostics Back after received door data");
        wait.until(ignored -> !isVisible(HEADING));
        returnedFromDoorResult = true;
        captureStep("After_Diagnostics_Back");
    }

    public String receivedDoorData() { return receivedDoorData; }

    private String receivedDataValue() {
        // Verified against the device's saved hierarchy: label and value are siblings.
        for (WebElement value : driver.findElements(RECEIVED_VALUE)) {
            try {
                if (!value.isDisplayed()) continue;
                String text = semanticText(value);
                if (hasData(text)) return text;
            } catch (StaleElementReferenceException ignored) {
                // Fall back to the alternate Flutter semantics layouts below.
            }
        }
        for (WebElement label : driver.findElements(RECEIVED_LABEL)) {
            try {
                if (!label.isDisplayed()) continue;
                String labelText = semanticText(label);
                // Flutter may merge the label and value into one semantics node.
                if (labelText.startsWith("Received Data")) {
                    String combined = labelText.substring("Received Data".length())
                            .replaceFirst("^[\\s:]+", "").trim();
                    if (hasData(combined)) return combined;
                }
                // Otherwise read the adjacent value in the same row, not an
                // arbitrary button or status elsewhere on the Diagnostics screen.
                for (WebElement value : label.findElements(AppiumBy.xpath(
                        "./following-sibling::*[1] | ./following-sibling::*[1]//*"))) {
                    if (!value.isDisplayed()) continue;
                    String text = semanticText(value);
                    if (hasData(text)) return text;
                }
            } catch (StaleElementReferenceException ignored) {
                // Received data is updated asynchronously; retry next poll.
            }
        }
        return null;
    }

    private String semanticText(WebElement element) {
        String text = element.getAttribute("content-desc");
        if (text == null || text.isBlank()) text = element.getText();
        return text == null ? "" : text.trim();
    }

    private boolean hasData(String text) {
        return !text.isBlank() && !List.of("-", "—", "–", "Received Data").contains(text);
    }

    private boolean isVisible(By locator) {
        for (WebElement element : driver.findElements(locator)) {
            try {
                if (element.isDisplayed()) return true;
            } catch (StaleElementReferenceException ignored) {
                // Retry on the next outcome poll.
            }
        }
        return false;
    }

    public void captureIfVisible() {
        if (captured) return;
        for (WebElement element : driver.findElements(HEADING)) {
            try {
                if (!element.isDisplayed()) continue;
                captureStep("Diagnostics_Connection_Status");
                captured = true;
                return;
            } catch (StaleElementReferenceException ignored) {
                // Check again on the next poll if the connection screen rebuilt.
            }
        }
    }
}
