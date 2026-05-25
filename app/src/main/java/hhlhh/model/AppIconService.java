package hhlhh.model;

import java.io.IOException;
import java.util.Locale;
import java.util.Optional;

import javax.imageio.ImageIO;

public final class AppIconService {

    private static final String PNG_ICON = "/hhlhh/icon/app.png";
    private static final String WINDOWS_ICON = "/hhlhh/icon/app.ico";
    private static final String MACOS_ICON = "/hhlhh/icon/app.icns";

    private AppIconService() {
    }

    public static OperatingSystem currentOperatingSystem() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            return OperatingSystem.LINUX;
        }
        return OperatingSystem.OTHER;
    }

    public static String runtimeIconResource() {
        return PNG_ICON;
    }

    public static String packageIconResource() {
        return switch (currentOperatingSystem()) {
            case WINDOWS -> WINDOWS_ICON;
            case MACOS -> MACOS_ICON;
            case LINUX, OTHER -> PNG_ICON;
        };
    }

    public static Optional<javafx.scene.image.Image> loadJavaFxIcon() {
        try (var inputStream = AppIconService.class.getResourceAsStream(runtimeIconResource())) {
            if (inputStream != null) {
                return Optional.of(new javafx.scene.image.Image(inputStream));
            }
        } catch (Exception e) {
            // A missing or unreadable icon should not block app startup.
        }
        return Optional.empty();
    }

    public static Optional<java.awt.Image> loadTrayIcon() {
        try (var inputStream = AppIconService.class.getResourceAsStream(runtimeIconResource())) {
            if (inputStream != null) {
                return Optional.ofNullable(ImageIO.read(inputStream));
            }
        } catch (IOException e) {
            // Fall back to the generated tray image in SystemTrayService.
        }
        return Optional.empty();
    }

    public enum OperatingSystem {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }
}
