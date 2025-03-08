package com.tagease.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tagease.database.DatabaseConfig;
import com.tagease.model.TaggedFile;
import com.tagease.view.MainView;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * Utility class for performing health checks on the application.
 * Checks database existence, file path integrity, and other system health aspects.
 */
public class HealthCheck {
    
    private static final String DB_NAME = "tagease.db";
    
    /**
     * Result class for health check operations
     */
    public static class HealthCheckResult {
        private boolean success;
        private String message;
        private final Map<String, List<FilePathStatus>> filePathIssues;
        
        public HealthCheckResult() {
            this.success = true;
            this.message = "Health check completed successfully.";
            this.filePathIssues = new HashMap<>();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public Map<String, List<FilePathStatus>> getFilePathIssues() {
            return filePathIssues;
        }
        
        public void addFilePathIssue(String category, FilePathStatus status) {
            filePathIssues.computeIfAbsent(category, k -> new ArrayList<>()).add(status);
        }
        
        public boolean hasFilePathIssues() {
            return !filePathIssues.isEmpty();
        }
    }
    
    /**
     * Status class for file path issues
     */
    public static class FilePathStatus {
        private final String filePath;
        private final String fileName;
        private final FileStatus status;
        
        public FilePathStatus(String filePath, String fileName, FileStatus status) {
            this.filePath = filePath;
            this.fileName = fileName;
            this.status = status;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public FileStatus getStatus() {
            return status;
        }
    }
    
    /**
     * Enum representing the status of a file
     */
    public enum FileStatus {
        MISSING("File is missing", "yellow"),
        OK("File is accessible", "green");
        
        private final String description;
        private final String color;
        
        FileStatus(String description, String color) {
            this.description = description;
            this.color = color;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getColor() {
            return color;
        }
    }
    
    /**
     * Performs a comprehensive health check on the application.
     * 
     * @param mainView The main view of the application to update UI elements
     */
    public static void performHealthCheck(MainView mainView) {
        HealthCheckResult result = new HealthCheckResult();
        
        // Check database existence
        checkDatabaseExistence(result);
        
        // Check file paths integrity if database exists
        if (result.isSuccess()) {
            checkFilePathsIntegrity(result);
        }
        
        // Check system resources
        checkSystemResources(result);
        
        // Update UI with missing files highlighted
        updateUIWithMissingFiles(mainView, result);
        
        // Show notification if database was newly created
        if (!result.isSuccess() && result.getMessage().contains("New database initialized")) {
            showDatabaseInitializedNotification();
        }
    }
    
    /**
     * Checks if the database exists and is accessible.
     * If not, initializes a new database.
     * 
     * @param result The health check result to update
     */
    private static void checkDatabaseExistence(HealthCheckResult result) {
        File dbFile = new File(DB_NAME);
        
        if (!dbFile.exists()) {
            result.setSuccess(false);
            result.setMessage("Database file not found. A new database will be initialized.");
            
            // Try to initialize the database
            try {
                Connection conn = DatabaseConfig.getConnection();
                if (conn != null && !conn.isClosed()) {
                    result.setSuccess(true);
                    result.setMessage("New database initialized successfully.");
                }
            } catch (SQLException e) {
                result.setSuccess(false);
                result.setMessage("Failed to initialize database: " + e.getMessage());
            }
        } else {
            try {
                Connection conn = DatabaseConfig.getConnection();
                if (conn != null && !conn.isClosed()) {
                    result.setSuccess(true);
                    result.setMessage("Database connection successful.");
                }
            } catch (SQLException e) {
                result.setSuccess(false);
                result.setMessage("Database exists but connection failed: " + e.getMessage());
            }
        }
    }
    
    /**
     * Checks the integrity of file paths stored in the database.
     * Identifies missing files.
     * 
     * @param result The health check result to update
     */
    private static void checkFilePathsIntegrity(HealthCheckResult result) {
        try {
            Connection conn = DatabaseConfig.getConnection();
            List<TaggedFile> files = new com.tagease.database.TaggedFileDAO(conn).getAllFiles();
            
            for (TaggedFile file : files) {
                File physicalFile = new File(file.getFilePath());
                
                if (!physicalFile.exists()) {
                    // File is missing (either deleted or moved)
                    result.addFilePathIssue("missing", 
                        new FilePathStatus(file.getFilePath(), file.getFileName(), FileStatus.MISSING));
                }
            }
        } catch (SQLException e) {
            result.setSuccess(false);
            result.setMessage("Failed to check file paths due to database error: " + e.getMessage());
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Failed to check file paths: " + e.getMessage());
        }
    }
    
    /**
     * Checks system resources like available memory and disk space.
     * 
     * @param result The health check result to update
     */
    private static void checkSystemResources(HealthCheckResult result) {
        // Check available memory
        long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
        
        if (freeMemory < 50) { // Less than 50MB free
            result.setSuccess(false);
            result.setMessage(result.getMessage() + " Low memory available: " + freeMemory + "MB out of " + totalMemory + "MB.");
        }
        
        // Check disk space where the database is stored
        File dbFile = new File(DB_NAME);
        // Get absolute file to ensure we have a valid path
        File absoluteDbFile = dbFile.getAbsoluteFile();
        // Get the directory containing the database file
        File dbDirectory = absoluteDbFile.getParentFile();
        
        if (dbDirectory != null) {
            long freeSpace = dbDirectory.getFreeSpace() / (1024 * 1024);
            
            if (freeSpace < 100) { // Less than 100MB free
                result.setSuccess(false);
                result.setMessage(result.getMessage() + " Low disk space: " + freeSpace + "MB free.");
            }
        } else {
            // If we can't determine the parent directory, use the current directory
            File currentDir = new File(".");
            long freeSpace = currentDir.getFreeSpace() / (1024 * 1024);
            
            if (freeSpace < 100) { // Less than 100MB free
                result.setSuccess(false);
                result.setMessage(result.getMessage() + " Low disk space: " + freeSpace + "MB free.");
            }
        }
    }
    
    /**
     * Updates the UI to highlight missing files in yellow.
     * 
     * @param mainView The main view to update
     * @param result The health check result containing missing files
     */
    private static void updateUIWithMissingFiles(MainView mainView, HealthCheckResult result) {
        if (result.hasFilePathIssues() && result.getFilePathIssues().containsKey("missing")) {
            List<FilePathStatus> missingFiles = result.getFilePathIssues().get("missing");
            
            // Get the file paths of all missing files
            List<String> missingFilePaths = new ArrayList<>();
            for (FilePathStatus status : missingFiles) {
                missingFilePaths.add(status.getFilePath());
            }
            
            // Update the UI to highlight these files
            mainView.highlightMissingFiles(missingFilePaths);
        }
    }
    
    /**
     * Shows a notification that a new database was initialized.
     */
    private static void showDatabaseInitializedNotification() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Database Initialized");
        alert.setHeaderText("New Database Created");
        alert.setContentText("A new database has been initialized. You can now start adding files and tags.");
        alert.show();
    }
}
