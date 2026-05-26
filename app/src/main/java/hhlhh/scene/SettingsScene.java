package hhlhh.scene;

import java.io.IOException;
import java.util.function.Consumer;

import hhlhh.desktop.SystemTrayService;
import hhlhh.model.SettingsService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsScene {

    private final SettingsService settingsService;
    private final SystemTrayService systemTrayService;

    @FXML
    private CheckBox darkModeToggle;

    @FXML
    private CheckBox closeToTrayToggle;

    @FXML
    private CheckBox notificationToggle;

    @FXML
    private CheckBox postprocessPipelineToggle;

    @FXML
    private Label traySupportLabel;

    @FXML
    private Button repairDependenciesButton;

    @FXML
    private Label dependencyRepairStatusLabel;

    public SettingsScene(SettingsService settingsService) {
        this(settingsService, null);
    }

    public SettingsScene(SettingsService settingsService, SystemTrayService systemTrayService) {
        this.settingsService = settingsService;
        this.systemTrayService = systemTrayService;
    }

    public Parent create() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/hhlhh/scene/settings.fxml"));
        loader.setControllerFactory(type -> this);
        try {
            return loader.load();
        } catch (IOException e) {
            return createFallback();
        }
    }

    @FXML
    private void initialize() {
        bindCheckBox(darkModeToggle, settingsService.isDarkMode(), settingsService::setDarkMode);
        bindCheckBox(
                closeToTrayToggle,
                settingsService.shouldCloseToTrayDuringDownload(),
                settingsService::setCloseToTrayDuringDownload
        );
        bindCheckBox(notificationToggle, settingsService.isNotificationsEnabled(), settingsService::setNotificationsEnabled);
        bindCheckBox(
                postprocessPipelineToggle,
                settingsService.shouldUsePostprocessPipeline(),
                settingsService::setUsePostprocessPipeline
        );

        boolean traySupported = isTraySupported();
        closeToTrayToggle.setDisable(!traySupported);
        traySupportLabel.setText(traySupported
                ? "When a download is active, closing the window hides the app to the tray."
                : "System tray is not available on this device.");
        bindDependencyRepairControls(repairDependenciesButton, dependencyRepairStatusLabel);
    }

    private Parent createFallback() {
        CheckBox darkMode = new CheckBox("Dark mode");
        bindCheckBox(darkMode, settingsService.isDarkMode(), settingsService::setDarkMode);
        CheckBox closeToTray = new CheckBox("Close to tray while downloading");
        bindCheckBox(
                closeToTray,
                settingsService.shouldCloseToTrayDuringDownload(),
                settingsService::setCloseToTrayDuringDownload
        );
        closeToTray.setDisable(!isTraySupported());
        CheckBox notifications = new CheckBox("Notify when complete");
        bindCheckBox(notifications, settingsService.isNotificationsEnabled(), settingsService::setNotificationsEnabled);
        CheckBox postprocessPipeline = new CheckBox("Use custom postprocess pipeline");
        bindCheckBox(
                postprocessPipeline,
                settingsService.shouldUsePostprocessPipeline(),
                settingsService::setUsePostprocessPipeline
        );
        Button repairDependencies = new Button("Repair dependencies");
        repairDependencies.getStyleClass().add("danger-action");
        Label repairStatus = new Label();
        repairStatus.getStyleClass().add("dependency-status");
        repairStatus.setWrapText(true);
        bindDependencyRepairControls(repairDependencies, repairStatus);

        VBox fallback = new VBox(
                12,
                darkMode,
                closeToTray,
                notifications,
                postprocessPipeline,
                repairDependencies,
                repairStatus
        );
        fallback.getStyleClass().addAll("content-panel", "settings-panel");
        return fallback;
    }

    private boolean isTraySupported() {
        return systemTrayService != null && systemTrayService.isTraySupported();
    }

    private void bindCheckBox(CheckBox checkBox, boolean selected, Consumer<Boolean> setter) {
        checkBox.setSelected(selected);
        checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> setter.accept(newValue));
    }

    private void bindDependencyRepairControls(Button button, Label statusLabel) {
        statusLabel.setText("Dependencies install to " + settingsService.getDependencyDirectoryPath());
        button.setOnAction(event -> repairDependencies(button, statusLabel));
    }

    private void repairDependencies(Button button, Label statusLabel) {
        Task<Void> repairTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                settingsService.repairDependencies();
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
}
