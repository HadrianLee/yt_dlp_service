package hhlhh.scene;

import java.io.IOException;

import hhlhh.model.SettingsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SettingsScene {

    private final SettingsService settingsService;

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
        this.settingsService = settingsService;
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
        darkModeToggle.selectedProperty().bindBidirectional(settingsService.darkModeProperty());
        closeToTrayToggle.selectedProperty().bindBidirectional(settingsService.closeToTrayDuringDownloadProperty());
        notificationToggle.selectedProperty().bindBidirectional(settingsService.notificationsEnabledProperty());
        settingsService.bindPostprocessPipelineToggle(postprocessPipelineToggle);

        boolean traySupported = settingsService.isTraySupported();
        closeToTrayToggle.setDisable(!traySupported);
        traySupportLabel.setText(traySupported
                ? "When a download is active, closing the window hides the app to the tray."
                : "System tray is not available on this device.");
        settingsService.bindDependencyRepairControls(repairDependenciesButton, dependencyRepairStatusLabel);
    }

    private Parent createFallback() {
        CheckBox darkMode = new CheckBox("Dark mode");
        darkMode.selectedProperty().bindBidirectional(settingsService.darkModeProperty());
        CheckBox closeToTray = new CheckBox("Close to tray while downloading");
        closeToTray.selectedProperty().bindBidirectional(settingsService.closeToTrayDuringDownloadProperty());
        closeToTray.setDisable(!settingsService.isTraySupported());
        CheckBox notifications = new CheckBox("Notify when complete");
        notifications.selectedProperty().bindBidirectional(settingsService.notificationsEnabledProperty());
        CheckBox postprocessPipeline = new CheckBox("Use custom postprocess pipeline");
        settingsService.bindPostprocessPipelineToggle(postprocessPipeline);
        Button repairDependencies = new Button("Repair dependencies");
        repairDependencies.getStyleClass().add("repair-button");
        Label repairStatus = new Label();
        repairStatus.getStyleClass().add("dependency-status");
        repairStatus.setWrapText(true);
        settingsService.bindDependencyRepairControls(repairDependencies, repairStatus);

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
}
