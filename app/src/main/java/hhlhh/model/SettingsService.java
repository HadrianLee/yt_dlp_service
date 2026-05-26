package hhlhh.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class SettingsService {

    public static final String DARK_MODE = "darkMode";
    public static final String CONSUME_EXIT_DURING_DOWNLOAD = "consumeExitDuringDownload";
    public static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";
    public static final String USE_POSTPROCESS_PIPELINE = "usePostprocessPipeline";

    private final Path settingsPath;
    private final PropertyChangeSupport propertyChanges = new PropertyChangeSupport(this);
    private final DependencyManager dependencyManager;
    private boolean darkMode;
    private boolean closeToTrayDuringDownload = true;
    private boolean notificationsEnabled = true;
    private boolean usePostprocessPipeline;

    public SettingsService() {
        this(AppPaths.appDirectory().resolve("settings.properties"), new DependencyManager());
    }

    SettingsService(Path settingsPath) {
        this(settingsPath, new DependencyManager());
    }

    public SettingsService(Path settingsPath, DependencyManager dependencyManager) {
        this.settingsPath = settingsPath;
        this.dependencyManager = dependencyManager;
        load();
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChanges.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChanges.removePropertyChangeListener(propertyName, listener);
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        update(DARK_MODE, this.darkMode, darkMode, () -> this.darkMode = darkMode);
    }

    public boolean shouldCloseToTrayDuringDownload() {
        return closeToTrayDuringDownload;
    }

    public void setCloseToTrayDuringDownload(boolean closeToTrayDuringDownload) {
        update(
                CONSUME_EXIT_DURING_DOWNLOAD,
                this.closeToTrayDuringDownload,
                closeToTrayDuringDownload,
                () -> this.closeToTrayDuringDownload = closeToTrayDuringDownload
        );
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        update(
                NOTIFICATIONS_ENABLED,
                this.notificationsEnabled,
                notificationsEnabled,
                () -> this.notificationsEnabled = notificationsEnabled
        );
    }

    public boolean shouldUsePostprocessPipeline() {
        return usePostprocessPipeline;
    }

    public void setUsePostprocessPipeline(boolean usePostprocessPipeline) {
        update(
                USE_POSTPROCESS_PIPELINE,
                this.usePostprocessPipeline,
                usePostprocessPipeline,
                () -> this.usePostprocessPipeline = usePostprocessPipeline
        );
    }

    public String getDependencyDirectoryPath() {
        return dependencyManager.getBinDirectoryPath();
    }

    public void repairDependencies() throws Exception {
        dependencyManager.repairDependencies();
    }

    private void update(String propertyName, boolean oldValue, boolean newValue, Runnable assignment) {
        if (oldValue == newValue) {
            return;
        }

        assignment.run();
        save();
        propertyChanges.firePropertyChange(propertyName, oldValue, newValue);
    }

    private void load() {
        if (!Files.isRegularFile(settingsPath)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(settingsPath)) {
            properties.load(input);
            darkMode = Boolean.parseBoolean(properties.getProperty(DARK_MODE, Boolean.toString(darkMode)));
            closeToTrayDuringDownload = Boolean.parseBoolean(properties.getProperty(
                    CONSUME_EXIT_DURING_DOWNLOAD,
                    Boolean.toString(closeToTrayDuringDownload)
            ));
            notificationsEnabled = Boolean.parseBoolean(properties.getProperty(
                    NOTIFICATIONS_ENABLED,
                    Boolean.toString(notificationsEnabled)
            ));
            usePostprocessPipeline = Boolean.parseBoolean(properties.getProperty(
                    USE_POSTPROCESS_PIPELINE,
                    Boolean.toString(usePostprocessPipeline)
            ));
        } catch (IOException e) {
            // Keep defaults when settings cannot be read.
        }
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty(DARK_MODE, Boolean.toString(darkMode));
        properties.setProperty(CONSUME_EXIT_DURING_DOWNLOAD, Boolean.toString(closeToTrayDuringDownload));
        properties.setProperty(NOTIFICATIONS_ENABLED, Boolean.toString(notificationsEnabled));
        properties.setProperty(USE_POSTPROCESS_PIPELINE, Boolean.toString(usePostprocessPipeline));

        try {
            Files.createDirectories(settingsPath.getParent());
            try (OutputStream output = Files.newOutputStream(settingsPath)) {
                properties.store(output, "yt-dlp service settings");
            }
        } catch (IOException e) {
            // Settings changes should not block normal app use.
        }
    }
}
