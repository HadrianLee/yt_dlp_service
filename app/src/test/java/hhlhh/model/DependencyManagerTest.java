package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import hhlhh.test.fake.FakeDependencyManager;

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
    void binaryNamesDoNotUseExeSuffixOnMacOS() {
        System.setProperty("os.name", "Mac OS X");
        DependencyManager dependencyManager = new DependencyManager();

        assertEquals("yt-dlp", dependencyManager.getYtDlpBinaryName());
        assertEquals("ffmpeg", dependencyManager.getFmpegbinaryName());
    }

    @Test
    void binDirectoryPathComesFromAppPaths() {
        DependencyManager dependencyManager = new DependencyManager();

        assertEquals(AppPaths.binDirectory().toAbsolutePath().toString(), dependencyManager.getBinDirectoryPath());
    }

    @Test
    void repairDependenciesRecreatesManagedBinaries(@TempDir Path tempDir) throws Exception {
        FakeDependencyManager dependencyManager = new FakeDependencyManager(tempDir);
        dependencyManager.ensureDependenciesExist();
        Files.writeString(dependencyManager.ytDlpPath(), "stale yt-dlp");
        Files.writeString(dependencyManager.ffmpegPath(), "stale ffmpeg");

        dependencyManager.repairDependencies();

        assertEquals("fake binary", Files.readString(dependencyManager.ytDlpPath()));
        assertEquals("fake binary", Files.readString(dependencyManager.ffmpegPath()));
    }
}
