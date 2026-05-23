package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import hhlhh.test.fake.FakeDependencyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void inspectUrlRejectsBlankUrlBeforeRunningDependencies() {
        DownloadService service = new DownloadService(new FakeDependencyManager(tempDir), new LogService());

        assertThrows(IllegalArgumentException.class, () -> service.inspectUrl(" "));
    }

    @Test
    void inspectUrlParsesPlaylistJson() throws Exception {
        TestDownloadService service = new TestDownloadService(new FakeDependencyManager(tempDir), new LogService());
        String titleToken = "Mix-" + UUID.randomUUID();
        String thumbnailUrl = "https://example.com/" + UUID.randomUUID() + "/thumb.jpg";
        service.outputLines = List.of("""
                {"_type":"playlist","title":"%s","thumbnail":"%s","entries":[]}
                """.formatted(titleToken, thumbnailUrl.replace("/", "\\/")));

        String url = "https://youtube.com/playlist?list=" + UUID.randomUUID();
        DownloadService.UrlInspection inspection = service.inspectUrl(" " + url + " ");

        assertEquals(url, inspection.url());
        assertTrue(inspection.playlist());
        assertEquals(titleToken, inspection.title());
        assertEquals(thumbnailUrl, inspection.thumbnailUrl());
        assertTrue(service.lastCommand.contains("--dump-single-json"));
    }

    @Test
    void inspectUrlUsesLastThumbnailFromThumbnailsArray() throws Exception {
        TestDownloadService service = new TestDownloadService(new FakeDependencyManager(tempDir), new LogService());
        String title = "Video-" + UUID.randomUUID();
        String smallThumbnail = "small-" + UUID.randomUUID() + ".jpg";
        String largeThumbnail = "large-" + UUID.randomUUID() + ".jpg";
        service.outputLines = List.of("""
                {"title":"%s","thumbnails":[{"url":"%s"},{"url":"%s"}]}
                """.formatted(title, smallThumbnail, largeThumbnail));

        DownloadService.UrlInspection inspection = service.inspectUrl("https://youtu.be/" + UUID.randomUUID());

        assertFalse(inspection.playlist());
        assertEquals(title, inspection.title());
        assertEquals(largeThumbnail, inspection.thumbnailUrl());
    }

    @Test
    void inspectUrlDecodesUnicodeEscapedTitle() throws Exception {
        TestDownloadService service = new TestDownloadService(new FakeDependencyManager(tempDir), new LogService());
        service.outputLines = List.of("""
                {"title":"\\u65e5\\u672c\\u8a9e\\u30bf\\u30a4\\u30c8\\u30eb \\u0026 Mix"}
                """);

        DownloadService.UrlInspection inspection = service.inspectUrl("https://youtu.be/" + UUID.randomUUID());

        assertFalse(inspection.playlist());
        assertEquals("\u65e5\u672c\u8a9e\u30bf\u30a4\u30c8\u30eb & Mix", inspection.title());
    }

    @Test
    void unescapeJsonStringDecodesCommonEscapes() {
        DownloadService service = new DownloadService(new FakeDependencyManager(tempDir), new LogService());

        assertEquals("A/B \"C\"\nD", service.unescapeJsonString("A\\/B \\\"C\\\"\\nD"));
    }

    @Test
    void downloadPlaylistRejectsBlankUrlBeforeCreatingOutputDirectory() {
        DownloadService service = new DownloadService(new FakeDependencyManager(tempDir), new LogService());
        Path outputDirectory = tempDir.resolve("output");

        assertThrows(IllegalArgumentException.class, () ->
                service.downloadPlaylist(" ", outputDirectory, false, false, line -> { }));
        assertFalse(outputDirectory.toFile().exists());
    }

    @Test
    void downloadPlaylistUsesPlaylistFolderAndWritesCsvForPlaylist() throws Exception {
        TestDownloadService service = new TestDownloadService(new FakeDependencyManager(tempDir), new LogService());
        Path outputDirectory = tempDir.resolve("downloads-" + UUID.randomUUID());
        String rawTitle = "My Playlist " + UUID.randomUUID();
        String safeTitle = service.toSafeFolderName(rawTitle);
        DownloadService.UrlInspection inspection = new DownloadService.UrlInspection(
                "https://youtube.com/playlist?list=" + UUID.randomUUID(),
                true,
                rawTitle,
                ""
        );

        DownloadService.DownloadResult result = service.downloadPlaylist(
                inspection.url(),
                outputDirectory,
                true,
                inspection,
                line -> { }
        );

        Path playlistDirectory = outputDirectory.resolve(safeTitle);
        assertEquals(0, result.exitCode());
        assertTrue(result.playlist());
        assertEquals(playlistDirectory, result.outputDirectory());
        assertEquals(playlistDirectory.resolve("playlist_log.csv"), result.csvPath());
        assertTrue(service.lastCommand.contains("--verbose"));
        assertTrue(service.lastCommand.contains("--output"));
        assertTrue(service.lastCommand.stream().anyMatch(value -> value.endsWith("%(playlist_index)s - %(title)s.%(ext)s")));
    }

    @Test
    void downloadPlaylistUsesSingleFileTemplateWhenNotPlaylist() throws Exception {
        FakeDependencyManager dependencyManager = new FakeDependencyManager(tempDir);
        TestDownloadService service = new TestDownloadService(dependencyManager, new LogService());
        Path outputDirectory = tempDir.resolve("downloads-" + UUID.randomUUID());

        DownloadService.DownloadResult result = service.downloadPlaylist(
                "https://youtu.be/" + UUID.randomUUID(),
                outputDirectory,
                false,
                false,
                line -> { }
        );

        assertEquals(0, result.exitCode());
        assertFalse(result.playlist());
        assertEquals(outputDirectory, result.outputDirectory());
        assertEquals(null, result.csvPath());
        assertTrue(service.lastCommand.contains("--no-warnings"));
        assertTrue(service.lastCommand.stream().anyMatch(value -> value.endsWith("%(title)s.%(ext)s")));
        assertTrue(Files.isRegularFile(dependencyManager.ytDlpPath()));
        assertTrue(Files.isRegularFile(dependencyManager.ffmpegPath()));
    }

    @Test
    void safeFolderNameFallsBackAndTruncates() {
        DownloadService service = new DownloadService(new FakeDependencyManager(tempDir), new LogService());
        String shortToken = UUID.randomUUID().toString().substring(0, 8);
        String longToken = UUID.randomUUID().toString();
        String longTitle = longToken + "-extra-text";

        assertEquals("playlist", service.toSafeFolderName(" ?! "));
        assertEquals("A_" + shortToken, service.toSafeFolderName(" A @ " + shortToken));
        assertEquals(longTitle.substring(0, 40), service.toSafeFolderName(longTitle));
        assertEquals(
                "\u65e5\u672c\u8a9e\u30d7\u30ec\u30a4\u30ea\u30b9\u30c8_" + shortToken,
                service.toSafeFolderName("\u65e5\u672c\u8a9e\u30d7\u30ec\u30a4\u30ea\u30b9\u30c8 " + shortToken)
        );
        assertEquals("日本語プレイリスト_" + shortToken, service.toSafeFolderName("日本語プレイリスト " + shortToken));
    }

    private static class TestDownloadService extends DownloadService {

        private List<String> outputLines = List.of();
        private List<String> lastCommand = List.of();

        TestDownloadService(DependencyManager dependencyManager, LogService logService) {
            super(dependencyManager, logService);
        }

        @Override
        protected int runCommand(List<String> command, Consumer<String> outputConsumer) {
            lastCommand = new ArrayList<>(command);
            outputLines.forEach(outputConsumer);
            return 0;
        }
    }

}
