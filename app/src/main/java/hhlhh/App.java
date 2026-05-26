package hhlhh;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import hhlhh.desktop.AppIconService;
import hhlhh.desktop.SystemTrayService;
import hhlhh.model.ConsentFormService;
import hhlhh.model.DependencyManager;
import hhlhh.model.SettingsService;
import hhlhh.model.SingleInstanceService;
import hhlhh.scene.ConsentFormScene;
import hhlhh.scene.DownloaderScene;
import hhlhh.ui.NavigationService;

public class App extends Application {

    private static final double WINDOW_WIDTH = 840;
    private static final double WINDOW_HEIGHT = 540;
    private static final String APP_STYLESHEET = "/hhlhh/style/app.css";
    private static final String DARK_CLASS = "dark";

    private final SettingsService settingsService = new SettingsService();
    private final SystemTrayService systemTrayService = new SystemTrayService();
    private final SingleInstanceService singleInstanceService = new SingleInstanceService();

    @Override
    public void start(Stage primaryStage) {
        if (!singleInstanceService.acquire()) {
            showAlreadyRunningMessage();
            Platform.exit();
            return;
        }

        primaryStage.setTitle("Downloader Shell");
        applyAppIcon(primaryStage);
        applyWindowSize(primaryStage);

        ConsentFormService consentFormService = new ConsentFormService();
        ConsentFormScene consentFormScene = new ConsentFormScene(consentFormService);
        if (!consentFormService.hasConsentCookie()) {
            setAppScene(primaryStage, consentFormScene.create(primaryStage, () -> showDependencyInitScene(primaryStage)));
            primaryStage.show();
            return;
        }

        showDependencyInitScene(primaryStage);
        primaryStage.show();
    }

    @Override
    public void stop() {
        singleInstanceService.close();
    }

    private void showDependencyInitScene(Stage primaryStage) {
        Label statusLabel = new Label("Verifying runtime dependencies...");
        ProgressIndicator progress = new ProgressIndicator();
        VBox root = new VBox(15, statusLabel, progress);
        root.setStyle("-fx-alignment: center; -fx-padding: 30;");

        // Execute initialization via worker threads
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                DependencyManager manager = new DependencyManager();
                manager.ensureDependenciesExist();
                return null;
            }
        };

        // UI Updates upon thread resolution states
        initTask.setOnSucceeded(e -> {
            statusLabel.setText("Ready! Launching Downloader App Interface...");
            NavigationService navigationService = new NavigationService(settingsService, systemTrayService);
            setAppScene(primaryStage, new DownloaderScene(settingsService, navigationService, systemTrayService)
                    .create(primaryStage));
        });

        initTask.setOnFailed(e -> {
            statusLabel.setText("System configuration failed: " + initTask.getException().getMessage());
            progress.setVisible(false);
        });

        setAppScene(primaryStage, new Scene(root));

        Thread thread = new Thread(initTask, "dependency-init");
        thread.setDaemon(true);
        thread.start();
    }

    private void setAppScene(Stage stage, Scene scene) {
        applyStyles(scene);
        stage.setScene(scene);
        applyWindowSize(stage);
    }

    private void applyWindowSize(Stage stage) {
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
    }

    private void applyAppIcon(Stage stage) {
        AppIconService.loadJavaFxIcon().ifPresent(stage.getIcons()::add);
    }

    private void applyStyles(Scene scene) {
        var stylesheet = App.class.getResource(APP_STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        setDarkModeClass(scene, settingsService.isDarkMode());
        settingsService.addPropertyChangeListener(
                SettingsService.DARK_MODE,
                event -> setDarkModeClass(scene, (boolean) event.getNewValue())
        );
    }

    private void setDarkModeClass(Scene scene, boolean darkMode) {
        setDarkModeClass(scene.getRoot(), darkMode);
    }

    private void setDarkModeClass(Parent root, boolean darkMode) {
        if (darkMode) {
            if (!root.getStyleClass().contains(DARK_CLASS)) {
                root.getStyleClass().add(DARK_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_CLASS);
        }
    }

    private void applyStyles(DialogPane dialogPane) {
        var stylesheet = App.class.getResource(APP_STYLESHEET);
        if (stylesheet != null) {
            dialogPane.getStylesheets().add(stylesheet.toExternalForm());
        }
        setDarkModeClass(dialogPane, settingsService.isDarkMode());
        settingsService.addPropertyChangeListener(
                SettingsService.DARK_MODE,
                event -> setDarkModeClass(dialogPane, (boolean) event.getNewValue())
        );
    }

    private void showAlreadyRunningMessage() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Downloader Shell");
        alert.setHeaderText("Downloader Shell is already running");
        alert.setContentText("Only one copy of this application can run at a time.");
        applyStyles(alert.getDialogPane());
        alert.showAndWait();
    }
}
