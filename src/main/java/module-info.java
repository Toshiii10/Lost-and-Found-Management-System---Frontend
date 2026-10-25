module com.lostandfound {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;

    opens com.lostandfound to javafx.fxml, com.fasterxml.jackson.databind;
    exports com.lostandfound;
}