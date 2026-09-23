package com.smartaccess.automation.tests;

import com.smartaccess.automation.config.ConfigLoader;
import com.smartaccess.automation.config.RunConfig;
import com.smartaccess.automation.driver.DriverFactory;
import com.smartaccess.automation.util.Adb;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.time.Duration;

public abstract class BaseTest {
    protected AndroidDriver driver;
    protected RunConfig config;
    protected String device;

    @BeforeClass(alwaysRun = true)
    public void startDriver() {
        config = ConfigLoader.load();
        String requested = System.getProperty("device", "");
        if (requested.isBlank()) requested = config.appium().deviceName();
        device = Adb.resolveDevice(requested);
        Adb.clearLogcat(device);
        driver = DriverFactory.create(config, device);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    @AfterClass(alwaysRun = true)
    public void stopDriver() {
        if (driver != null) driver.quit();
    }

    public AndroidDriver getDriver() { return driver; }
}
