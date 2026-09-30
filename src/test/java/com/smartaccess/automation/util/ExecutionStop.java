package com.smartaccess.automation.util;

import org.testng.ITestContext;
import org.testng.Reporter;
import org.testng.SkipException;

/** Ends the current flow and prevents subsequent tests from reopening the app. */
public final class ExecutionStop {
    private static final String KEY = ExecutionStop.class.getName();

    private ExecutionStop() {}

    public static void requestStop(String reason) {
        var result = Reporter.getCurrentTestResult();
        if (result != null) result.getTestContext().getSuite().setAttribute(KEY, reason);
    }

    public static void afterLogout(String reason) {
        requestStop(reason);
        throw new SkipException(reason);
    }

    public static boolean requested(ITestContext context) {
        return context.getSuite().getAttribute(KEY) != null;
    }

    public static void check(ITestContext context) {
        if (requested(context))
            throw new SkipException("Execution ended after logout: " + context.getSuite().getAttribute(KEY));
    }
}
