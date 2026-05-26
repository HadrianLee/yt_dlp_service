package hhlhh.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Path;

import hhlhh.desktop.FakeSystemTrayService;
import hhlhh.model.SettingsService;
import hhlhh.test.fake.FakeDependencyManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

class NavigationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void updateThemeClassAddsAndRemovesDarkClassWithoutDuplicates() {
        SettingsService settingsService = new SettingsService(
                tempDir.resolve("settings.properties"),
                new FakeDependencyManager(tempDir)
        );
        NavigationService navigationService = new NavigationService(settingsService, new FakeSystemTrayService(true));
        ObservableList<String> styleClasses = FXCollections.observableArrayList("app-shell");

        navigationService.updateThemeClass(styleClasses, true);
        navigationService.updateThemeClass(styleClasses, true);
        assertTrue(styleClasses.contains("dark"));
        assertEquals(1, styleClasses.stream().filter("dark"::equals).count());

        navigationService.updateThemeClass(styleClasses, false);
        assertFalse(styleClasses.contains("dark"));
    }

    @Test
    void applyThemeIgnoresNullScene() {
        SettingsService settingsService = new SettingsService(
                tempDir.resolve("settings.properties"),
                new FakeDependencyManager(tempDir)
        );
        NavigationService navigationService = new NavigationService(settingsService, new FakeSystemTrayService(true));

        navigationService.applyTheme(null);
    }

    @Test
    void showSettingsAndDownloaderSwitchContentAndHideNavigation() throws Exception {
        NavigationService navigationService = new NavigationService(settingsService(), new FakeSystemTrayService(true));
        BorderPane root = new BorderPane();
        Pane downloaderContent = new Pane();
        Pane settingsContent = new Pane();
        Pane navigationDrawer = new Pane();
        navigationDrawer.setVisible(true);
        navigationDrawer.setManaged(true);
        installNavigationState(navigationService, root, downloaderContent, settingsContent, navigationDrawer);

        navigationService.showSettings();
        assertSame(settingsContent, root.getCenter());
        assertFalse(navigationDrawer.isVisible());
        assertFalse(navigationDrawer.isManaged());

        navigationDrawer.setVisible(true);
        navigationDrawer.setManaged(true);
        navigationService.showDownloader();
        assertSame(downloaderContent, root.getCenter());
        assertFalse(navigationDrawer.isVisible());
        assertFalse(navigationDrawer.isManaged());
    }

    @Test
    void toggleNavigationFlipsVisibleAndManagedTogether() throws Exception {
        NavigationService navigationService = new NavigationService(settingsService(), new FakeSystemTrayService(true));
        Pane navigationDrawer = new Pane();
        navigationDrawer.setVisible(false);
        navigationDrawer.setManaged(false);
        installNavigationState(navigationService, new BorderPane(), new Pane(), new Pane(), navigationDrawer);

        navigationService.toggleNavigation();
        assertTrue(navigationDrawer.isVisible());
        assertTrue(navigationDrawer.isManaged());

        navigationService.toggleNavigation();
        assertFalse(navigationDrawer.isVisible());
        assertFalse(navigationDrawer.isManaged());
    }

    private SettingsService settingsService() {
        return new SettingsService(
                tempDir.resolve("settings.properties"),
                new FakeDependencyManager(tempDir)
        );
    }

    private void installNavigationState(
            NavigationService navigationService,
            BorderPane root,
            Node downloaderContent,
            Node settingsContent,
            Node navigationDrawer
    ) throws Exception {
        setField(navigationService, "root", root);
        setField(navigationService, "downloaderContent", downloaderContent);
        setField(navigationService, "settingsContent", settingsContent);
        setField(navigationService, "navigationDrawer", navigationDrawer);
    }

    private void setField(NavigationService navigationService, String fieldName, Object value) throws Exception {
        Field field = NavigationService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(navigationService, value);
    }
}
