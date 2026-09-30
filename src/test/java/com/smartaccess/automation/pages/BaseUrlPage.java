package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class BaseUrlPage extends BasePage {
    private static final By BASE_URL = AppiumBy.xpath("//android.widget.EditText");
    private static final By APPLY = AppiumBy.className("android.widget.Button");

    public BaseUrlPage(AndroidDriver driver) { super(driver); }

    public void applyBaseUrl(String url) {
        enterText(BASE_URL, url);
        logStep("URL entered");
        click(APPLY);
        logStep("Apply clicked");
    }
}
