package com.smartaccess.automation.util;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.AndroidStartScreenRecordingOptions;
import org.openqa.selenium.OutputType;
import org.testng.ITestResult;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/** Collects per-session evidence; media failures remain visible in the report. */
public final class RunEvidence {
    private static final Map<AndroidDriver, RunEvidence> ACTIVE = new ConcurrentHashMap<>();
    private final AndroidDriver driver;
    private final Path directory;
    private final List<String> entries = new ArrayList<>();
    private final ScheduledExecutorService recorder = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "flow-recording");
        thread.setDaemon(true);
        return thread;
    });
    private boolean recording;
    private boolean finished;
    private int images;
    private int videos;
    private int iteration;
    private int iterationTotal;
    private boolean iterationActive;

    public static void loopsConfigured(AndroidDriver driver, int total) {
        RunEvidence evidence = ACTIVE.get(driver);
        if (evidence != null) evidence.note("Loops configured: " + total);
    }

    public static void iterationStarted(AndroidDriver driver, int number, int total) {
        RunEvidence evidence = ACTIVE.get(driver);
        if (evidence == null) return;
        synchronized (evidence) {
            evidence.iteration = number;
            evidence.iterationTotal = total;
            evidence.iterationActive = true;
            evidence.note("Iteration " + number + "/" + total + " — RUNNING");
        }
    }

    public static void iterationCompleted(AndroidDriver driver) {
        RunEvidence evidence = ACTIVE.get(driver);
        if (evidence == null) return;
        synchronized (evidence) {
            evidence.note("Iteration " + evidence.iteration + "/" + evidence.iterationTotal + " — PASS");
            evidence.iterationActive = false;
            evidence.iteration = 0;
        }
    }

    private RunEvidence(AndroidDriver driver, String name) throws Exception {
        this.driver = driver;
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        directory = Path.of("reports", "flows", stamp + "-" + name).toAbsolutePath();
        Files.createDirectories(directory);
    }

    public static void start(AndroidDriver driver, String name) {
        try {
            RunEvidence evidence = new RunEvidence(driver, name);
            ACTIVE.put(driver, evidence);
            evidence.startVideo();
            evidence.recorder.scheduleWithFixedDelay(evidence::rotateVideo, 150, 150, TimeUnit.SECONDS);
            evidence.screenshot("Application launched");
            System.out.println("[Evidence] " + evidence.directory.resolve("index.html"));
        } catch (Exception error) {
            throw new IllegalStateException("Cannot create execution evidence report", error);
        }
    }

    public static void capture(AndroidDriver driver, String label) {
        RunEvidence evidence = ACTIVE.get(driver);
        if (evidence != null) evidence.screenshot(label);
    }

    public static boolean isActive(AndroidDriver driver) {
        return ACTIVE.containsKey(driver);
    }

    public static void result(AndroidDriver driver, ITestResult result) {
        RunEvidence evidence = ACTIVE.get(driver);
        if (evidence == null) return;
        String status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> "PASS";
            case ITestResult.SKIP -> "SKIPPED";
            default -> "FAIL";
        };
        evidence.note(status + " — " + result.getName() +
                (result.getThrowable() == null ? "" : ": " + result.getThrowable().getMessage()));
        evidence.screenshot(status + " — " + result.getName());
        synchronized (evidence) {
            if (evidence.iterationActive) {
                evidence.note("Iteration " + evidence.iteration + "/" + evidence.iterationTotal + " — " + status);
                evidence.iterationActive = false;
            }
        }
    }

    public static void finish(AndroidDriver driver) {
        RunEvidence evidence = ACTIVE.remove(driver);
        if (evidence != null) evidence.finish();
    }

    private synchronized void screenshot(String label) {
        if (finished) return;
        if (iteration > 0) label = "[Iteration " + iteration + "/" + iterationTotal + "] " + label;
        try {
            String file = String.format("step-%04d.png", ++images);
            Files.write(directory.resolve(file), driver.getScreenshotAs(OutputType.BYTES));
            entries.add("<figure><figcaption>" + images + ". " + escape(label) + " — "
                    + LocalTime.now() + "</figcaption><a href='" + file + "'><img loading='lazy' src='"
                    + file + "' alt='" + escape(label) + "'></a></figure>");
            render();
        } catch (Exception error) { note("Screenshot unavailable: " + error.getMessage()); }
    }

    private synchronized void startVideo() {
        try {
            driver.startRecordingScreen(new AndroidStartScreenRecordingOptions()
                    .withTimeLimit(Duration.ofSeconds(180)));
            recording = true;
        } catch (Exception error) { note("Recording unavailable: " + error.getMessage()); }
    }

    private synchronized void saveVideo() {
        if (!recording) return;
        recording = false;
        try {
            String data = driver.stopRecordingScreen();
            if (data == null || data.isBlank()) throw new IllegalStateException("Empty recording returned");
            String file = String.format("flow-%03d.mp4", ++videos);
            Files.write(directory.resolve(file), Base64.getDecoder().decode(data));
            entries.add("<section><h2>Flow recording — part " + videos + "</h2><video controls preload='metadata' src='"
                    + file + "'></video><p><a href='" + file + "'>Download recording</a></p></section>");
            render();
        } catch (Exception error) { note("Recording could not be saved: " + error.getMessage()); }
    }

    private synchronized void rotateVideo() {
        if (finished) return;
        saveVideo();
        startVideo();
    }

    private synchronized void finish() {
        recorder.shutdownNow();
        saveVideo();
        finished = true;
        note("Execution finished. Screenshots: " + images + "; recording parts: " + videos);
    }

    private synchronized void note(String message) {
        entries.add("<p class='note'>" + escape(message) + "</p>");
        try { render(); } catch (Exception error) {
            System.err.println("[Evidence] Report write failed: " + error.getMessage());
        }
    }

    private void render() throws Exception {
        try (var stream = RunEvidence.class.getResourceAsStream("/report-dashboard.html")) {
            if (stream == null) throw new IllegalStateException("Report dashboard template is missing");
            String template = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            Files.writeString(directory.resolve("index.html"), template
                    .replace("@@RUN_NAME@@", escape(directory.getFileName().toString()))
                    .replace("@@ENTRIES@@", String.join("\n", entries)));
        }
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
