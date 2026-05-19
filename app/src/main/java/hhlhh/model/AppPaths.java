package hhlhh.model;

import java.nio.file.Path;

public final class AppPaths {

    private static final String APP_DIR_NAME = ".yt_dlp_service";
    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), APP_DIR_NAME);

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
}
