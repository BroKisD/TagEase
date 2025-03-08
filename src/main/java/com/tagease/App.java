package com.tagease;

import java.sql.Connection;
import java.sql.SQLException;

import com.tagease.controller.TagController;
import com.tagease.database.DatabaseConfig;
import com.tagease.utils.HealthCheck;
import com.tagease.view.MainView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Check database existence before starting the application
        if (!checkDatabaseConnection()) {
            // If database connection failed, show error and exit
            showDatabaseErrorAndExit();
            return;
        }
        
        // Initialize the application
        TagController controller = new TagController();
        MainView mainView = new MainView(primaryStage, controller);
        mainView.show();
        
        // Run health check after UI is displayed
        Platform.runLater(() -> {
            HealthCheck.performHealthCheck(mainView);
        });
    }
    
    /**
     * Checks if the database exists and can be connected to.
     * If the database doesn't exist, it will be created.
     * 
     * @return true if database connection is successful, false otherwise
     */
    private boolean checkDatabaseConnection() {
        try {
            boolean dbExisted = DatabaseConfig.databaseExists();
            Connection conn = DatabaseConfig.getConnection();
            
            if (conn != null && !conn.isClosed()) {
                // If database was newly created, show notification
                if (!dbExisted) {
                    Platform.runLater(() -> {
                        showDatabaseInitializedNotification();
                    });
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Shows a notification that a new database was initialized.
     */
    private void showDatabaseInitializedNotification() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Database Initialized");
        alert.setHeaderText("New Database Created");
        alert.setContentText("A new database has been initialized");
        alert.show();
    }
    
    /**
     * Shows a database error alert and exits the application.
     */
    private void showDatabaseErrorAndExit() {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Database Error");
        alert.setHeaderText("Database Connection Failed");
        alert.setContentText("Failed to connect to the database. The application will now exit.");
        alert.showAndWait();
        
        // Exit the application
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}