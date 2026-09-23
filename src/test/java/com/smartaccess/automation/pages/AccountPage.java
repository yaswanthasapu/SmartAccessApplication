package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class AccountPage extends BasePage {
    // The semantic View is only the floating label. The hierarchy exposes one
    // real EditText on this screen, and typing into it enables Proceed.
    private static final By ACCOUNT_ID = AppiumBy.className("android.widget.EditText");
    private static final By PROCEED = AppiumBy.accessibilityId("Proceed");

    public AccountPage(AndroidDriver driver) { super(driver); }

    public void proceedWithAccount(String accountId) {
        enterText(ACCOUNT_ID, accountId);
        hideKeyboardIfVisible();
        captureStep("02_Account_ID_Entered");
        waitForClickability(PROCEED).click();
    }
}
