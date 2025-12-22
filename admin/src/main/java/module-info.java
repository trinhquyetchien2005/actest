module com.actest.admin {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.net.http;
    requires org.xerial.sqlitejdbc;
    requires com.fasterxml.jackson.databind;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires java.desktop;
    requires javafx.swing;
    requires webcam.capture;

    opens com.actest.admin to javafx.fxml;
    opens com.actest.admin.offline.controller to javafx.fxml;
    opens com.actest.admin.online.controller to javafx.fxml;
    opens com.actest.admin.controller to javafx.fxml;

    exports com.actest.admin;
    exports com.actest.admin.offline.controller;
    exports com.actest.admin.online.controller;
    exports com.actest.admin.controller;
    exports com.actest.admin.model;

    opens com.actest.admin.model to com.fasterxml.jackson.databind;
}
