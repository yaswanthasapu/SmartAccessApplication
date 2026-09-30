package com.smartaccess.automation.config;

import java.util.Map;

public record RunConfig(String name, Environment environment, Credentials credentials,
                        Appium appium, Application application, Execution execution) {
    public record Environment(String url, String accountId) {}
    public record Credentials(String username, String password) {}
    public record Appium(String serverUrl, String deviceName, String platformVersion,
                         int newCommandTimeout, int adbExecTimeout) {}
    public record Application(String apkPath, String packageName, String activity,
                              boolean fullReset, boolean noReset) {}
    public record Execution(String siteId, String door, String accessReason,
                            String armPolicy, Map<String, String> securityAnswers,
                            String otpMode, String doorAction,
                            String profileUserName, String approvedAction, ArmDisarm armDisarm) {}

    public record ArmDisarm(String mode, Integer iterations, Boolean logoutAfterCompletion) {
        public ArmDisarm {
            mode = mode == null ? "configured-action" : mode;
            iterations = iterations == null ? 1 : iterations;
            logoutAfterCompletion = logoutAfterCompletion == null ? false : logoutAfterCompletion;
            if (!java.util.List.of("configured-action", "arm-disarm-cycle").contains(mode))
                throw new IllegalArgumentException("Unsupported armDisarm.mode: " + mode);
            if (iterations < 1) throw new IllegalArgumentException("armDisarm.iterations must be positive.");
        }
    }
}
