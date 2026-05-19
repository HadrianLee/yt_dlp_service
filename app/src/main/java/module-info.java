module hhlhh {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    opens hhlhh to javafx.fxml;
    exports hhlhh;
}
