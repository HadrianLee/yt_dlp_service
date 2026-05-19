package hhlhh.model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LogService {

    private static final String CSV_FILE_NAME = "playlist_log.csv";
    private static final Pattern LEADING_INDEX = Pattern.compile("^(\\d+)");

    public Path writePlaylistCsv(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);

        Path csvPath = outputDirectory.resolve(CSV_FILE_NAME);
        List<DownloadedFile> downloadedFiles = findDownloadedMp3Files(outputDirectory);

        try (BufferedWriter writer = Files.newBufferedWriter(csvPath, StandardCharsets.UTF_8)) {
            writer.write("index,file_name");
            writer.newLine();

            for (int i = 0; i < downloadedFiles.size(); i++) {
                DownloadedFile file = downloadedFiles.get(i);
                int index = file.index() > 0 ? file.index() : i + 1;
                writer.write(index + "," + escapeCsv(file.fileName()));
                writer.newLine();
            }
        }

        return csvPath;
    }

    private List<DownloadedFile> findDownloadedMp3Files(Path outputDirectory) throws IOException {
        List<DownloadedFile> files = new ArrayList<>();

        try (Stream<Path> paths = Files.list(outputDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isMp3File)
                    .map(this::toDownloadedFile)
                    .forEach(files::add);
        }

        files.sort(Comparator
                .comparingInt((DownloadedFile file) -> file.index() > 0 ? file.index() : Integer.MAX_VALUE)
                .thenComparing(DownloadedFile::fileName, String.CASE_INSENSITIVE_ORDER));

        return files;
    }

    private boolean isMp3File(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".mp3");
    }

    private DownloadedFile toDownloadedFile(Path path) {
        String fileName = path.getFileName().toString();
        Matcher matcher = LEADING_INDEX.matcher(fileName);
        int index = matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
        return new DownloadedFile(index, fileName);
    }

    String escapeCsv(String value) {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record DownloadedFile(int index, String fileName) {
    }
}
