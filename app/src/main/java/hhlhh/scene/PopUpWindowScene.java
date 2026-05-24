package hhlhh.scene;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class PopUpWindowScene {

    private static final String APP_STYLESHEET = "/hhlhh/style/app.css";
    private static final String DARK_CLASS = "dark";

    private PopUpWindowScene() {
    }

    public static Scene create(Parent root, Stage ownerStage) {
        applyOwnerTheme(root, ownerStage);
        Scene scene = new Scene(root);
        var stylesheet = PopUpWindowScene.class.getResource(APP_STYLESHEET);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
        return scene;
    }

    private static void applyOwnerTheme(Parent root, Stage ownerStage) {
        if (ownerStage == null || ownerStage.getScene() == null) {
            return;
        }

        Parent ownerRoot = ownerStage.getScene().getRoot();
        if (ownerRoot.getStyleClass().contains(DARK_CLASS)
                && !root.getStyleClass().contains(DARK_CLASS)) {
            root.getStyleClass().add(DARK_CLASS);
        }
    }
}
