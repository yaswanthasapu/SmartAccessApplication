package com.smartaccess.automation.config;

public record RunConfig(String name, Environment environment, Credentials credentials,
                        Appium appium, Application application) {
    public record Environment(String url, String accountId) {}
    public record Credentials(String username, String password) {}
    public record Appium(String serverUrl, String deviceName, String platformVersion,
                         int newCommandTimeout, int adbExecTimeout) {}
    public record Application(String apkPath, String packageName, String activity,
                              boolean fullReset, boolean noReset) {}
}
