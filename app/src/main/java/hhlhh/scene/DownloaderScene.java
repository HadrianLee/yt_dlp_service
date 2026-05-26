package hhlhh.scene;

import hhlhh.desktop.SystemTrayService;
import hhlhh.model.SettingsService;
import hhlhh.ui.Downloader;
import hhlhh.ui.NavigationService;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DownloaderScene {

    private final SettingsService settingsService;
    private final NavigationService navigationService;
    private final SystemTrayService systemTrayService;

    public DownloaderScene() {
        this(new SettingsService(), null, null);
    }

    public DownloaderScene(SettingsService settingsService, NavigationService navigationService) {
        this(settingsService, navigationService, null);
    }

    public DownloaderScene(
            SettingsService settingsService,
            NavigationService navigationService,
            SystemTrayService systemTrayService
    ) {
        this.settingsService = settingsService;
        this.navigationService = navigationService;
        this.systemTrayService = systemTrayService;
    }

    public Scene create(Stage stage) {
        TextField urlField = new TextField();
        urlField.setPromptText("Paste YouTube video or playlist URL");
        Button enterButton = new Button("Enter");

        TextField pathField = new TextField();
        Button browseButton = new Button("Browse");

        Button downloadButton = new Button("Download");
        Button stopButton = new Button("Force Stop");
        CheckBox verboseCheckBox = new CheckBox("Verbose");
        Label statusLabel = new Label("Enter a URL to begin.");
        Label previewTitleLabel = new Label();
        ProgressIndicator progressIndicator = new ProgressIndicator();
        TextArea outputArea = new TextArea();
        ImageView thumbnailView = new ImageView();

        progressIndicator.setMaxSize(24, 24);
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        thumbnailView.setFitWidth(240);
        thumbnailView.setFitHeight(135);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);

        HBox urlRow = new HBox(8, urlField, enterButton);
        HBox.setHgrow(urlField, Priority.ALWAYS);
        VBox urlGroup = new VBox(4, new Label("URL"), urlRow);
        urlGroup.getStyleClass().add("field-group");

        HBox pathRow = new HBox(8, pathField, browseButton);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        VBox pathGroup = new VBox(4, new Label("Download folder"), pathRow);
        pathGroup.getStyleClass().add("field-group");

        HBox actionRow = new HBox(10, downloadButton, stopButton, verboseCheckBox, progressIndicator);
        actionRow.getStyleClass().add("action-row");

        VBox preview = new VBox(8, previewTitleLabel, thumbnailView);
        preview.getStyleClass().add("preview-panel");

        VBox form = new VBox(
                10,
                urlGroup,
                pathGroup,
                actionRow,
                statusLabel,
                preview,
                outputArea
        );
        form.setPadding(new Insets(16));
        form.getStyleClass().add("content-panel");

        Downloader downloader = new Downloader(
                stage,
                urlField,
                pathField,
                enterButton,
                browseButton,
                downloadButton,
                stopButton,
                verboseCheckBox,
                statusLabel,
                previewTitleLabel,
                preview,
                outputArea,
                progressIndicator,
                thumbnailView,
                settingsService,
                systemTrayService
        );
        downloader.initialize();

        BorderPane shell = navigationService != null
                ? navigationService.createDownloaderShell(stage, form)
                : new BorderPane(form);

        if (navigationService != null) {
            navigationService.bindDownloadInProgress(() -> downloader.downloadInProgressProperty().get());
        }

        return new Scene(shell);
    }
}
