package hhlhh.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DownloaderTest {

    @TempDir
    Path tempDir;

    private final Downloader downloader = new Downloader(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void isValidYoutubeUrlAcceptsYoutubeHosts() {
        String videoId = UUID.randomUUID().toString();

        assertTrue(downloader.isValidYoutubeUrl("https://youtube.com/watch?v=" + videoId));
        assertTrue(downloader.isValidYoutubeUrl("https://www.youtube.com/watch?v=" + videoId));
        assertTrue(downloader.isValidYoutubeUrl("https://music.youtube.com/watch?v=" + videoId));
        assertTrue(downloader.isValidYoutubeUrl("https://youtu.be/" + videoId));
    }

    @Test
    void isValidYoutubeUrlAcceptsTrimmedHttpAndUppercaseHosts() {
        String videoId = UUID.randomUUID().toString();

        assertTrue(downloader.isValidYoutubeUrl(" HTTP://YOUTUBE.COM/watch?v=" + videoId + " "));
        assertTrue(downloader.isValidYoutubeUrl(" https://WWW.YOUTUBE.COM/shorts/" + videoId + " "));
    }

    @Test
    void isValidYoutubeUrlRejectsBlankNonHttpAndNonYoutubeUrls() {
        String videoId = UUID.randomUUID().toString();

        assertFalse(downloader.isValidYoutubeUrl(""));
        assertFalse(downloader.isValidYoutubeUrl(null));
        assertFalse(downloader.isValidYoutubeUrl("ftp://youtube.com/watch?v=" + videoId));
        assertFalse(downloader.isValidYoutubeUrl("https://example.com/watch?v=" + videoId));
        assertFalse(downloader.isValidYoutubeUrl("https://youtube.com.example.com/watch?v=" + videoId));
        assertFalse(downloader.isValidYoutubeUrl("https://youtu.be.example.com/" + videoId));
        assertFalse(downloader.isValidYoutubeUrl("https:///watch?v=" + videoId));
        assertFalse(downloader.isValidYoutubeUrl("not a url"));
    }

    @Test
    void toDisplayNameSanitizesBlankAndUnsafeCharacters() {
        String token = UUID.randomUUID().toString().substring(0, 8);

        assertEquals("Untitled", downloader.toDisplayName(" "));
        assertEquals("A_" + token, downloader.toDisplayName(" A @ " + token));
        assertEquals("Untitled", downloader.toDisplayName(" ?! "));
    }

    @Test
    void toDisplayNameTruncatesAndRemovesTrailingUnsafeCharacters() {
        String longToken = UUID.randomUUID().toString();
        String shortToken = UUID.randomUUID().toString().substring(0, 8);
        String longTitle = longToken + "-extra-text";

        assertEquals(longTitle.substring(0, 40), downloader.toDisplayName(longTitle));
        assertEquals(shortToken, downloader.toDisplayName(shortToken + "!!!"));
        assertEquals("日本語タイトル_" + shortToken, downloader.toDisplayName("日本語タイトル " + shortToken));
    }

    @Test
    void toDisplayNameKeepsReadableFileCharactersAndCollapsesSeparators() {
        String token = UUID.randomUUID().toString().substring(0, 8);

        assertEquals("Some.Title-01_" + token, downloader.toDisplayName(" Some.Title-01 / " + token + " "));
        assertEquals("Untitled", downloader.toDisplayName(".___---"));
    }

    @Test
    void countFilesReturnsRegularFileCountRecursively() throws Exception {
        Path nestedDirectory = tempDir.resolve("nested-" + UUID.randomUUID());
        Files.createDirectories(nestedDirectory);
        Files.writeString(tempDir.resolve("one-" + UUID.randomUUID() + ".txt"), "");
        Files.writeString(nestedDirectory.resolve("two-" + UUID.randomUUID() + ".txt"), "");

        assertEquals(2, downloader.countFiles(tempDir));
    }

    @Test
    void countFilesReturnsZeroForMissingDirectory() {
        assertEquals(0, downloader.countFiles(tempDir.resolve("missing")));
    }

    @Test
    void countFilesReturnsZeroForNullDirectory() {
        assertEquals(0, downloader.countFiles(null));
    }
}
