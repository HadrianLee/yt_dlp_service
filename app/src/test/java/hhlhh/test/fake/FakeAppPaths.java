package hhlhh.test.fake;

import java.nio.file.Path;
import java.util.UUID;

public final class FakeAppPaths {

    private final Path appDirectory;
    private final Path binDirectory;
    private final Path consentCookie;

    public FakeAppPaths(Path tempDirectory) {
        this.appDirectory = tempDirectory.resolve("app-" + UUID.randomUUID());
        this.binDirectory = appDirectory.resolve("bin");
        this.consentCookie = appDirectory.resolve("consent-" + UUID.randomUUID() + ".properties");
    }

    public Path appDirectory() {
        return appDirectory;
    }

    public Path binDirectory() {
        return binDirectory;
    }

    public Path consentCookie() {
        return consentCookie;
    }
}
