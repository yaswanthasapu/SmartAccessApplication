package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

public class SitePage extends BasePage {
    public SitePage(AndroidDriver driver) { super(driver); }

    public ArmDisarmPage selectSite(String targetSite) {
        captureStep("05_Site_List_Screen");
        String escapedSite = targetSite.replace("\\", "\\\\").replace("\"", "\\\"");
        String scrollQuery = "new UiScrollable(new UiSelector().scrollable(true))"
                + ".scrollIntoView(new UiSelector().className(\"android.widget.ImageView\")"
                + ".descriptionContains(\"" + escapedSite + "\"))";

        WebElement siteCard = waitForClickability(AppiumBy.androidUIAutomator(scrollQuery));
        Assert.assertTrue(siteCard.isDisplayed(), "Site card was not found: " + targetSite);
        captureStep("06_Before_Click_Site_" + targetSite);
        siteCard.click();
        captureStep("07_After_Click_Site_" + targetSite);
        return new ArmDisarmPage(driver);
    }
}
