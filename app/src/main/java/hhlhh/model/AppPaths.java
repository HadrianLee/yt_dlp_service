package hhlhh.model;

import java.nio.file.Path;
import java.util.Locale;

public final class AppPaths {

    private static final String APP_DIR_NAME = ".yt_dlp_service";
    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), APP_DIR_NAME);
    private static final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

    private AppPaths() {
    }

    public static Path appDirectory() {
        return APP_DIR;
    }

    public static Path binDirectory() {
        return APP_DIR.resolve("bin");
    }

    public static Path consentCookie() {
        return APP_DIR.resolve("consent.properties");
    }

    public static Path defaultDownloadPath() {
        if (os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Downloads").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir"), "downloads").toAbsolutePath().normalize();
    };
}
