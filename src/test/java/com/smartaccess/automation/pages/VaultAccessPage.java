package com.smartaccess.automation.pages;

import com.smartaccess.automation.config.RunConfig;
import io.appium.java_client.android.AndroidDriver;
import org.testng.Assert;
import java.util.List;

/** Coordinates the access flow; screen interactions belong to individual page objects. */
public class VaultAccessPage extends BasePage {
    public VaultAccessPage(AndroidDriver driver) { super(driver); }

    public void execute(RunConfig.Execution execution) {
        validateExecution(execution);
        DoorSelectionPage door = new DoorSelectionPage(driver);
        door.selectDoor(execution.door());
        door.selectReasonAndContinue(execution.accessReason(), execution.door());
        door.confirmRequestAndProceedThroughWarning(execution.accessReason());
        boolean authenticated = new FrsPage(driver)
                .authenticateWithProfileRecovery(() -> new ProfileRecoveryPage(driver)
                        .recoverRevokedProfile(execution.profileUserName(), execution.approvedAction()));
        if (!authenticated) return;

        SecurityQuestionsPage.PostMcqOutcome outcome = new SecurityQuestionsPage(driver).answerSecurityQuestions(execution.securityAnswers());
        if (outcome == SecurityQuestionsPage.PostMcqOutcome.DIAGNOSTICS) {
            logStep("No OTP screen displayed; Diagnostics opened directly after MCQ");
            new DoorConfirmationPage(driver).finishDoorResult();
            return;
        }
        if (outcome == SecurityQuestionsPage.PostMcqOutcome.OTP) {
            logStep("OTP screen displayed; waiting for manual OTP entry");
            if (new OtpPage(driver).verifyManualOtp(execution.otpMode())) {
                new DoorConfirmationPage(driver).finishDoorResult();
                return;
            }
        } else if (outcome == SecurityQuestionsPage.PostMcqOutcome.DOOR_CONFIRMATION) {
            logStep("No OTP screen displayed; proceeding directly to door confirmation");
        }
        new DoorConfirmationPage(driver).finishDoorRequest("open".equalsIgnoreCase(execution.doorAction()));
    }

    private void validateExecution(RunConfig.Execution execution) {
        Assert.assertNotNull(execution, "Execution configuration is required.");
        Assert.assertFalse(execution.siteId().isBlank(), "execution.siteId is required.");
        Assert.assertFalse(execution.door().isBlank(), "execution.door is required.");
        Assert.assertFalse(execution.accessReason().isBlank(), "execution.accessReason is required.");
        Assert.assertTrue(List.of("open", "cancel").contains(execution.doorAction().toLowerCase()),
                "execution.doorAction must be open or cancel.");
    }

}
