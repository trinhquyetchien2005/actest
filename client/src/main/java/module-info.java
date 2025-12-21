module com.actest.client {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.fasterxml.jackson.databind;

    opens com.actest.client to javafx.fxml;
    opens com.actest.client.controller to javafx.fxml;

    exports com.actest.client;
    exports com.actest.client.controller;
    exports com.actest.client.model;

    opens com.actest.client.model to com.fasterxml.jackson.databind;
}
