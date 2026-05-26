package hhlhh.ui;

import hhlhh.scene.ExitDialogScene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ExitDialog {

    private final String title;
    private final String message;
    private boolean exitConfirmed;

    public ExitDialog(String title, String message) {
        this.title = title;
        this.message = message;
    }

    public boolean showAndWait(Stage owner) {
        exitConfirmed = false;

        Stage dialogStage = new Stage();
        dialogStage.initOwner(owner);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.setTitle(title);
        dialogStage.setResizable(false);
        dialogStage.setScene(new ExitDialogScene(this, owner).create(dialogStage));
        dialogStage.showAndWait();

        return exitConfirmed;
    }

    public void confirmExit(Stage dialogStage) {
        exitConfirmed = true;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void cancelExit(Stage dialogStage) {
        exitConfirmed = false;
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    boolean isExitConfirmed() {
        return exitConfirmed;
    }
}
