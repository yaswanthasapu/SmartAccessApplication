package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class DashboardPage extends BasePage {
    private static final By SMART_ACCESS = AppiumBy.xpath(
            "//android.widget.ImageView[contains(@content-desc, \"Smart Access\") " +
                    "and contains(@content-desc, \"Secure access and authentication\")]");

    public DashboardPage(AndroidDriver driver) { super(driver); }

    public void openSmartAccess() {
        click(SMART_ACCESS);
        logStep("Smart Access clicked");
    }

    public void reopenSmartAccess() {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        // Check again after the third Back, which can reach the dashboard.
        for (int attempt = 0; attempt <= 3; attempt++) {
            try {
                WebElement smartAccess = shortWait.until(ignored -> {
                    List<WebElement> matches = driver.findElements(SMART_ACCESS);
                    return matches.stream().filter(WebElement::isDisplayed)
                            .filter(WebElement::isEnabled).findFirst().orElse(null);
                });
                clickElement(smartAccess, "DashboardPage click");
                return;
            } catch (org.openqa.selenium.TimeoutException ignored) {
                if (attempt == 3) break;
                driver.navigate().back();
                captureStep("Return_To_Smart_Access_Back_" + (attempt + 1));
            }
        }
        throw new IllegalStateException("Unable to return to Smart Access after ARM/DISARM.");
    }
}
