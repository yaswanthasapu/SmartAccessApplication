package com.smartaccess.automation.tests;

import com.smartaccess.automation.pages.*;
import com.smartaccess.automation.config.RunConfig;
import com.smartaccess.automation.util.RunEvidence;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Standalone state-change test: never starts a door transaction. */
public class ArmDisarmTest extends BaseTest {
    @Test(groups = "arm-disarm")
    public void checkConfiguredArmDisarm() {
        String policy = config.execution().armPolicy();
        RunConfig.ArmDisarm options = config.execution().armDisarm();
        if (options == null) options = new RunConfig.ArmDisarm(null, null, null);
        boolean cycle = "arm-disarm-cycle".equals(options.mode());
        Assert.assertTrue(cycle || "arm-if-disarmed".equals(policy) || "disarm-if-armed".equals(policy),
                "execution.armPolicy must be arm-if-disarmed or disarm-if-armed.");
        boolean targetArmed = "arm-if-disarmed".equals(policy);
        RunEvidence.loopsConfigured(driver, options.iterations());
        new BaseUrlPage(driver).applyBaseUrl(config.environment().url());
        new AccountPage(driver).proceedWithAccount(config.environment().accountId());
        new LoginPage(driver).login(config.credentials().username(), config.credentials().password());
        DashboardPage dashboard = new DashboardPage(driver);
        SitePage sites = new SitePage(driver);
        dashboard.openSmartAccess();
        ArmDisarmPage page = sites.selectSite(config.execution().siteId());
        for (int iteration = 1; iteration <= options.iterations(); iteration++) {
            RunEvidence.iterationStarted(driver, iteration, options.iterations());
            System.out.println("[ARM/DISARM] Iteration " + iteration + "/" + options.iterations());
            if (cycle) {
                page = ensureState(page, true);
                page = ensureState(page, false);
            } else {
                page = ensureState(page, targetArmed);
            }
            System.out.println("[ARM/DISARM] Iteration " + iteration + " completed");
            RunEvidence.iterationCompleted(driver);
        }
        if (options.logoutAfterCompletion()) {
            new ProfilePage(driver).logoutAfterDoorResult("ARM/DISARM iterations completed. Execution ended.");
        } else {
            System.out.println("[ARM/DISARM] Logout disabled by JSON; test completed without logout.");
        }
    }

    private ArmDisarmPage ensureState(ArmDisarmPage page, boolean targetArmed) {
        boolean armed = page.waitForKnownArmState();
        System.out.println("[ARM/DISARM] Requested state: " + (targetArmed ? "ARMED" : "DISARMED"));
        if (armed != targetArmed) {
            boolean changed = targetArmed ? page.activateArm() : page.deactivateArm(true);
            Assert.assertTrue(changed, "ARM/DISARM action was not performed.");
            page.verifyArmState(targetArmed);
            System.out.println("[ARM/DISARM] Returning through Smart Access and site selection after "
                    + (targetArmed ? "ARM" : "DISARM"));
            new DashboardPage(driver).reopenSmartAccess();
            page = new SitePage(driver).selectSite(config.execution().siteId());
        } else {
            System.out.println("[ARM/DISARM] Already in requested state; no toggle needed.");
        }
        page.verifyArmState(targetArmed);
        return page;
    }
}
