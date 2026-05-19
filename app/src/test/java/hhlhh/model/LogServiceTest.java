package hhlhh.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void writePlaylistCsvCreatesDirectoryAndHeaderWhenNoMp3FilesExist() throws Exception {
        Path outputDirectory = tempDir.resolve("playlist-" + UUID.randomUUID());
        LogService logService = new LogService();

        Path csvPath = logService.writePlaylistCsv(outputDirectory);

        assertEquals(outputDirectory.resolve("playlist_log.csv"), csvPath);
        assertEquals(List.of("index,file_name"), Files.readAllLines(csvPath));
    }

    @Test
    void writePlaylistCsvSortsMp3FilesByLeadingIndexThenName() throws Exception {
        String earlyTitle = "Earlier-" + UUID.randomUUID();
        String laterTitle = "Later-" + UUID.randomUUID();
        String noIndexA = "No Index A-" + UUID.randomUUID() + ".mp3";
        String noIndexB = "No Index B-" + UUID.randomUUID() + ".mp3";
        Files.writeString(tempDir.resolve("10 - " + laterTitle + ".mp3"), "");
        Files.writeString(tempDir.resolve("2 - " + earlyTitle + ".mp3"), "");
        Files.writeString(tempDir.resolve(noIndexB), "");
        Files.writeString(tempDir.resolve(noIndexA), "");
        Files.writeString(tempDir.resolve("ignored.txt"), "");
        LogService logService = new LogService();

        Path csvPath = logService.writePlaylistCsv(tempDir);

        assertEquals(List.of(
                "index,file_name",
                "2,2 - " + earlyTitle + ".mp3",
                "10,10 - " + laterTitle + ".mp3",
                "3," + noIndexA,
                "4," + noIndexB
        ), Files.readAllLines(csvPath));
    }

    @Test
    void writePlaylistCsvEscapesCsvSpecialCharacters() throws Exception {
        String fileName = "1 - Title, " + UUID.randomUUID() + ".mp3";
        Files.writeString(tempDir.resolve(fileName), "");
        LogService logService = new LogService();

        Path csvPath = logService.writePlaylistCsv(tempDir);

        List<String> lines = Files.readAllLines(csvPath);
        assertTrue(lines.contains("1,\"" + fileName + "\""));
    }

    @Test
    void escapeCsvEscapesQuotesAndNewlines() {
        LogService logService = new LogService();
        String token = UUID.randomUUID().toString();

        assertEquals("\"Title \"\"" + token + "\"\"\"", logService.escapeCsv("Title \"" + token + "\""));
        assertEquals("\"Title\n" + token + "\"", logService.escapeCsv("Title\n" + token));
    }
}
