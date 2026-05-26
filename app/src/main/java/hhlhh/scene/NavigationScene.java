package hhlhh.scene;

import java.io.IOException;

import hhlhh.ui.NavigationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class NavigationScene {

    private final NavigationService navigationService;

    @FXML
    private VBox root;

    @FXML
    private Button downloaderButton;

    @FXML
    private Button settingsButton;

    public NavigationScene(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    public Parent create() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/hhlhh/scene/navigation.fxml"));
        loader.setControllerFactory(type -> this);
        try {
            return loader.load();
        } catch (IOException e) {
            return createFallback();
        }
    }

    @FXML
    private void initialize() {
        downloaderButton.setOnAction(event -> navigationService.showDownloader());
        settingsButton.setOnAction(event -> navigationService.showSettings());
    }

    private Parent createFallback() {
        Button downloader = new Button("Downloader");
        downloader.setOnAction(event -> navigationService.showDownloader());
        Button settings = new Button("Settings");
        settings.setOnAction(event -> navigationService.showSettings());
        VBox drawer = new VBox(8, downloader, settings);
        drawer.getStyleClass().add("navigation-drawer");
        return drawer;
    }
}
