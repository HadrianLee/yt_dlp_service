package hhlhh.scene;

import java.io.IOException;

import hhlhh.model.ConsentFormService;
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

    private final ConsentFormService consentFormService;

    public ConsentFormScene(ConsentFormService consentFormService) {
        this.consentFormService = consentFormService;
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
                paragraph("On first run, the app downloads and stores yt-dlp and FFmpeg in "
                        + consentFormService.getAppDirectoryPath() + "."),
                paragraph("This consent choice is also stored in that folder so the app does not ask again before starting.")
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
                consentFormService.writeConsentCookie();
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

        return new Scene(root);
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
}
