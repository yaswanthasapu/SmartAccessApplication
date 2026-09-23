package com.smartaccess.automation.driver;

import com.smartaccess.automation.config.ConfigLoader;
import com.smartaccess.automation.config.RunConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

public final class DriverFactory {
    private DriverFactory() {}

    public static AndroidDriver create(RunConfig config, String device) {
        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(device).setUdid(device).setApp(ConfigLoader.apkPath().toString())
                .setAppPackage(config.application().packageName())
                .setAppActivity(config.application().activity())
                .setFullReset(config.application().fullReset()).setNoReset(config.application().noReset())
                .setNewCommandTimeout(Duration.ofSeconds(config.appium().newCommandTimeout()))
                // FRS needs camera access (and the app also requests location, audio,
                // notifications and nearby-device access). Grant runtime permissions
                // when the APK is installed so system dialogs cannot block the flow.
                .setAutoGrantPermissions(true);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:adbExecTimeout", config.appium().adbExecTimeout());
        if (config.appium().platformVersion() != null && !config.appium().platformVersion().isBlank())
            options.setPlatformVersion(config.appium().platformVersion());
        try {
            return new AndroidDriver(URI.create(config.appium().serverUrl()).toURL(), options);
        } catch (MalformedURLException error) {
            throw new IllegalStateException("Invalid Appium URL: " + config.appium().serverUrl(), error);
        }
    }
}
