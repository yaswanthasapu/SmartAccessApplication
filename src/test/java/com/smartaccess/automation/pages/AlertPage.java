package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/** Acknowledges any door interlock, verifies site selection, then logs out. */
public class AlertPage extends BasePage {
    private static final By DOOR_ALERT = AppiumBy.xpath(
            "//android.view.View[contains(translate(@content-desc, " +
                    "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'please close ') " +
                    "and contains(translate(@content-desc, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', " +
                    "'abcdefghijklmnopqrstuvwxyz'), 'door') and contains(translate(@content-desc, " +
                    "'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'retry')]");
    private static final By OK = AppiumBy.xpath(
            "//android.widget.Button[@content-desc='OK']");

    public AlertPage(AndroidDriver driver) { super(driver); }

    public void dismissDoorAlertIfPresent() {
        for (WebElement alert : driver.findElements(DOOR_ALERT)) {
            if (!alert.isDisplayed()) continue;
            String message = alert.getAttribute("content-desc");
            System.out.println("[Door alert] " + message);
            captureStep("Door_Open_Alert");
            click(OK);
            new SitePage(driver).verifySiteList();
            captureStep("Sites_List_After_Door_Alert_OK");
            new ProfilePage(driver).logoutFromSitesList("Door request blocked: " + message
                    + ". Clicked OK, verified sites list, and logged out. Execution ended.");
        }
    }
}
