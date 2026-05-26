package hhlhh.desktop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppIconServiceTest {

    private final String originalOsName = System.getProperty("os.name");

    @AfterEach
    void restoreOsName() {
        System.setProperty("os.name", originalOsName);
    }

    @Test
    void currentOperatingSystemDetectsSupportedPlatforms() {
        System.setProperty("os.name", "Windows 11");
        assertEquals(AppIconService.OperatingSystem.WINDOWS, AppIconService.currentOperatingSystem());

        System.setProperty("os.name", "Mac OS X");
        assertEquals(AppIconService.OperatingSystem.MACOS, AppIconService.currentOperatingSystem());

        System.setProperty("os.name", "Darwin");
        assertEquals(AppIconService.OperatingSystem.MACOS, AppIconService.currentOperatingSystem());

        System.setProperty("os.name", "Linux");
        assertEquals(AppIconService.OperatingSystem.LINUX, AppIconService.currentOperatingSystem());

        System.setProperty("os.name", "Solaris");
        assertEquals(AppIconService.OperatingSystem.OTHER, AppIconService.currentOperatingSystem());
    }

    @Test
    void packageIconResourceMatchesPlatformPackagingFormat() {
        System.setProperty("os.name", "Windows 11");
        assertEquals("/hhlhh/icon/app.ico", AppIconService.packageIconResource());

        System.setProperty("os.name", "Mac OS X");
        assertEquals("/hhlhh/icon/app.icns", AppIconService.packageIconResource());

        System.setProperty("os.name", "Linux");
        assertEquals("/hhlhh/icon/app.png", AppIconService.packageIconResource());
    }

    @Test
    void runtimeIconResourceUsesPngIconAndCanLoadTrayImage() {
        assertEquals("/hhlhh/icon/app.png", AppIconService.runtimeIconResource());
        assertTrue(AppIconService.loadTrayIcon().isPresent());
    }

    @Test
    void loadJavaFxIconDoesNotThrowWhenResourceIsPresent() {
        Optional<javafx.scene.image.Image> icon = AppIconService.loadJavaFxIcon();

        assertTrue(icon.isEmpty() || !icon.get().isError());
    }
}
