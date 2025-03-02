module com.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    exports com.tagease;
    exports com.tagease.model;
    exports com.tagease.controller;
    exports com.tagease.database;
    
    opens com.tagease to javafx.fxml;
    opens com.tagease.controller to javafx.fxml;
}
