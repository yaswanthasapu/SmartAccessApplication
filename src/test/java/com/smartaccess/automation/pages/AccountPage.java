package com.smartaccess.automation.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AccountPage extends BasePage {
    // The semantic View is only the floating label. The hierarchy exposes one
    // real EditText on this screen, and typing into it enables Proceed.
    private static final By ACCOUNT_ID = AppiumBy.className("android.widget.EditText");
    private static final By PROCEED = AppiumBy.accessibilityId("Proceed");
    private static final By INVALID_ACCOUNT = AppiumBy.accessibilityId("Invalid Account ID");
    private static final By TRY_AGAIN = AppiumBy.accessibilityId("Try Again");
    private static final By LOGIN_INPUTS = AppiumBy.className("android.widget.EditText");

    public AccountPage(AndroidDriver driver) { super(driver); }

    public void proceedWithAccount(String accountId) {
        WebDriverWait accountWait = new WebDriverWait(driver, Duration.ofSeconds(20));
        int maximumAttempts = 3;
        for (int attempt = 1; attempt <= maximumAttempts; attempt++) {
            enterText(ACCOUNT_ID, accountId);
            logStep("Account ID entered (attempt " + attempt + ")");
            hideKeyboardIfVisible();
            captureStep("02_Account_ID_Entered");
            clickElement(waitForClickability(PROCEED), "AccountPage click");
            logStep("Account ID submitted");
            AccountOutcome outcome = accountWait.until(ignored -> {
                if (!driver.findElements(INVALID_ACCOUNT).isEmpty()) return AccountOutcome.INVALID;
                if (driver.findElements(LOGIN_INPUTS).size() >= 2) return AccountOutcome.LOGIN;
                return null;
            });
            if (outcome == AccountOutcome.LOGIN) {
                logStep("Account accepted; login screen displayed");
                return;
            }
            captureStep("02_Invalid_Account_ID_Attempt_" + attempt);
            if (attempt == maximumAttempts)
                throw new AssertionError("Account ID was rejected after " + maximumAttempts
                        + " attempts: " + accountId);
            clickElement(waitForClickability(TRY_AGAIN), "AccountPage click");
            try {
                Thread.sleep(Duration.ofSeconds(10).toMillis());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while waiting to retry account validation.", interrupted);
            }
        }
    }

    private enum AccountOutcome { LOGIN, INVALID }
}
