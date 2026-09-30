package com.smartaccess.automation.tests;

import com.smartaccess.automation.config.ConfigLoader;
import com.smartaccess.automation.config.RunConfig;
import com.smartaccess.automation.driver.DriverFactory;
import com.smartaccess.automation.util.Adb;
import com.smartaccess.automation.util.RunEvidence;
import com.smartaccess.automation.util.ExecutionStop;
import org.testng.ITestContext;
import org.testng.annotations.BeforeMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.time.Duration;

public abstract class BaseTest {
    protected AndroidDriver driver;
    protected RunConfig config;
    protected String device;

    @BeforeClass(alwaysRun = true)
    public void startDriver(ITestContext context) {
        ExecutionStop.check(context);
        config = ConfigLoader.load();
        String requested = System.getProperty("device", "");
        if (requested.isBlank()) requested = config.appium().deviceName();
        device = Adb.resolveDevice(requested);
        Adb.clearLogcat(device);
        driver = DriverFactory.create(config, device);
        // Page objects already use explicit waits. Optional probes must return immediately.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        RunEvidence.start(driver, getClass().getSimpleName());
    }

    @AfterClass(alwaysRun = true)
    public void stopDriver() {
        stopDriver(false);
    }

    private void stopDriver(boolean forceStopApplication) {
        if (driver != null) {
            try { RunEvidence.finish(driver); }
            finally {
                try { driver.quit(); }
                finally {
                    driver = null;
                    if (forceStopApplication)
                        Adb.shell(device, "am", "force-stop", config.application().packageName());
                }
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void recordResult(ITestResult result) {
        try {
            if (driver != null) RunEvidence.result(driver, result);
        } finally {
            if (ExecutionStop.requested(result.getTestContext())) stopDriver(true);
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void checkExecutionState(ITestContext context) {
        ExecutionStop.check(context);
    }

    public AndroidDriver getDriver() { return driver; }
}
