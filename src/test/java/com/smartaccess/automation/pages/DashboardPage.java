package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class DashboardPage extends BasePage {
    private static final By SMART_ACCESS = AppiumBy.xpath(
            "//android.widget.ImageView[contains(@content-desc, \"Smart Access\") " +
                    "and contains(@content-desc, \"Secure access and authentication\")]");

    public DashboardPage(AndroidDriver driver) { super(driver); }

    public void openSmartAccess() { click(SMART_ACCESS); }
}
