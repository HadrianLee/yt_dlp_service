package hhlhh.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SettingsService {

    private static final String DARK_MODE = "darkMode";
    private static final String CONSUME_EXIT_DURING_DOWNLOAD = "consumeExitDuringDownload";
    private static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";
    private static final String USE_POSTPROCESS_PIPELINE = "usePostprocessPipeline";

    private final Path settingsPath;
    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);
    private final BooleanProperty closeToTrayDuringDownload = new SimpleBooleanProperty(true);
    private final BooleanProperty notificationsEnabled = new SimpleBooleanProperty(true);
    private final BooleanProperty usePostprocessPipeline = new SimpleBooleanProperty(false);
    private final DependencyManager dependencyManager;
    private final SystemTrayService systemTrayService;

    public SettingsService() {
        this(AppPaths.appDirectory().resolve("settings.properties"), new DependencyManager());
    }

    SettingsService(Path settingsPath) {
        this(settingsPath, new DependencyManager());
    }

    SettingsService(Path settingsPath, DependencyManager dependencyManager) {
        this(settingsPath, dependencyManager, new SystemTrayService());
    }

    SettingsService(Path settingsPath, DependencyManager dependencyManager, SystemTrayService systemTrayService) {
        this.settingsPath = settingsPath;
        this.dependencyManager = Objects.requireNonNull(dependencyManager);
        this.systemTrayService = Objects.requireNonNull(systemTrayService);
        load();
        if (!isTraySupported()) {
            closeToTrayDuringDownload.set(false);
        }
        addPersistenceListener(darkMode);
        addPersistenceListener(closeToTrayDuringDownload);
        addPersistenceListener(notificationsEnabled);
        addPersistenceListener(usePostprocessPipeline);
        closeToTrayDuringDownload.addListener((observable, oldValue, enabled) -> {
            if (enabled && !isTraySupported()) {
                closeToTrayDuringDownload.set(false);
                return;
            }
            if (!enabled) {
                removeTrayIcon();
            }
            save();
        });
    }

    public BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public boolean isDarkMode() {
        return darkMode.get();
    }

    public BooleanProperty closeToTrayDuringDownloadProperty() {
        return closeToTrayDuringDownload;
    }

    public boolean shouldCloseToTrayDuringDownload() {
        return isTraySupported() && closeToTrayDuringDownload.get();
    }

    public BooleanProperty notificationsEnabledProperty() {
        return notificationsEnabled;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled.get();
    }

    public BooleanProperty usePostprocessPipelineProperty() {
        return usePostprocessPipeline;
    }

    public boolean shouldUsePostprocessPipeline() {
        return usePostprocessPipeline.get();
    }

    public ReadOnlyBooleanProperty traySupportedProperty() {
        return systemTrayService.traySupportedProperty();
    }

    public boolean isTraySupported() {
        return systemTrayService.isTraySupported();
    }

    public String getDependencyDirectoryPath() {
        return dependencyManager.getBinDirectoryPath();
    }

    public void repairDependencies() throws Exception {
        dependencyManager.repairDependencies();
    }

    public void bindDependencyRepairControls(Button button, Label statusLabel) {
        statusLabel.setText("Dependencies install to " + getDependencyDirectoryPath());
        button.setOnAction(event -> repairDependencies(button, statusLabel));
    }

    public void bindPostprocessPipelineToggle(CheckBox checkBox) {
        checkBox.selectedProperty().bindBidirectional(usePostprocessPipeline);
    }

    public void attachStage(Stage stage) {
        systemTrayService.attachStage(stage);
    }

    public void setExitAction(Runnable exitAction) {
        systemTrayService.setExitAction(exitAction != null ? exitAction : Platform::exit);
    }

    public void setOpenDownloadAction(Runnable openDownloadAction) {
        systemTrayService.setOpenDownloadAction(openDownloadAction);
    }

    public void setOpenSettingsAction(Runnable openSettingsAction) {
        systemTrayService.setOpenSettingsAction(openSettingsAction);
    }

    public void showTrayIcon(Stage stage) {
        systemTrayService.showTrayIcon(stage);
    }

    public void notifyDownloadComplete(String title, String message) {
        if (!isNotificationsEnabled()) {
            return;
        }

        systemTrayService.notifyTrayMessage(title, message);
    }

    public void notifyTrayMessage(String title, String message) {
        systemTrayService.notifyTrayMessage(title, message);
    }

    public void removeTrayIcon() {
        systemTrayService.removeTrayIcon();
    }

    private void addPersistenceListener(BooleanProperty property) {
        property.addListener((observable, oldValue, newValue) -> save());
    }

    private void repairDependencies(Button button, Label statusLabel) {
        Task<Void> repairTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                repairDependencies();
                return null;
            }
        };

        button.disableProperty().bind(repairTask.runningProperty());
        statusLabel.setText("Repairing dependencies...");

        repairTask.setOnSucceeded(event -> {
            button.disableProperty().unbind();
            button.setDisable(false);
            statusLabel.setText("Dependencies repaired.");
        });
        repairTask.setOnFailed(event -> {
            button.disableProperty().unbind();
            button.setDisable(false);
            Throwable exception = repairTask.getException();
            String message = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : "Unknown error";
            statusLabel.setText("Dependency repair failed: " + message);
        });

        Thread thread = new Thread(repairTask, "dependency-repair");
        thread.setDaemon(true);
        thread.start();
    }

    private void load() {
        if (!Files.isRegularFile(settingsPath)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsPath)) {
            properties.load(input);
            darkMode.set(Boolean.parseBoolean(properties.getProperty(DARK_MODE, Boolean.toString(darkMode.get()))));
            closeToTrayDuringDownload.set(Boolean.parseBoolean(properties.getProperty(
                    CONSUME_EXIT_DURING_DOWNLOAD,
                    Boolean.toString(closeToTrayDuringDownload.get())
            )));
            notificationsEnabled.set(Boolean.parseBoolean(properties.getProperty(
                    NOTIFICATIONS_ENABLED,
                    Boolean.toString(notificationsEnabled.get())
            )));
            usePostprocessPipeline.set(Boolean.parseBoolean(properties.getProperty(
                    USE_POSTPROCESS_PIPELINE,
                    Boolean.toString(usePostprocessPipeline.get())
            )));
        } catch (IOException e) {
            // Keep defaults when settings cannot be read.
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty(DARK_MODE, Boolean.toString(darkMode.get()));
        properties.setProperty(CONSUME_EXIT_DURING_DOWNLOAD, Boolean.toString(closeToTrayDuringDownload.get()));
        properties.setProperty(NOTIFICATIONS_ENABLED, Boolean.toString(notificationsEnabled.get()));
        properties.setProperty(USE_POSTPROCESS_PIPELINE, Boolean.toString(usePostprocessPipeline.get()));

        try {
            Files.createDirectories(settingsPath.getParent());
            try (OutputStream output = Files.newOutputStream(settingsPath)) {
                properties.store(output, "yt-dlp service settings");
            }
        } catch (IOException e) {
            // Settings changes should not block normal app use.
        }
    }
}
