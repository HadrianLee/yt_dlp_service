package hhlhh.model;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
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
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SettingsService {

    private static final String DARK_MODE = "darkMode";
    private static final String CONSUME_EXIT_DURING_DOWNLOAD = "consumeExitDuringDownload";
    private static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";

    private final Path settingsPath;
    private final BooleanProperty darkMode = new SimpleBooleanProperty(false);
    private final BooleanProperty closeToTrayDuringDownload = new SimpleBooleanProperty(true);
    private final BooleanProperty notificationsEnabled = new SimpleBooleanProperty(true);
    private final ReadOnlyBooleanWrapper traySupported = new ReadOnlyBooleanWrapper(SystemTray.isSupported());
    private final DependencyManager dependencyManager;

    private TrayIcon trayIcon;
    private Stage attachedStage;
    private Runnable exitAction = Platform::exit;

    public SettingsService() {
        this(AppPaths.appDirectory().resolve("settings.properties"), new DependencyManager());
    }

    SettingsService(Path settingsPath) {
        this(settingsPath, new DependencyManager());
    }

    SettingsService(Path settingsPath, DependencyManager dependencyManager) {
        this.settingsPath = settingsPath;
        this.dependencyManager = Objects.requireNonNull(dependencyManager);
        load();
        if (!isTraySupported()) {
            closeToTrayDuringDownload.set(false);
        }
        addPersistenceListener(darkMode);
        addPersistenceListener(closeToTrayDuringDownload);
        addPersistenceListener(notificationsEnabled);
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

    public ReadOnlyBooleanProperty traySupportedProperty() {
        return traySupported.getReadOnlyProperty();
    }

    public boolean isTraySupported() {
        return traySupported.get();
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

    public void attachStage(Stage stage) {
        attachedStage = stage;
    }

    public void setExitAction(Runnable exitAction) {
        this.exitAction = exitAction != null ? exitAction : Platform::exit;
    }

    public void showTrayIcon(Stage stage) {
        if (!isTraySupported()) {
            return;
        }
        attachedStage = stage != null ? stage : attachedStage;
        ensureTrayIcon();
    }

    public void notifyDownloadComplete(String title, String message) {
        if (!isNotificationsEnabled() || !isTraySupported()) {
            return;
        }

        ensureTrayIcon();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public void notifyTrayMessage(String title, String message) {
        if (!isTraySupported()) {
            return;
        }

        ensureTrayIcon();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public void removeTrayIcon() {
        if (trayIcon != null && isTraySupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        trayIcon = null;
    }

    private void ensureTrayIcon() {
        if (trayIcon != null || !isTraySupported()) {
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem openItem = new MenuItem("Open");
        openItem.addActionListener(event -> Platform.runLater(this::showAttachedStage));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(event -> {
            Runnable action = exitAction;
            Platform.runLater(action);
        });
        menu.add(openItem);
        menu.add(exitItem);

        TrayIcon icon = new TrayIcon(createTrayImage(), "yt-dlp Service", menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(event -> Platform.runLater(this::showAttachedStage));

        try {
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
        } catch (AWTException e) {
            trayIcon = null;
        }
    }

    private Image createTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(9, 105, 218));
            graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(4, 4, 8, 2);
            graphics.fillRect(7, 4, 2, 7);
            graphics.fillPolygon(new int[] { 4, 12, 8 }, new int[] { 9, 9, 13 }, 3);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void showAttachedStage() {
        if (attachedStage == null) {
            return;
        }
        attachedStage.show();
        attachedStage.toFront();
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
        } catch (IOException e) {
            // Keep defaults when settings cannot be read.
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty(DARK_MODE, Boolean.toString(darkMode.get()));
        properties.setProperty(CONSUME_EXIT_DURING_DOWNLOAD, Boolean.toString(closeToTrayDuringDownload.get()));
        properties.setProperty(NOTIFICATIONS_ENABLED, Boolean.toString(notificationsEnabled.get()));

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
