package com.smartaccess.automation.tests;

import com.smartaccess.automation.pages.AccountPage;
import com.smartaccess.automation.pages.ArmDisarmPage;
import com.smartaccess.automation.pages.BaseUrlPage;
import com.smartaccess.automation.pages.DashboardPage;
import com.smartaccess.automation.pages.LoginPage;
import com.smartaccess.automation.pages.ProfileRecoveryPage;
import com.smartaccess.automation.pages.SitePage;
import com.smartaccess.automation.pages.VaultAccessPage;
import org.testng.annotations.Test;

public class LoginAndSiteTest extends BaseTest {
    @Test(groups = {"e2e", "requires-login"})
    public void executeConfiguredSmartAccessFlow() {
        new BaseUrlPage(driver).applyBaseUrl(config.environment().url());
        new AccountPage(driver).proceedWithAccount(config.environment().accountId());
        new LoginPage(driver).login(config.credentials().username(), config.credentials().password());
        DashboardPage dashboard = new DashboardPage(driver);
        dashboard.openSmartAccess();

        SitePage sites = new SitePage(driver);
        ArmDisarmPage armDisarm = sites.selectSite(config.execution().siteId());
        if (!"disarm-if-armed".equals(config.execution().armPolicy()))
            throw new IllegalArgumentException("Unsupported execution.armPolicy: "
                    + config.execution().armPolicy());

        if (armDisarm.isVaultOperationBlocked()) {
            boolean disarmed = armDisarm.deactivateArmWithProfileRecovery(true,
                    () -> new ProfileRecoveryPage(driver).recoverRevokedProfile(
                            config.execution().profileUserName(), config.execution().approvedAction()));
            if (!disarmed) return;
            dashboard.reopenSmartAccess();
            sites.selectSite(config.execution().siteId());
        }

        new VaultAccessPage(driver).execute(config.execution());
    }
}
