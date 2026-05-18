package hhlhh;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import hhlhh.model.DependencyManager;
import hhlhh.scene.ConsentFormScene;
import hhlhh.scene.DownloaderScene;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Downloader Shell");

        ConsentFormScene consentFormScene = new ConsentFormScene();
        if (!ConsentFormScene.hasConsentCookie()) {
            primaryStage.setScene(consentFormScene.create(primaryStage, () -> showDependencyInitScene(primaryStage)));
            primaryStage.show();
            return;
        }

        showDependencyInitScene(primaryStage);
        primaryStage.show();
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
            primaryStage.setScene(new DownloaderScene().create(primaryStage));
        });

        initTask.setOnFailed(e -> {
            statusLabel.setText("System configuration failed: " + initTask.getException().getMessage());
            progress.setVisible(false);
        });

        primaryStage.setScene(new Scene(root, 400, 200));

        Thread thread = new Thread(initTask, "dependency-init");
        thread.setDaemon(true);
        thread.start();
    }
}
