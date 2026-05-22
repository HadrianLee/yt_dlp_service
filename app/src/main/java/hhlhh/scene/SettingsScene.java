package hhlhh.scene;

import java.io.IOException;

import hhlhh.model.SettingsService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
    private Label traySupportLabel;

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

        boolean traySupported = settingsService.isTraySupported();
        closeToTrayToggle.setDisable(!traySupported);
        traySupportLabel.setText(traySupported
                ? "When a download is active, closing the window hides the app to the tray."
                : "System tray is not available on this device.");
    }

    private Parent createFallback() {
        CheckBox darkMode = new CheckBox("Dark mode");
        darkMode.selectedProperty().bindBidirectional(settingsService.darkModeProperty());
        CheckBox closeToTray = new CheckBox("Close to tray while downloading");
        closeToTray.selectedProperty().bindBidirectional(settingsService.closeToTrayDuringDownloadProperty());
        closeToTray.setDisable(!settingsService.isTraySupported());
        CheckBox notifications = new CheckBox("Notify when complete");
        notifications.selectedProperty().bindBidirectional(settingsService.notificationsEnabledProperty());
        VBox fallback = new VBox(12, darkMode, closeToTray, notifications);
        fallback.getStyleClass().addAll("content-panel", "settings-panel");
        return fallback;
    }
}
