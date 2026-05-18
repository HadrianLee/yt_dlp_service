package hhlhh;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import hhlhh.model.DependencyManager;
import hhlhh.scene.DownloaderScene;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
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

        new Thread(initTask).start();

        primaryStage.setScene(new Scene(root, 400, 200));
        primaryStage.setTitle("Downloader Shell");
        primaryStage.show();
    }
}
