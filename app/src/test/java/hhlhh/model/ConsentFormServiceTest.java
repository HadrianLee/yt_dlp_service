package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

import hhlhh.test.fake.FakeAppPaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConsentFormServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void hasConsentCookieReturnsFalseWhenCookieDoesNotExist() {
        FakeAppPaths appPaths = new FakeAppPaths(tempDir);
        ConsentFormService service = new ConsentFormService(appPaths.appDirectory(), appPaths.consentCookie());

        assertFalse(service.hasConsentCookie());
    }

    @Test
    void writeConsentCookieCreatesExpectedProperties() throws Exception {
        FakeAppPaths appPaths = new FakeAppPaths(tempDir);
        ConsentFormService service = new ConsentFormService(appPaths.appDirectory(), appPaths.consentCookie());

        service.writeConsentCookie();

        assertTrue(service.hasConsentCookie());
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(appPaths.consentCookie())) {
            properties.load(inputStream);
        }

        assertEquals("true", properties.getProperty("accepted"));
        assertEquals(appPaths.appDirectory().toAbsolutePath().toString(), properties.getProperty("appDataDirectory"));
        assertTrue(Instant.parse(properties.getProperty("acceptedAt")).isBefore(Instant.now().plusSeconds(1)));
    }

    @Test
    void getAppDirectoryPathReturnsAbsolutePath() {
        FakeAppPaths appPaths = new FakeAppPaths(tempDir);
        ConsentFormService service = new ConsentFormService(appPaths.appDirectory(), appPaths.consentCookie());

        assertEquals(appPaths.appDirectory().toAbsolutePath().toString(), service.getAppDirectoryPath());
    }
}
