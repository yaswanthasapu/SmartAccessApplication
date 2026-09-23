package com.smartaccess.automation.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static RunConfig cached;

    private ConfigLoader() {}

    public static synchronized RunConfig load() {
        if (cached != null) return cached;
        String configuredPath = System.getProperty("testConfig", "config/uat1.json");
        Path path = Path.of(configuredPath);
        if (!path.isAbsolute()) path = PROJECT_ROOT.resolve(path).normalize();
        if (!Files.isRegularFile(path)) throw new IllegalStateException("Test configuration not found: " + path);

        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            cached = mapper.readValue(path.toFile(), RunConfig.class);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read test configuration: " + path, error);
        }
        if (cached.appium() == null || cached.application() == null)
            throw new IllegalStateException("Missing appium or application section in " + path);
        if (!Files.isRegularFile(apkPath())) throw new IllegalStateException("APK not found: " + apkPath());
        return cached;
    }

    public static Path projectRoot() { return PROJECT_ROOT; }

    public static Path apkPath() {
        Path configured = Path.of(cached.application().apkPath());
        return configured.isAbsolute() ? configured : PROJECT_ROOT.resolve(configured).normalize();
    }
}
