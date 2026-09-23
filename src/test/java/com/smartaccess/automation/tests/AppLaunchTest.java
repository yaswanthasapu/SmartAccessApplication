package com.smartaccess.automation.tests;

import com.smartaccess.automation.util.Adb;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppLaunchTest extends BaseTest {
    @Test(groups = "smoke")
    public void appIsInstalledAndForeground() {
        Assert.assertEquals(driver.getCurrentPackage(), config.application().packageName());
        Assert.assertTrue(driver.currentActivity().contains("MainActivity"));
        Assert.assertTrue(Adb.shell(device, "pm", "path", config.application().packageName())
                .contains(config.application().packageName()));
    }

    @Test(groups = "smoke")
    public void appExposesUiHierarchy() {
        String source = driver.getPageSource();
        Assert.assertFalse(source.isBlank());
        Assert.assertTrue(source.toLowerCase().contains("hierarchy"));
    }

    @Test(groups = "smoke")
    public void appHasNotCrashed() {
        String crashes = Adb.crashLog(device);
        Assert.assertFalse(crashes.contains(config.application().packageName()), crashes);
    }
}
