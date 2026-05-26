module hhlhh {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires java.desktop;

    opens hhlhh to javafx.fxml;
    opens hhlhh.desktop;
    opens hhlhh.model;
    opens hhlhh.scene to javafx.fxml;
    opens hhlhh.ui;
    exports hhlhh;
}
