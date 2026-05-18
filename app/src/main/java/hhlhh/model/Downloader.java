package hhlhh.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Downloader {

    private final Stage owner;
    private final DownloadService downloadService;
    private final Path defaultDownloadPath;

    private final TextField urlField;
    private final TextField pathField;
    private final Button enterButton;
    private final Button browseButton;
    private final Button downloadButton;
    private final Button stopButton;
    private final CheckBox verboseCheckBox;
    private final Label statusLabel;
    private final Label previewTitleLabel;
    private final VBox previewPane;
    private final TextArea outputArea;
    private final ProgressIndicator progressIndicator;
    private final ImageView thumbnailView;

    private String inspectedUrl;
    private DownloadService.UrlInspection urlInspection;

    public Downloader(
            Stage owner,
            TextField urlField,
            TextField pathField,
            Button enterButton,
            Button browseButton,
            Button downloadButton,
            Button stopButton,
            CheckBox verboseCheckBox,
            Label statusLabel,
            Label previewTitleLabel,
            VBox previewPane,
            TextArea outputArea,
            ProgressIndicator progressIndicator,
            ImageView thumbnailView
    ) {
        this.owner = owner;
        this.downloadService = new DownloadService();
        this.defaultDownloadPath = resolveDefaultDownloadPath();
        this.urlField = urlField;
        this.pathField = pathField;
        this.enterButton = enterButton;
        this.browseButton = browseButton;
        this.downloadButton = downloadButton;
        this.stopButton = stopButton;
        this.verboseCheckBox = verboseCheckBox;
        this.statusLabel = statusLabel;
        this.previewTitleLabel = previewTitleLabel;
        this.previewPane = previewPane;
        this.outputArea = outputArea;
        this.progressIndicator = progressIndicator;
        this.thumbnailView = thumbnailView;
    }

    public void initialize() {
        pathField.setText(defaultDownloadPath.toString());
        progressIndicator.setVisible(false);
        downloadButton.setDisable(true);
        stopButton.setDisable(true);
        setVisibleAndManaged(previewPane, false);
        setVisibleAndManaged(outputArea, false);

        urlField.textProperty().addListener((observable, oldValue, newValue) -> {
            inspectedUrl = null;
            urlInspection = null;
            thumbnailView.setImage(null);
            previewTitleLabel.setText("");
            setVisibleAndManaged(previewPane, false);
            updateDownloadButtonState();
        });
        pathField.textProperty().addListener((observable, oldValue, newValue) -> updateDownloadButtonState());
        urlField.setOnAction(event -> inspectUrl());
        enterButton.setOnAction(event -> inspectUrl());
        browseButton.setOnAction(event -> chooseDownloadPath());
        downloadButton.setOnAction(event -> startDownload());
        stopButton.setOnAction(event -> forceStopDownload());

        updateDownloadButtonState();
    }

    private Path resolveDefaultDownloadPath() {
        return Path.of(System.getProperty("user.dir")).resolve("download").toAbsolutePath().normalize();
    }

    private void chooseDownloadPath() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select download folder");

        Path selectedPath = Path.of(pathField.getText()).toAbsolutePath().normalize();
        Path initialDirectory = Files.isDirectory(selectedPath) ? selectedPath : defaultDownloadPath.getParent();
        if (initialDirectory != null && Files.isDirectory(initialDirectory)) {
            directoryChooser.setInitialDirectory(initialDirectory.toFile());
        }

        var selectedDirectory = directoryChooser.showDialog(owner);
        if (selectedDirectory != null) {
            pathField.setText(selectedDirectory.toPath().toAbsolutePath().normalize().toString());
            statusLabel.setText("Download folder selected.");
        }
    }

    private void inspectUrl() {
        String url = urlField.getText() == null ? "" : urlField.getText().trim();
        if (!isValidYoutubeUrl(url)) {
            inspectedUrl = null;
            urlInspection = null;
            statusLabel.setText("Enter a valid YouTube URL.");
            updateDownloadButtonState();
            return;
        }

        statusLabel.setText("Checking URL...");
        progressIndicator.setVisible(true);
        enterButton.setDisable(true);
        downloadButton.setDisable(true);

        Task<DownloadService.UrlInspection> inspectTask = new Task<>() {
            @Override
            protected DownloadService.UrlInspection call() throws Exception {
                return downloadService.inspectUrl(url);
            }
        };

        inspectTask.setOnSucceeded(event -> {
            DownloadService.UrlInspection result = inspectTask.getValue();
            if (!url.equals(urlField.getText().trim())) {
                progressIndicator.setVisible(false);
                enterButton.setDisable(false);
                updateDownloadButtonState();
                return;
            }

            inspectedUrl = result.url();
            urlInspection = result;
            progressIndicator.setVisible(false);
            enterButton.setDisable(false);
            previewTitleLabel.setText(result.title());
            setVisibleAndManaged(previewPane, true);
            showThumbnail(result.thumbnailUrl());
            String displayTitle = toDisplayName(result.title());
            statusLabel.setText(result.playlist()
                    ? "Playlist detected: " + displayTitle + "/"
                    : "Video detected: " + displayTitle + ".mp3");
            updateDownloadButtonState();
        });

        inspectTask.setOnFailed(event -> {
            inspectedUrl = null;
            urlInspection = null;
            progressIndicator.setVisible(false);
            enterButton.setDisable(false);
            thumbnailView.setImage(null);
            previewTitleLabel.setText("");
            setVisibleAndManaged(previewPane, false);
            Throwable error = inspectTask.getException();
            statusLabel.setText("URL check failed: " + error.getMessage());
            updateDownloadButtonState();
        });

        Thread thread = new Thread(inspectTask, "yt-dlp-url-inspect");
        thread.setDaemon(true);
        thread.start();
    }

    private void validateUrl() {
        if (isValidYoutubeUrl(urlField.getText())) {
            statusLabel.setText("Press Enter to check whether this is a playlist.");
        } else {
            statusLabel.setText("Enter a valid YouTube URL.");
        }
        updateDownloadButtonState();
    }

    private void updateDownloadButtonState() {
        String currentUrl = urlField.getText() == null ? "" : urlField.getText().trim();
        boolean inspectedCurrentUrl = urlInspection != null && currentUrl.equals(inspectedUrl);
        downloadButton.setDisable(!inspectedCurrentUrl || pathField.getText().isBlank());
        enterButton.setDisable(!isValidYoutubeUrl(currentUrl));
    }

    private void showThumbnail(String thumbnailUrl) {
        if (thumbnailUrl == null || thumbnailUrl.isBlank()) {
            thumbnailView.setImage(null);
            return;
        }

        thumbnailView.setImage(new Image(thumbnailUrl, true));
    }

    private void setVisibleAndManaged(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private boolean isValidYoutubeUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                return false;
            }

            String normalizedHost = host.toLowerCase();
            return normalizedHost.equals("youtu.be")
                    || normalizedHost.equals("youtube.com")
                    || normalizedHost.endsWith(".youtube.com");
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void startDownload() {
        validateUrl();
        if (downloadButton.isDisable()) {
            return;
        }

        Path outputPath = Path.of(pathField.getText()).toAbsolutePath().normalize();
        String url = urlField.getText().trim();
        boolean verbose = verboseCheckBox.isSelected();
        boolean playlist = urlInspection != null && urlInspection.playlist();
        long filesBeforeDownload = countFiles(outputPath);

        setControlsDisabled(true);
        stopButton.setDisable(false);
        progressIndicator.setVisible(true);
        setVisibleAndManaged(outputArea, true);
        outputArea.clear();
        statusLabel.setText(playlist ? "Downloading playlist..." : "Downloading video...");

        Task<DownloadService.DownloadResult> downloadTask = new Task<>() {
            @Override
            protected DownloadService.DownloadResult call() throws Exception {
                return downloadService.downloadPlaylist(url, outputPath, verbose, urlInspection, line -> Platform.runLater(() -> appendOutput(line)));
            }
        };

        downloadTask.setOnSucceeded(event -> {
            DownloadService.DownloadResult result = downloadTask.getValue();
            if (result.playlist()) {
                statusLabel.setText("Playlist download finished in " + result.outputDirectory() + ". CSV saved at " + result.csvPath());
            } else {
                statusLabel.setText("Video download finished in " + result.outputDirectory() + ".");
            }
            progressIndicator.setVisible(false);
            stopButton.setDisable(true);
            setControlsDisabled(false);
            updateDownloadButtonState();
        });

        downloadTask.setOnFailed(event -> {
            Throwable error = downloadTask.getException();
            if (error instanceof DownloadService.DownloadStoppedException) {
                long createdFiles = Math.max(0, countFiles(outputPath) - filesBeforeDownload);
                String message = "Download stopped. " + createdFiles
                        + " new file(s) were created. They can be safely deleted if you do not want to keep partial output.";
                statusLabel.setText(message);
            } else {
                statusLabel.setText("Download failed: " + error.getMessage());
                appendOutput("Download failed: " + error.getMessage());
            }
            progressIndicator.setVisible(false);
            stopButton.setDisable(true);
            setControlsDisabled(false);
            updateDownloadButtonState();
        });

        Thread thread = new Thread(downloadTask, "yt-dlp-download");
        thread.setDaemon(true);
        thread.start();
    }

    private void forceStopDownload() {
        statusLabel.setText("Stopping download...");
        appendOutput("Stopping download...");
        stopButton.setDisable(true);
        downloadService.forceStop();
    }

    private void setControlsDisabled(boolean disabled) {
        urlField.setDisable(disabled);
        pathField.setDisable(disabled);
        enterButton.setDisable(disabled);
        browseButton.setDisable(disabled);
        verboseCheckBox.setDisable(disabled);
        downloadButton.setDisable(disabled);
    }

    private void appendOutput(String line) {
        outputArea.appendText(line + System.lineSeparator());
    }

    private String toDisplayName(String title) {
        String safeTitle = title == null ? "" : title.trim();
        if (safeTitle.isBlank()) {
            safeTitle = "Untitled";
        }

        safeTitle = safeTitle.replaceAll("[^A-Za-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._-]+|[._-]+$", "");

        if (safeTitle.isBlank()) {
            safeTitle = "Untitled";
        }

        safeTitle = safeTitle.length() > 30 ? safeTitle.substring(0, 30) : safeTitle;
        safeTitle = safeTitle.replaceAll("[^A-Za-z0-9]+$", "");

        return safeTitle.isBlank() ? "Untitled" : safeTitle;
    }

    private long countFiles(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return 0;
        }

        try (var paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            return 0;
        }
    }
}
