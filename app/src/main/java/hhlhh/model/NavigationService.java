package hhlhh.model;

import java.util.function.BooleanSupplier;

import hhlhh.scene.NavigationScene;
import hhlhh.scene.SettingsScene;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class NavigationService {

    private static final String EXIT_DIALOG_TITLE = "Download still running";
    private static final String EXIT_DIALOG_MESSAGE =
            "A download is still running. Exiting now will stop the app and may leave partial output files.";

    private final SettingsService settingsService;
    private BooleanSupplier downloadInProgress = () -> false;
    private BorderPane root;
    private Node downloaderContent;
    private Node settingsContent;
    private Node navigationDrawer;

    public NavigationService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public BorderPane createDownloaderShell(Stage stage, Node content) {
        downloaderContent = content;
        settingsContent = new SettingsScene(settingsService).create();
        navigationDrawer = new NavigationScene(this).create();

        root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(createTopBar());
        root.setLeft(navigationDrawer);
        root.setCenter(downloaderContent);
        setNavigationVisible(false);

        settingsService.attachStage(stage);
        settingsService.setExitAction(() -> exitFromTray(stage));
        settingsService.setOpenDownloadAction(this::showDownloader);
        settingsService.setOpenSettingsAction(this::showSettings);
        installCloseHandler(stage);
        return root;
    }

    public void bindDownloadInProgress(BooleanSupplier downloadInProgress) {
        this.downloadInProgress = downloadInProgress != null ? downloadInProgress : () -> false;
    }

    public void showDownloader() {
        if (root != null && downloaderContent != null) {
            root.setCenter(downloaderContent);
            setNavigationVisible(false);
        }
    }

    public void showSettings() {
        if (root != null && settingsContent != null) {
            root.setCenter(settingsContent);
            setNavigationVisible(false);
        }
    }

    public void toggleNavigation() {
        if (navigationDrawer != null) {
            setNavigationVisible(!navigationDrawer.isVisible());
        }
    }

    public void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }

        updateThemeClass(scene, settingsService.isDarkMode());
        settingsService.darkModeProperty().addListener((observable, oldValue, dark) -> updateThemeClass(scene, dark));
    }

    private HBox createTopBar() {
        Button menuButton = new Button("\u2630");
        menuButton.getStyleClass().addAll("icon-button", "hamburger-button");
        menuButton.setOnAction(event -> toggleNavigation());

        HBox topBar = new HBox(menuButton);
        topBar.getStyleClass().add("top-bar");
        return topBar;
    }

    private void setNavigationVisible(boolean visible) {
        if (navigationDrawer == null) {
            return;
        }
        navigationDrawer.setVisible(visible);
        navigationDrawer.setManaged(visible);
    }

    private void installCloseHandler(Stage stage) {
        stage.setOnCloseRequest(event -> {
            boolean inProgress = downloadInProgress.getAsBoolean();
            if (inProgress && settingsService.shouldCloseToTrayDuringDownload()) {
                event.consume();
                hideToTray(stage, "Download still running", "The app is still downloading in the system tray.");
                return;
            } else if (inProgress) {
                ExitDialog exitDialog = new ExitDialog(EXIT_DIALOG_TITLE, EXIT_DIALOG_MESSAGE);
                if (!exitDialog.showAndWait(stage)) {
                    event.consume();
                    return;
                }
            }

            settingsService.removeTrayIcon();
            Platform.setImplicitExit(true);
        });
    }

    private void hideToTray(Stage stage, String title, String message) {
        settingsService.showTrayIcon(stage);
        Platform.setImplicitExit(false);
        stage.hide();
        settingsService.notifyTrayMessage(title, message);
    }

    private void exitFromTray(Stage stage) {
        if (downloadInProgress.getAsBoolean() && settingsService.shouldCloseToTrayDuringDownload()) {
            stage.show();
            stage.toFront();
            settingsService.notifyTrayMessage("Download still running", "Stop the download before exiting.");
            return;
        }

        settingsService.removeTrayIcon();
        Platform.setImplicitExit(true);
        Platform.exit();
    }

    private void updateThemeClass(Scene scene, boolean dark) {
        if (dark) {
            if (!scene.getRoot().getStyleClass().contains("dark")) {
                scene.getRoot().getStyleClass().add("dark");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark");
        }
    }
}
