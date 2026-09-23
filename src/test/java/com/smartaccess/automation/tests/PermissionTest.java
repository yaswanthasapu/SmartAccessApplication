package com.smartaccess.automation.tests;

import com.smartaccess.automation.config.ConfigLoader;
import com.smartaccess.automation.config.RunConfig;
import com.smartaccess.automation.util.Adb;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PermissionTest {
    private RunConfig config;
    private String device;

    @BeforeClass(alwaysRun = true)
    public void loadDeviceConfiguration() {
        config = ConfigLoader.load();
        String requested = System.getProperty("device", "");
        if (requested.isBlank()) requested = config.appium().deviceName();
        device = Adb.resolveDevice(requested);
    }

    @DataProvider(name = "runtimePermissions")
    public Object[][] runtimePermissions() {
        return new Object[][]{{"android.permission.CAMERA"}, {"android.permission.ACCESS_FINE_LOCATION"},
                {"android.permission.ACCESS_COARSE_LOCATION"}, {"android.permission.RECORD_AUDIO"},
                {"android.permission.POST_NOTIFICATIONS"}, {"android.permission.BLUETOOTH_SCAN"},
                {"android.permission.BLUETOOTH_CONNECT"}, {"android.permission.BLUETOOTH_ADVERTISE"},
                {"android.permission.READ_EXTERNAL_STORAGE"},
                {"android.permission.USE_BIOMETRIC"}, {"android.permission.USE_FINGERPRINT"},
                {"android.permission.INTERNET"}, {"android.permission.ACCESS_NETWORK_STATE"},
                {"android.permission.WAKE_LOCK"}, {"android.permission.VIBRATE"},
                {"android.permission.FOREGROUND_SERVICE"}, {"android.permission.RECEIVE_BOOT_COMPLETED"},
                {"android.permission.MODIFY_AUDIO_SETTINGS"}};
    }

    @Test(groups = "permissions", dataProvider = "runtimePermissions")
    public void expectedPermissionIsDeclared(String permission) {
        String packageDump = Adb.shell(device, "dumpsys", "package", config.application().packageName());
        Assert.assertTrue(packageDump.contains(permission), "Missing permission: " + permission);
    }
}
