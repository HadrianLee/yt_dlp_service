package hhlhh.model;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.stage.Stage;

public class SystemTrayService {

    private static final String TRAY_TOOLTIP = "yt-dlp Service";

    private final ReadOnlyBooleanWrapper traySupported = new ReadOnlyBooleanWrapper(SystemTray.isSupported());

    private TrayIcon trayIcon;
    private Stage attachedStage;
    private Runnable exitAction = Platform::exit;
    private Runnable openDownloadAction = () -> { };
    private Runnable openSettingsAction = () -> { };

    public ReadOnlyBooleanProperty traySupportedProperty() {
        return traySupported.getReadOnlyProperty();
    }

    public boolean isTraySupported() {
        return traySupported.get();
    }

    public void attachStage(Stage stage) {
        attachedStage = stage;
    }

    public void setExitAction(Runnable exitAction) {
        this.exitAction = exitAction != null ? exitAction : Platform::exit;
    }

    public void setOpenDownloadAction(Runnable openDownloadAction) {
        this.openDownloadAction = openDownloadAction != null ? openDownloadAction : () -> { };
    }

    public void setOpenSettingsAction(Runnable openSettingsAction) {
        this.openSettingsAction = openSettingsAction != null ? openSettingsAction : () -> { };
    }

    public void showTrayIcon(Stage stage) {
        if (!isTraySupported()) {
            return;
        }
        attachedStage = stage != null ? stage : attachedStage;
        ensureTrayIcon();
    }

    public void notifyTrayMessage(String title, String message) {
        if (!isTraySupported()) {
            return;
        }

        ensureTrayIcon();
        if (trayIcon != null) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    public void removeTrayIcon() {
        if (trayIcon != null && isTraySupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        trayIcon = null;
    }

    private void ensureTrayIcon() {
        if (trayIcon != null || !isTraySupported()) {
            return;
        }

        PopupMenu menu = new PopupMenu();
        MenuItem downloadItem = new MenuItem("Download");
        downloadItem.addActionListener(event -> Platform.runLater(() -> openAttachedStage(openDownloadAction)));
        MenuItem settingsItem = new MenuItem("Settings");
        settingsItem.addActionListener(event -> Platform.runLater(() -> openAttachedStage(openSettingsAction)));
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(event -> {
            Runnable action = exitAction;
            Platform.runLater(action);
        });
        menu.add(downloadItem);
        menu.add(settingsItem);
        menu.add(exitItem);

        TrayIcon icon = new TrayIcon(createTrayImage(), TRAY_TOOLTIP, menu);
        icon.setImageAutoSize(true);
        icon.addActionListener(event -> Platform.runLater(() -> openAttachedStage(openDownloadAction)));

        try {
            SystemTray.getSystemTray().add(icon);
            trayIcon = icon;
        } catch (AWTException e) {
            trayIcon = null;
        }
    }

    private Image createTrayImage() {
        return AppIconService.loadTrayIcon().orElseGet(this::createFallbackTrayImage);
    }

    private Image createFallbackTrayImage() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(9, 105, 218));
            graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(4, 4, 8, 2);
            graphics.fillRect(7, 4, 2, 7);
            graphics.fillPolygon(new int[] { 4, 12, 8 }, new int[] { 9, 9, 13 }, 3);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private void openAttachedStage(Runnable action) {
        if (attachedStage == null) {
            return;
        }
        attachedStage.show();
        attachedStage.toFront();
        action.run();
    }
}
