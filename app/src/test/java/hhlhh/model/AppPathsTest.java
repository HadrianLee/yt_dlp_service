package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Locale;

import org.junit.jupiter.api.Test;

class AppPathsTest {

    @Test
    void appManagedPathsShareAppDirectory() {
        Path appDirectory = AppPaths.appDirectory();

        assertEquals(".yt_dlp_service", appDirectory.getFileName().toString());
        assertEquals(appDirectory.resolve("bin"), AppPaths.binDirectory());
        assertEquals(appDirectory.resolve("consent.properties"), AppPaths.consentCookie());
    }

    @Test
    void defaultDownloadPathIsAbsoluteAndFollowsCurrentPlatformPolicy() {
        Path defaultDownloadPath = AppPaths.defaultDownloadPath();

        assertTrue(defaultDownloadPath.isAbsolute());
        assertEquals(defaultDownloadPath.normalize(), defaultDownloadPath);

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            assertEquals(Path.of(System.getProperty("user.home"), "Downloads").toAbsolutePath().normalize(), defaultDownloadPath);
        } else {
            assertEquals(Path.of(System.getProperty("user.dir"), "downloads").toAbsolutePath().normalize(), defaultDownloadPath);
        }
    }
}
