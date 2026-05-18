package hhlhh.scene;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ConsentFormScene {

    private static final double WIDTH = 560;
    private static final double HEIGHT = 430;
    private static final String APP_DIR_NAME = ".yt_dlp_service";
    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), APP_DIR_NAME);
    private static final Path CONSENT_COOKIE = APP_DIR.resolve("consent.properties");

    public static boolean hasConsentCookie() {
        return Files.isRegularFile(CONSENT_COOKIE);
    }

    public Scene create(Stage stage, Runnable onAccepted) {
        Label title = new Label("yt-dlp Service Consent");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label intro = paragraph("""
                This small personal-use JavaFX application downloads media with yt-dlp.
                Before the app installs yt-dlp and FFmpeg, confirm that you understand how it works.
                """);

        VBox notice = new VBox(
                10,
                heading("Notice"),
                paragraph("""
                        This project is provided as-is, with no warranty, support, or guarantee that it will work correctly on your machine.
                        """),
                paragraph("""
                        Use it at your own risk. You are responsible for how you use this software and for complying with applicable laws, platform terms, copyright rules, and network policies.
                        """),
                heading("Local files"),
                paragraph("""
                        On first run, the app downloads and stores yt-dlp and FFmpeg in %USERPROFILE%\\.yt_dlp_service\\.
                        """),
                paragraph("""
                        This consent choice is also stored in that folder so the app does not ask again before starting.
                        """)
        );
        notice.setPadding(new Insets(4, 8, 4, 0));

        ScrollPane scrollPane = new ScrollPane(notice);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(220);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        CheckBox confirmCheckBox = new CheckBox("I understand and agree to continue.");
        Button declineButton = new Button("Decline");
        Button acceptButton = new Button("Accept and continue");
        acceptButton.setDisable(true);
        confirmCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> acceptButton.setDisable(!selected));

        declineButton.setOnAction(event -> stage.close());
        acceptButton.setOnAction(event -> {
            try {
                writeConsentCookie();
                onAccepted.run();
            } catch (IOException e) {
                confirmCheckBox.setText("Unable to save consent: " + e.getMessage());
                acceptButton.setDisable(true);
            }
        });

        HBox buttons = new HBox(10, declineButton, acceptButton);
        buttons.setStyle("-fx-alignment: center-right;");

        VBox root = new VBox(16, title, intro, scrollPane, confirmCheckBox, buttons);
        root.setPadding(new Insets(20));

        return new Scene(root, WIDTH, HEIGHT);
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private static Label paragraph(String text) {
        Label label = new Label(text.strip());
        label.setWrapText(true);
        return label;
    }

    private static void writeConsentCookie() throws IOException {
        Files.createDirectories(APP_DIR);

        Properties properties = new Properties();
        properties.setProperty("accepted", "true");
        properties.setProperty("acceptedAt", Instant.now().toString());
        properties.setProperty("appDataDirectory", APP_DIR.toAbsolutePath().toString());

        try (OutputStream outputStream = Files.newOutputStream(CONSENT_COOKIE)) {
            properties.store(outputStream, "yt-dlp Service consent");
        }
    }
}
