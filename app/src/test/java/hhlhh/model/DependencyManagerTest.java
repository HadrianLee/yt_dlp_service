package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DependencyManagerTest {

    private final String originalOsName = System.getProperty("os.name");

    @AfterEach
    void restoreOsName() {
        System.setProperty("os.name", originalOsName);
    }

    @Test
    void binaryNamesUseExeSuffixOnWindows() {
        System.setProperty("os.name", "Windows 11");
        DependencyManager dependencyManager = new DependencyManager();

        assertEquals("yt-dlp.exe", dependencyManager.getYtDlpBinaryName());
        assertEquals("ffmpeg.exe", dependencyManager.getFmpegbinaryName());
    }

    @Test
    void binaryNamesDoNotUseExeSuffixOnLinux() {
        System.setProperty("os.name", "Linux");
        DependencyManager dependencyManager = new DependencyManager();

        assertEquals("yt-dlp", dependencyManager.getYtDlpBinaryName());
        assertEquals("ffmpeg", dependencyManager.getFmpegbinaryName());
    }

    @Test
    void binDirectoryPathComesFromAppPaths() {
        DependencyManager dependencyManager = new DependencyManager();

        assertEquals(AppPaths.binDirectory().toAbsolutePath().toString(), dependencyManager.getBinDirectoryPath());
    }
}
