package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

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

    @Test
    void ensureDependenciesExistDownloadsMissingWindowsBinaries(@TempDir Path tempDir) throws Exception {
        System.setProperty("os.name", "Windows 11");
        FakeHttpClient httpClient = new FakeHttpClient(
                Map.of(
                        "yt-dlp.exe", "downloaded yt-dlp".getBytes(),
                        "ffmpeg-6.1-win-64.zip", zipBytes("ffmpeg-6.1-win-64/bin/ffmpeg.exe", "downloaded ffmpeg")
                )
        );
        DependencyManager dependencyManager = new DependencyManager(tempDir.resolve("bin"), httpClient);

        dependencyManager.ensureDependenciesExist();

        assertEquals("downloaded yt-dlp", Files.readString(tempDir.resolve("bin").resolve("yt-dlp.exe")));
        assertEquals("downloaded ffmpeg", Files.readString(tempDir.resolve("bin").resolve("ffmpeg.exe")));
        assertEquals(2, httpClient.requestUris().size());
        assertTrue(httpClient.requestUris().stream().anyMatch(uri -> uri.toString().endsWith("/yt-dlp.exe")));
        assertTrue(httpClient.requestUris().stream().anyMatch(uri -> uri.toString().endsWith("ffmpeg-6.1-win-64.zip")));
    }

    @Test
    void ensureDependenciesExistKeepsExistingBinariesWithoutDownloading(@TempDir Path tempDir) throws Exception {
        System.setProperty("os.name", "Linux");
        Path binDirectory = tempDir.resolve("bin");
        Files.createDirectories(binDirectory);
        Files.writeString(binDirectory.resolve("yt-dlp"), "existing yt-dlp");
        Files.writeString(binDirectory.resolve("ffmpeg"), "existing ffmpeg");
        FakeHttpClient httpClient = new FakeHttpClient(Map.of());
        DependencyManager dependencyManager = new DependencyManager(binDirectory, httpClient);

        dependencyManager.ensureDependenciesExist();

        assertEquals("existing yt-dlp", Files.readString(binDirectory.resolve("yt-dlp")));
        assertEquals("existing ffmpeg", Files.readString(binDirectory.resolve("ffmpeg")));
        assertTrue(httpClient.requestUris().isEmpty());
    }

    @Test
    void ensureDependenciesExistRejectsUnsupportedOperatingSystemBeforeCreatingBinDirectory(@TempDir Path tempDir) {
        System.setProperty("os.name", "Plan 9");
        Path binDirectory = tempDir.resolve("bin");
        DependencyManager dependencyManager = new DependencyManager(binDirectory, new FakeHttpClient(Map.of()));

        assertThrows(IllegalStateException.class, dependencyManager::ensureDependenciesExist);
        assertTrue(Files.notExists(binDirectory));
    }

    private byte[] zipBytes(String entryName, String content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes());
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    private static final class FakeHttpClient extends HttpClient {

        private final Map<String, byte[]> responsesByUrlSuffix;
        private final List<URI> requestUris = new ArrayList<>();

        private FakeHttpClient(Map<String, byte[]> responsesByUrlSuffix) {
            this.responsesByUrlSuffix = responsesByUrlSuffix;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.ALWAYS;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            requestUris.add(request.uri());
            byte[] body = responsesByUrlSuffix.entrySet().stream()
                    .filter(entry -> request.uri().toString().endsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Unexpected request: " + request.uri()));
            return (HttpResponse<T>) new FakeHttpResponse(request, new ByteArrayInputStream(body));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("sendAsync is not used"));
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException("sendAsync is not used"));
        }

        private List<URI> requestUris() {
            return requestUris;
        }
    }

    private record FakeHttpResponse(HttpRequest request, InputStream body) implements HttpResponse<InputStream> {

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
