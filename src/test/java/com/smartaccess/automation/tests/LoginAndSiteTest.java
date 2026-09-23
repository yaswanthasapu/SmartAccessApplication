package com.smartaccess.automation.tests;

import com.smartaccess.automation.pages.AccountPage;
import com.smartaccess.automation.pages.ArmDisarmPage;
import com.smartaccess.automation.pages.BaseUrlPage;
import com.smartaccess.automation.pages.DashboardPage;
import com.smartaccess.automation.pages.LoginPage;
import com.smartaccess.automation.pages.SitePage;
import org.testng.annotations.Test;

public class LoginAndSiteTest extends BaseTest {
    @Test(groups = {"e2e", "requires-login"})
    public void loginOpenSmartAccessSelectSiteAndOpenFrs() {
        new BaseUrlPage(driver).applyBaseUrl(config.environment().url());
        new AccountPage(driver).proceedWithAccount(config.environment().accountId());
        new LoginPage(driver).login(config.credentials().username(), config.credentials().password());
        new DashboardPage(driver).openSmartAccess();
        ArmDisarmPage armDisarmPage = new SitePage(driver).selectSite("MAFIN1001");
        String armModeAction = System.getProperty("armModeAction", "activate");
        switch (armModeAction) {
            case "activate" -> armDisarmPage.activateArm();
            case "deactivate" -> armDisarmPage.deactivateArm(true);
            case "cancel-deactivation" -> armDisarmPage.deactivateArm(false);
            default -> throw new IllegalArgumentException("Unsupported armModeAction: " + armModeAction);
        }
    }
}
