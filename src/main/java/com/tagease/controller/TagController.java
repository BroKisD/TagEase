package com.tagease.controller;

import com.tagease.database.DatabaseConfig;
import com.tagease.database.TaggedFileDAO;
import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;
import javafx.scene.control.Alert;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TagController {
    private Connection connection;
    protected TaggedFileDAO fileDAO;

    private static final int MAX_TAG_LENGTH = 50;
    private static final String TAG_REGEX = "^[a-zA-Z0-9 _-]+$";

    public TagController() {
        initializeDatabase();
    }
    
    /**
     * Initializes the database connection and DAO.
     * This method is extracted to make the class more testable.
     */
    protected void initializeDatabase() {
        try {
            this.connection = DatabaseConfig.getConnection();
            this.fileDAO = new TaggedFileDAO(connection);
        } catch (SQLException e) {
            showErrorDialog("Database Error", "Failed to establish database connection", e.getMessage());
            throw new RuntimeException("Failed to establish database connection: " + e.getMessage(), e);
        }
    }

    public void addFile(TaggedFile file, Set<String> existingTags) {
        try {
            // Validate only new tags
            for (String tag : file.getTags()) {
                if (!existingTags.contains(tag)) {
                    validateTag(tag);
                }
            }
            fileDAO.addFile(file, existingTags);
        } catch (SQLException e) {
            if (e.getMessage().contains("File already exists")) {
                showErrorDialog("Duplicate File", "This file has already been added to the system.", "");
            } else {
                showErrorDialog("Error Adding File", 
                    "Could not add file: " + file.getFileName(), 
                    "Database error: " + e.getMessage() + "\nSQL State: " + e.getSQLState());
            }
            throw new RuntimeException("Error adding file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            showErrorDialog("Invalid Tag", 
                "Could not add file due to invalid tags", 
                e.getMessage());
            throw new RuntimeException("Invalid tag: " + e.getMessage(), e);
        }
    }

    public void updateFileTags(TaggedFile file) {
        try {
            // Validate tags before updating
            for (String tag : file.getTags()) {
                validateTag(tag);
            }
            fileDAO.updateFileTags(file);
        } catch (SQLException e) {
            showErrorDialog("Error Updating Tags", 
                "Could not update tags for file: " + file.getFileName(), 
                "Database error: " + e.getMessage() + "\nSQL State: " + e.getSQLState());
            throw new RuntimeException("Error updating file tags: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            showErrorDialog("Invalid Tag", 
                "Could not update file tags due to invalid tags", 
                e.getMessage());
            throw new RuntimeException("Invalid tag: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            fileDAO.deleteFile(filePath);
        } catch (SQLException e) {
            showErrorDialog("Error Deleting File", 
                "Could not delete file: " + filePath, 
                e.getMessage());
            throw new RuntimeException("Error deleting file: " + e.getMessage(), e);
        }
    }

    public List<TaggedFile> getAllFiles() {
        try {
            return fileDAO.getAllFiles();
        } catch (SQLException e) {
            showErrorDialog("Error Loading Files", 
                "Could not load files from database", 
                e.getMessage());
            throw new RuntimeException("Error loading files: " + e.getMessage(), e);
        }
    }

    public Set<String> getAllTags() {
        try {
            return fileDAO.getAllTags();
        } catch (SQLException e) {
            showErrorDialog("Database Error", "Failed to retrieve tags", e.getMessage());
            throw new RuntimeException("Failed to retrieve tags: " + e.getMessage(), e);
        }
    }

    public void deleteTag(String tagName) {
        try {
            validateTag(tagName);
            fileDAO.deleteTag(tagName);
        } catch (SQLException e) {
            showErrorDialog("Error Deleting Tag", 
                "Could not delete tag: " + tagName, 
                "Database error: " + e.getMessage() + "\nSQL State: " + e.getSQLState());
            throw new RuntimeException("Error deleting tag: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            showErrorDialog("Invalid Tag", 
                "Could not delete tag due to invalid tag name", 
                e.getMessage());
            throw new RuntimeException("Invalid tag: " + e.getMessage(), e);
        }
    }

    public void deleteTagFromFile(String filePath, String tagName) {
        try {
            validateTag(tagName);
            fileDAO.deleteTagFromFile(filePath, tagName);
        } catch (SQLException e) {
            showErrorDialog("Error Removing Tag", 
                "Could not remove tag from file", 
                "Database error: " + e.getMessage() + "\nSQL State: " + e.getSQLState());
            throw new RuntimeException("Error removing tag from file: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            showErrorDialog("Invalid Tag", 
                "Could not remove tag from file due to invalid tag name", 
                e.getMessage());
            throw new RuntimeException("Invalid tag: " + e.getMessage(), e);
        }
    }

    public List<TaggedFile> getFilesByTags(Set<String> tags) {
        try {
            return fileDAO.getFilesByTags(tags);
        } catch (SQLException e) {
            showErrorDialog("Error", "Failed to get files by tags", e.getMessage());
            throw new RuntimeException("Error getting files by tags: " + e.getMessage(), e);
        }
    }

    public void addTag(String tagName) {
        try {
            validateTag(tagName);
            Tag tag = new Tag(tagName);
            fileDAO.addTag(tag);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add tag: " + e.getMessage(), e);
        }
    }

    public void removeTag(String tagName) {
        try {
            fileDAO.deleteTag(tagName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove tag: " + e.getMessage(), e);
        }
    }
    
    /**
     * Updates the color of a tag.
     * 
     * @param tag The tag with the updated color
     */
    public void updateTagColor(Tag tag) {
        try {
            fileDAO.updateTagColor(tag);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update tag color: " + e.getMessage(), e);
        }
    }
    
    /**
     * Gets all tags with their colors.
     * 
     * @return A map of tag names to Tag objects
     */
    public Map<String, Tag> getAllTagsWithColors() {
        try {
            return fileDAO.getAllTagsWithColors();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get tags with colors: " + e.getMessage(), e);
        }
    }

    /**
     * Checks all files in the database and adds the "Missing" tag to those that don't exist.
     */
    public void checkForMissingFiles() {
        try {
            List<TaggedFile> allFiles = getAllFiles();
            boolean changesDetected = false;
            
            for (TaggedFile file : allFiles) {
                File physicalFile = new File(file.getFilePath());
                
                // If file doesn't exist and doesn't already have the Missing tag
                if (!physicalFile.exists() && !file.getTags().contains(Tag.TAG_MISSING)) {
                    // Add the Missing tag
                    Set<String> updatedTags = new HashSet<>(file.getTags());
                    updatedTags.add(Tag.TAG_MISSING);
                    file.setTags(updatedTags);
                    
                    // Update the file in the database
                    fileDAO.updateFileTags(file);
                    changesDetected = true;
                }
                
                // If file exists but has the Missing tag, remove it
                if (physicalFile.exists() && file.getTags().contains(Tag.TAG_MISSING)) {
                    // Remove the Missing tag
                    Set<String> updatedTags = file.getTags().stream()
                            .filter(tag -> !tag.equals(Tag.TAG_MISSING))
                            .collect(Collectors.toSet());
                    file.setTags(updatedTags);
                    
                    // Update the file in the database
                    fileDAO.updateFileTags(file);
                    changesDetected = true;
                }
            }
            
            // Return whether any changes were made
            if (changesDetected) {
                System.out.println("Updated missing file tags");
            }
        } catch (Exception e) {
            System.err.println("Error checking for missing files: " + e.getMessage());
        }
    }

    private void validateTag(String tag) throws IllegalArgumentException {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be empty");
        }
        if (tag.length() > MAX_TAG_LENGTH) {
            throw new IllegalArgumentException("Tag cannot be longer than " + MAX_TAG_LENGTH + " characters");
        }
        if (!tag.matches(TAG_REGEX)) {
            throw new IllegalArgumentException("Tag can only contain letters, numbers, spaces, underscores and hyphens");
        }
    }

    protected void showErrorDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (Exception e) {
            showErrorDialog("Error Closing Connection", 
                "Could not close database connection", 
                e.getMessage());
            throw new RuntimeException("Error closing database connection: " + e.getMessage(), e);
        }
    }
}
