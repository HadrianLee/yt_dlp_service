package hhlhh.model;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public class ConsentFormService {

    private final Path appDirectory;
    private final Path consentCookie;

    public ConsentFormService() {
        this(AppPaths.appDirectory(), AppPaths.consentCookie());
    }

    ConsentFormService(Path appDirectory, Path consentCookie) {
        this.appDirectory = appDirectory;
        this.consentCookie = consentCookie;
    }

    public boolean hasConsentCookie() {
        return Files.isRegularFile(consentCookie);
    }

    public void writeConsentCookie() throws IOException {
        Files.createDirectories(appDirectory);

        Properties properties = new Properties();
        properties.setProperty("accepted", "true");
        properties.setProperty("acceptedAt", Instant.now().toString());
        properties.setProperty("appDataDirectory", appDirectory.toAbsolutePath().toString());

        try (OutputStream outputStream = Files.newOutputStream(consentCookie)) {
            properties.store(outputStream, "yt-dlp Service consent");
        }
    }

    public String getAppDirectoryPath() {
        return appDirectory.toAbsolutePath().toString();
    }
}
