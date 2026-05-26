package hhlhh.desktop;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.stage.Stage;

public final class FakeSystemTrayService extends SystemTrayService {

    private final ReadOnlyBooleanWrapper traySupported;
    private int notificationCount;
    private int removeCount;

    public FakeSystemTrayService(boolean traySupported) {
        this.traySupported = new ReadOnlyBooleanWrapper(traySupported);
    }

    @Override
    public ReadOnlyBooleanProperty traySupportedProperty() {
        return traySupported.getReadOnlyProperty();
    }

    @Override
    public boolean isTraySupported() {
        return traySupported.get();
    }

    @Override
    public void attachStage(Stage stage) {
    }

    @Override
    public void setExitAction(Runnable exitAction) {
    }

    @Override
    public void setOpenDownloadAction(Runnable openDownloadAction) {
    }

    @Override
    public void setOpenSettingsAction(Runnable openSettingsAction) {
    }

    @Override
    public void showTrayIcon(Stage stage) {
    }

    @Override
    public void notifyTrayMessage(String title, String message) {
        notificationCount++;
    }

    @Override
    public void removeTrayIcon() {
        removeCount++;
    }

    int notificationCount() {
        return notificationCount;
    }

    int removeCount() {
        return removeCount;
    }
}
