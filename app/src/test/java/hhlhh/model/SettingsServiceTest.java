package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import hhlhh.test.fake.FakeDependencyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsUseCloseToTrayPreference() {
        SettingsService settingsService = new SettingsService(
                tempDir.resolve("settings.properties"),
                new FakeDependencyManager(tempDir)
        );

        assertFalse(settingsService.isDarkMode());
        assertTrue(settingsService.shouldCloseToTrayDuringDownload());
        assertTrue(settingsService.isNotificationsEnabled());
        assertFalse(settingsService.shouldUsePostprocessPipeline());
    }

    @Test
    void closeToTrayPreferenceCanBeChanged() {
        SettingsService settingsService = new SettingsService(
                tempDir.resolve("settings.properties"),
                new FakeDependencyManager(tempDir)
        );

        assertTrue(settingsService.shouldCloseToTrayDuringDownload());

        settingsService.setCloseToTrayDuringDownload(false);

        assertFalse(settingsService.shouldCloseToTrayDuringDownload());
    }

    @Test
    void loadsSettingsFromPropertiesFile() throws Exception {
        Path settingsPath = tempDir.resolve("settings.properties");
        Properties properties = new Properties();
        properties.setProperty("darkMode", "true");
        properties.setProperty("consumeExitDuringDownload", "false");
        properties.setProperty("notificationsEnabled", "false");
        properties.setProperty("usePostprocessPipeline", "true");
        Files.createDirectories(settingsPath.getParent());
        try (var output = Files.newOutputStream(settingsPath)) {
            properties.store(output, "test");
        }

        SettingsService settingsService = new SettingsService(
                settingsPath,
                new FakeDependencyManager(tempDir)
        );

        assertTrue(settingsService.isDarkMode());
        assertFalse(settingsService.shouldCloseToTrayDuringDownload());
        assertFalse(settingsService.isNotificationsEnabled());
        assertTrue(settingsService.shouldUsePostprocessPipeline());
    }

    @Test
    void persistsSettingsWhenPropertiesChange() throws Exception {
        Path settingsPath = tempDir.resolve("settings.properties");
        SettingsService settingsService = new SettingsService(
                settingsPath,
                new FakeDependencyManager(tempDir)
        );

        settingsService.setDarkMode(true);
        settingsService.setCloseToTrayDuringDownload(false);
        settingsService.setNotificationsEnabled(false);
        settingsService.setUsePostprocessPipeline(true);

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsPath)) {
            properties.load(input);
        }

        assertEquals("true", properties.getProperty("darkMode"));
        assertEquals("false", properties.getProperty("consumeExitDuringDownload"));
        assertEquals("false", properties.getProperty("notificationsEnabled"));
        assertEquals("true", properties.getProperty("usePostprocessPipeline"));
    }

    @Test
    void exposesDependencyDirectoryAndRepairsDependencies() throws Exception {
        FakeDependencyManager dependencyManager = new FakeDependencyManager(tempDir);
        SettingsService settingsService = new SettingsService(
                tempDir.resolve("settings.properties"),
                dependencyManager
        );

        assertEquals(dependencyManager.getBinDirectoryPath(), settingsService.getDependencyDirectoryPath());

        settingsService.repairDependencies();

        assertTrue(Files.isRegularFile(dependencyManager.ytDlpPath()));
        assertTrue(Files.isRegularFile(dependencyManager.ffmpegPath()));
    }
}
