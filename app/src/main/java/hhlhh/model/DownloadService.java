package hhlhh.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DownloadService {

    private static final String PLAYLIST_OUTPUT_TEMPLATE = "%(playlist_index)s - %(title)s.%(ext)s";
    private static final String SINGLE_OUTPUT_TEMPLATE = "%(title)s.%(ext)s";
    private static final Pattern THUMBNAIL_PATTERN = Pattern.compile("\"thumbnail\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern THUMBNAILS_ARRAY_PATTERN = Pattern.compile(
            "\"thumbnails\"\\s*:\\s*\\[(.*?)\\]",
            Pattern.DOTALL
    );
    private static final Pattern THUMBNAILS_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN = Pattern.compile("\"title\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");
    private static final int SAFE_FOLDER_NAME_LIMIT = 40;

    private final DependencyManager dependencyManager;
    private final LogService logService;
    private volatile Process currentProcess;
    private volatile boolean stopRequested;

    public DownloadService() {
        this(new DependencyManager(), new LogService());
    }

    public DownloadService(DependencyManager dependencyManager, LogService logService) {
        this.dependencyManager = Objects.requireNonNull(dependencyManager);
        this.logService = Objects.requireNonNull(logService);
    }

    public DownloadResult downloadPlaylist(String url, Path outputDirectory, boolean verbose) throws Exception {
        UrlInspection inspection = inspectUrl(url);
        return downloadPlaylist(url, outputDirectory, verbose, inspection, System.out::println);
    }

    public DownloadResult downloadPlaylist(
            String url,
            Path outputDirectory,
            boolean verbose,
            Consumer<String> outputConsumer
    ) throws Exception {
        UrlInspection inspection = inspectUrl(url);
        return downloadPlaylist(url, outputDirectory, verbose, inspection, outputConsumer);
    }

    public DownloadResult downloadPlaylist(
            String url,
            Path outputDirectory,
            boolean verbose,
            UrlInspection inspection,
            Consumer<String> outputConsumer
    ) throws Exception {
        UrlInspection safeInspection = inspection != null ? inspection : inspectUrl(url);
        Path resolvedOutputDirectory = safeInspection.playlist()
                ? outputDirectory.resolve(toSafeFolderName(safeInspection.title()))
                : outputDirectory;

        return downloadPlaylist(url, resolvedOutputDirectory, verbose, safeInspection.playlist(), outputConsumer);
    }

    public DownloadResult downloadPlaylist(
            String url,
            Path outputDirectory,
            boolean verbose,
            boolean playlist,
            Consumer<String> outputConsumer
    ) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A playlist or video URL is required.");
        }

        Objects.requireNonNull(outputDirectory);
        Consumer<String> safeOutputConsumer = outputConsumer != null ? outputConsumer : line -> { };

        Files.createDirectories(outputDirectory);
        dependencyManager.ensureDependenciesExist();

        Path binDirectory = Path.of(dependencyManager.getBinDirectoryPath());
        Path ytDlpPath = binDirectory.resolve(dependencyManager.getYtDlpBinaryName());
        Path ffmpegPath = binDirectory.resolve(dependencyManager.getFmpegbinaryName());
        String outputTemplate = playlist ? PLAYLIST_OUTPUT_TEMPLATE : SINGLE_OUTPUT_TEMPLATE;
        Path outputTemplatePath = outputDirectory.resolve(outputTemplate);

        List<String> command = buildCommand(
                ytDlpPath,
                ffmpegPath.getParent(),
                outputTemplatePath,
                url,
                verbose
        );

        stopRequested = false;
        int exitCode = runCommand(command, safeOutputConsumer);
        if (exitCode != 0) {
            throw new IllegalStateException("yt-dlp failed with exit code " + exitCode);
        }

        Path csvPath = playlist ? logService.writePlaylistCsv(outputDirectory) : null;
        return new DownloadResult(exitCode, outputDirectory, csvPath, playlist);
    }

    public UrlInspection inspectUrl(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A playlist or video URL is required.");
        }

        dependencyManager.ensureDependenciesExist();

        Path binDirectory = Path.of(dependencyManager.getBinDirectoryPath());
        Path ytDlpPath = binDirectory.resolve(dependencyManager.getYtDlpBinaryName());

        List<String> command = new ArrayList<>();
        command.add(ytDlpPath.toString());
        command.add("--dump-single-json");
        command.add("--flat-playlist");
        command.add("--skip-download");
        command.add("--no-warnings");
        command.add(url.trim());

        stopRequested = false;
        StringBuilder output = new StringBuilder();
        int exitCode = runCommand(command, line -> output.append(line).append('\n'));
        if (exitCode != 0) {
            throw new IllegalStateException("yt-dlp could not inspect URL. Exit code: " + exitCode);
        }

        String json = output.toString();
        boolean playlist = json.contains("\"_type\": \"playlist\"")
                || json.contains("\"_type\":\"playlist\"")
                || json.contains("\"entries\": [")
                || json.contains("\"entries\":[");

        return new UrlInspection(url.trim(), playlist, findTitle(json), findThumbnailUrl(json));
    }

    String findTitle(String json) {
        Matcher matcher = TITLE_PATTERN.matcher(json);
        if (!matcher.find()) {
            return "Untitled";
        }

        String title = unescapeJsonString(matcher.group(1)).trim();
        return title.isBlank() ? "Untitled" : title;
    }

    String findThumbnailUrl(String json) {
        Matcher matcher = THUMBNAIL_PATTERN.matcher(json);
        if (matcher.find()) {
            return unescapeJsonString(matcher.group(1));
        }

        matcher = THUMBNAILS_ARRAY_PATTERN.matcher(json);
        if (!matcher.find()) {
            return "";
        }

        matcher = THUMBNAILS_URL_PATTERN.matcher(matcher.group(1));
        String thumbnailUrl = "";
        while (matcher.find()) {
            thumbnailUrl = matcher.group(1);
        }

        return thumbnailUrl.isBlank() ? "" : unescapeJsonString(thumbnailUrl);
    }

    String toSafeFolderName(String title) {
        String safeTitle = title == null ? "" : title.trim();
        if (safeTitle.isBlank()) {
            safeTitle = "playlist";
        }

        safeTitle = toReadableFileName(safeTitle)
                .replaceAll("^[._-]+|[._-]+$", "");

        if (safeTitle.isBlank()) {
            return "playlist";
        }

        safeTitle = limitCodePoints(safeTitle, SAFE_FOLDER_NAME_LIMIT)
                .replaceAll("[^\\p{L}\\p{N}]+$", "");
        return safeTitle.isBlank() ? "playlist" : safeTitle;
    }

    private String toReadableFileName(String value) {
        StringBuilder builder = new StringBuilder();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (Character.isLetterOrDigit(codePoint)
                    || codePoint == '.'
                    || codePoint == '_'
                    || codePoint == '-') {
                builder.appendCodePoint(codePoint);
            } else {
                builder.append('_');
            }
            offset += Character.charCount(codePoint);
        }

        return builder.toString().replaceAll("_+", "_");
    }

    private String limitCodePoints(String value, int limit) {
        if (value.codePointCount(0, value.length()) <= limit) {
            return value;
        }

        int endIndex = value.offsetByCodePoints(0, limit);
        return value.substring(0, endIndex);
    }

    String unescapeJsonString(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current != '\\' || i + 1 >= value.length()) {
                builder.append(current);
                continue;
            }

            char escaped = value.charAt(++i);
            switch (escaped) {
                case '"' -> builder.append('"');
                case '\\' -> builder.append('\\');
                case '/' -> builder.append('/');
                case 'b' -> builder.append('\b');
                case 'f' -> builder.append('\f');
                case 'n' -> builder.append('\n');
                case 'r' -> builder.append('\r');
                case 't' -> builder.append('\t');
                case 'u' -> {
                    if (i + 4 < value.length()) {
                        String hex = value.substring(i + 1, i + 5);
                        try {
                            builder.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            builder.append("\\u");
                        }
                    } else {
                        builder.append("\\u");
                    }
                }
                default -> builder.append(escaped);
            }
        }
        return builder.toString();
    }

    List<String> buildCommand(
            Path ytDlpPath,
            Path ffmpegDirectory,
            Path outputTemplatePath,
            String url,
            boolean verbose
    ) {
        List<String> command = new ArrayList<>();
        command.add(ytDlpPath.toString());
        command.add("--format");
        command.add("bestaudio/best");
        command.add("--extract-audio");
        command.add("--audio-format");
        command.add("mp3");
        command.add("--audio-quality");
        command.add("320K");
        command.add("--write-thumbnail");
        command.add("--embed-thumbnail");
        command.add("--add-metadata");
        command.add("--ffmpeg-location");
        command.add(ffmpegDirectory.toString());
        command.add("--output");
        command.add(outputTemplatePath.toString());

        if (verbose) {
            command.add("--verbose");
        } else {
            command.add("--no-warnings");
        }

        command.add(url);
        return command;
    }

    protected int runCommand(List<String> command, Consumer<String> outputConsumer)
            throws IOException, InterruptedException, DownloadStoppedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        currentProcess = process;
        try {
            try (InputStream processOutput = process.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(processOutput, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputConsumer.accept(line);
                }
            }

            int exitCode = process.waitFor();
            if (stopRequested) {
                throw new DownloadStoppedException("Download stopped by user.");
            }
            return exitCode;
        } finally {
            if (currentProcess == process) {
                currentProcess = null;
            }
        }
    }

    public void forceStop() {
        stopRequested = true;
        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            destroyProcessTree(process);
        }
    }

    private void destroyProcessTree(Process process) {
        ProcessHandle handle = process.toHandle();
        List<ProcessHandle> descendants = handle.descendants()
                .sorted(Comparator.comparingLong(ProcessHandle::pid).reversed())
                .toList();

        for (ProcessHandle descendant : descendants) {
            destroyForcibly(descendant);
        }

        destroyForcibly(handle);
        process.destroyForcibly();
    }

    private void destroyForcibly(ProcessHandle handle) {
        if (handle.isAlive()) {
            handle.destroyForcibly();
        }
    }

    public record UrlInspection(String url, boolean playlist, String title, String thumbnailUrl) {
    }

    public record DownloadResult(int exitCode, Path outputDirectory, Path csvPath, boolean playlist) {
    }

    public static class DownloadStoppedException extends Exception {
        public DownloadStoppedException(String message) {
            super(message);
        }
    }
}
