package com.smartaccess.automation.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class Adb {
    private Adb() {}

    public static String resolveDevice(String preferred) {
        List<String> devices = connectedDevices();
        if (preferred != null && !preferred.isBlank()) {
            if (!devices.contains(preferred)) throw new IllegalStateException("Configured device is unavailable: " + preferred);
            return preferred;
        }
        if (devices.isEmpty()) throw new IllegalStateException("No ready Android device found");
        return devices.getFirst();
    }

    public static List<String> connectedDevices() {
        List<String> devices = new ArrayList<>();
        run(Duration.ofSeconds(30), "devices").lines().skip(1)
                .filter(line -> line.trim().endsWith("\tdevice"))
                .map(line -> line.trim().split("\\s+")[0]).forEach(devices::add);
        return devices;
    }

    public static String shell(String device, String... arguments) {
        List<String> command = new ArrayList<>(List.of("-s", device, "shell"));
        command.addAll(Arrays.asList(arguments));
        return run(Duration.ofSeconds(60), command.toArray(String[]::new));
    }

    public static void clearLogcat(String device) { run(Duration.ofSeconds(30), "-s", device, "logcat", "-c"); }
    public static String crashLog(String device) { return run(Duration.ofSeconds(60), "-s", device, "logcat", "-d", "-b", "crash"); }

    private static String run(Duration timeout, String... arguments) {
        List<String> command = new ArrayList<>(List.of("adb"));
        command.addAll(Arrays.asList(arguments));
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            CompletableFuture<String> outputReader = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                } catch (IOException error) {
                    throw new CompletionException(error);
                }
            });
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("ADB command timed out: " + String.join(" ", command));
            }
            String output = outputReader.join();
            if (process.exitValue() != 0) throw new IllegalStateException("ADB command failed: " + output);
            return output;
        } catch (IOException error) {
            throw new IllegalStateException("Unable to run adb", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running adb", error);
        }
    }
}
