package hhlhh.scene;

import java.io.IOException;

import hhlhh.model.ExitDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ExitDialogScene {

    private static final String EXIT_DIALOG_FXML = "/hhlhh/scene/exit_dialog.fxml";

    private final ExitDialog exitDialog;
    private final Stage ownerStage;
    private Stage dialogStage;

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private Button exitButton;

    public ExitDialogScene(ExitDialog exitDialog, Stage ownerStage) {
        this.exitDialog = exitDialog;
        this.ownerStage = ownerStage;
    }

    public javafx.scene.Scene create(Stage dialogStage) {
        this.dialogStage = dialogStage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource(EXIT_DIALOG_FXML));
        loader.setControllerFactory(type -> this);
        try {
            return createScene(loader.load());
        } catch (IOException e) {
            return createScene(createFallback());
        }
    }

    @FXML
    private void initialize() {
        titleLabel.setText(exitDialog.title());
        messageLabel.setText(exitDialog.message());
        cancelButton.setOnAction(event -> exitDialog.cancelExit(dialogStage));
        exitButton.setOnAction(event -> exitDialog.confirmExit(dialogStage));
    }

    private Parent createFallback() {
        Label title = new Label(exitDialog.title());
        title.getStyleClass().add("page-title");

        Label message = new Label(exitDialog.message());
        message.setWrapText(true);

        Button cancelButton = new Button("Keep downloading");
        cancelButton.setOnAction(event -> exitDialog.cancelExit(dialogStage));

        Button exitButton = new Button("Exit");
        exitButton.getStyleClass().add("danger-action");
        exitButton.setOnAction(event -> exitDialog.confirmExit(dialogStage));

        HBox buttons = new HBox(10, cancelButton, exitButton);
        buttons.setStyle("-fx-alignment: center-right;");

        VBox panel = new VBox(16, title, message, buttons);
        panel.getStyleClass().addAll("content-panel", "dialog-panel");
        panel.setPrefWidth(420);

        StackPane root = new StackPane(panel);
        root.setPadding(new Insets(16));
        return root;
    }

    private javafx.scene.Scene createScene(Parent root) {
        return PopUpWindowScene.create(root, ownerStage);
    }
}
