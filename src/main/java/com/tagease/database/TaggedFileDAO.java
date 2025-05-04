package com.tagease.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;

public class TaggedFileDAO {
    private final Connection connection;

    private static final int MAX_TAG_LENGTH = 50;
    private static final String TAG_REGEX = "^[a-zA-Z0-9 _-]+$";

    public TaggedFileDAO(Connection connection) {
        this.connection = connection;
        initializeDefaultTags();
    }

    /**
     * Initializes the default system tags with their predefined colors.
     */
    private void initializeDefaultTags() {
        try {
            // Check if default tags exist
            Set<String> existingTags = getAllTags();
            
            // Add default tags if they don't exist
            if (!existingTags.contains(Tag.TAG_DONE)) {
                addTag(new Tag(Tag.TAG_DONE, Tag.COLOR_DONE));
            }
            if (!existingTags.contains(Tag.TAG_IN_PROGRESS)) {
                addTag(new Tag(Tag.TAG_IN_PROGRESS, Tag.COLOR_IN_PROGRESS));
            }
            if (!existingTags.contains(Tag.TAG_NEW)) {
                addTag(new Tag(Tag.TAG_NEW, Tag.COLOR_NEW));
            }
            if (!existingTags.contains(Tag.TAG_MISSING)) {
                addTag(new Tag(Tag.TAG_MISSING, Tag.COLOR_MISSING));
            }
            
            // Update colors for default tags in case they already exist but don't have colors
            updateTagColorIfNeeded(Tag.TAG_DONE, Tag.COLOR_DONE);
            updateTagColorIfNeeded(Tag.TAG_IN_PROGRESS, Tag.COLOR_IN_PROGRESS);
            updateTagColorIfNeeded(Tag.TAG_NEW, Tag.COLOR_NEW);
            updateTagColorIfNeeded(Tag.TAG_MISSING, Tag.COLOR_MISSING);
            
        } catch (SQLException e) {
            System.err.println("Failed to initialize default tags: " + e.getMessage());
        }
    }
    
    /**
     * Updates the color of a tag if it doesn't have one.
     */
    private void updateTagColorIfNeeded(String tagName, String color) throws SQLException {
        String sql = "UPDATE tags SET color = ? WHERE tag_name = ? AND (color IS NULL OR color = '')";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, color);
            pstmt.setString(2, tagName);
            pstmt.executeUpdate();
        }
    }

    private void validateTag(String tag) throws SQLException {
        if (tag == null || tag.trim().isEmpty()) {
            throw new SQLException("Tag cannot be empty", "TAG_EMPTY");
        }
        if (tag.length() > MAX_TAG_LENGTH) {
            throw new SQLException("Tag cannot be longer than " + MAX_TAG_LENGTH + " characters", "TAG_TOO_LONG");
        }
        if (!tag.matches(TAG_REGEX)) {
            throw new SQLException("Tag can only contain letters, numbers, spaces, underscores and hyphens", "TAG_INVALID_CHARS");
        }
    }

    public void addFile(TaggedFile file, Set<String> existingTags) throws SQLException {
        String insertFileSql = "INSERT INTO files (file_path, file_name, created_at, last_accessed_at) VALUES (?, ?, ?, ?)";
        String insertTagSql = "INSERT OR IGNORE INTO tags (tag_name, color) VALUES (?, ?)";
        String insertFileTagSql = "INSERT INTO file_tags (file_path, tag_id) SELECT ?, tag_id FROM tags WHERE tag_name = ?";
        String insertRelationshipSql = "INSERT INTO file_relationships (source_file_path, related_file_path) VALUES (?, ?)";

        connection.setAutoCommit(false);
        try {
            // Validate tags before adding
            for (String tagName : file.getTags()) {
                if (!existingTags.contains(tagName)) {
                    validateTag(tagName);
                }
            }

            // Check if file already exists
            String checkSQL = "SELECT COUNT(*) FROM files WHERE file_path = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(checkSQL)) {
                pstmt.setString(1, file.getFilePath());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("File already exists in the database");
                }
            }

            // Insert file
            try (PreparedStatement pstmt = connection.prepareStatement(insertFileSql)) {
                pstmt.setString(1, file.getFilePath());
                pstmt.setString(2, file.getFileName());
                pstmt.setTimestamp(3, Timestamp.valueOf(file.getCreatedAt()));
                pstmt.setTimestamp(4, Timestamp.valueOf(file.getLastAccessedAt()));
                pstmt.executeUpdate();
            }

            // Insert tags and file-tag relationships
            try (PreparedStatement tagStmt = connection.prepareStatement(insertTagSql);
                 PreparedStatement fileTagStmt = connection.prepareStatement(insertFileTagSql)) {
                
                for (String tagName : file.getTags()) {
                    // Insert tag if it doesn't exist
                    if (!existingTags.contains(tagName)) {
                        Tag newTag = new Tag(tagName);
                        tagStmt.setString(1, newTag.getName());
                        tagStmt.setString(2, newTag.getColorHex());
                        tagStmt.executeUpdate();
                    }

                    // Create file-tag relationship
                    fileTagStmt.setString(1, file.getFilePath());
                    fileTagStmt.setString(2, tagName);
                    fileTagStmt.executeUpdate();
                }
            }

            // Insert file relationships
            if (file.getRelatedFiles() != null && !file.getRelatedFiles().isEmpty()) {
                try (PreparedStatement relStmt = connection.prepareStatement(insertRelationshipSql)) {
                    for (TaggedFile relatedFile : file.getRelatedFiles()) {
                        relStmt.setString(1, file.getFilePath());
                        relStmt.setString(2, relatedFile.getFilePath());
                        relStmt.executeUpdate();
                    }
                }
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void updateFileTags(TaggedFile file) throws SQLException {
        String deleteTagsSql = "DELETE FROM file_tags WHERE file_path = ?";
        String insertTagSql = "INSERT OR IGNORE INTO tags (tag_name, color) VALUES (?, ?)";
        String insertFileTagSql = "INSERT INTO file_tags (file_path, tag_id) SELECT ?, tag_id FROM tags WHERE tag_name = ?";
        String updateAccessTimeSql = "UPDATE files SET last_accessed_at = ? WHERE file_path = ?";

        connection.setAutoCommit(false);
        try {
            // Validate tags before updating
            for (String tagName : file.getTags()) {
                validateTag(tagName);
            }

            // Delete existing tags
            try (PreparedStatement pstmt = connection.prepareStatement(deleteTagsSql)) {
                pstmt.setString(1, file.getFilePath());
                pstmt.executeUpdate();
            }

            // Get all existing tags with their colors
            Map<String, Tag> existingTags = getAllTagsWithColors();

            // Insert new tags
            try (PreparedStatement tagStmt = connection.prepareStatement(insertTagSql);
                 PreparedStatement fileTagStmt = connection.prepareStatement(insertFileTagSql)) {
                
                for (String tagName : file.getTags()) {
                    // Use existing tag if available, otherwise create a new one
                    Tag tag;
                    if (existingTags.containsKey(tagName)) {
                        tag = existingTags.get(tagName);
                    } else {
                        tag = new Tag(tagName);
                    }
                    
                    tagStmt.setString(1, tag.getName());
                    tagStmt.setString(2, tag.getColorHex());
                    tagStmt.executeUpdate();

                    // Create file-tag relationship
                    fileTagStmt.setString(1, file.getFilePath());
                    fileTagStmt.setString(2, tagName);
                    fileTagStmt.executeUpdate();
                }
            }

            // Update last accessed time
            try (PreparedStatement pstmt = connection.prepareStatement(updateAccessTimeSql)) {
                pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                pstmt.setString(2, file.getFilePath());
                pstmt.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public void deleteFile(String filePath) throws SQLException {
        String deleteFileTagsSql = "DELETE FROM file_tags WHERE file_path = ?";
        String deleteFileRelationshipsSql = "DELETE FROM file_relationships WHERE source_file_path = ? OR related_file_path = ?";
        String deleteFileSql = "DELETE FROM files WHERE file_path = ?";
    
        connection.setAutoCommit(false);
        try {
            // Delete file-tag relationships
            try (PreparedStatement pstmt = connection.prepareStatement(deleteFileTagsSql)) {
                pstmt.setString(1, filePath);
                pstmt.executeUpdate();
            }
    
            // Delete file relationships
            try (PreparedStatement pstmt = connection.prepareStatement(deleteFileRelationshipsSql)) {
                pstmt.setString(1, filePath);
                pstmt.setString(2, filePath);
                pstmt.executeUpdate();
            }
    
            // Delete the file
            try (PreparedStatement pstmt = connection.prepareStatement(deleteFileSql)) {
                pstmt.setString(1, filePath);
                pstmt.executeUpdate();
            }
    
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<TaggedFile> getAllFiles() throws SQLException {
        String sql = """
            SELECT f.*, GROUP_CONCAT(t.tag_name) as tags
            FROM files f
            LEFT JOIN file_tags ft ON f.file_path = ft.file_path
            LEFT JOIN tags t ON ft.tag_id = t.tag_id
            GROUP BY f.file_path
        """;

        List<TaggedFile> files = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String filePath = rs.getString("file_path");
                String fileName = rs.getString("file_name");
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                LocalDateTime lastAccessedAt = rs.getTimestamp("last_accessed_at").toLocalDateTime();
                
                Set<String> tags = new HashSet<>();
                String tagString = rs.getString("tags");
                if (tagString != null) {
                    tags.addAll(Arrays.asList(tagString.split(",")));
                }
                
                TaggedFile file = new TaggedFile(fileName, filePath, tags);
                file.setCreatedAt(createdAt);
                file.setLastAccessedAt(lastAccessedAt);
                files.add(file);
            }
        }
        return files;
    }

    public Set<String> getAllTags() throws SQLException {
        Set<String> tags = new HashSet<>();
        String sql = "SELECT tag_name FROM tags";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(rs.getString("tag_name"));
            }
        }
        return tags;
    }

    public void deleteTag(String tagName) throws SQLException {
        // Don't allow deleting system tags
        Tag tag = new Tag(tagName);
        if (tag.isSystemTag()) {
            throw new SQLException("Cannot delete system tag: " + tagName);
        }
        
        // Save the current auto-commit state
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            // Disable auto-commit to start a transaction
            connection.setAutoCommit(false);
            
            // First delete all file-tag relationships
            String deleteRelationsSQL = "DELETE FROM file_tags WHERE tag_id = (SELECT tag_id FROM tags WHERE tag_name = ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteRelationsSQL)) {
                pstmt.setString(1, tagName);
                int relationRowsAffected = pstmt.executeUpdate();
                System.out.println("Deleted " + relationRowsAffected + " file-tag relationships for tag: " + tagName);
            }
    
            // Then delete the tag itself
            String deleteTagSQL = "DELETE FROM tags WHERE tag_name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(deleteTagSQL)) {
                pstmt.setString(1, tagName);
                int tagRowsAffected = pstmt.executeUpdate();
                System.out.println("Deleted " + tagRowsAffected + " tag entries for tag: " + tagName);
            }
            
            // Commit the transaction
            connection.commit();
            System.out.println("Successfully deleted tag: " + tagName);
            
        } catch (SQLException e) {
            // If there's an error, roll back the transaction
            try {
                connection.rollback();
                System.err.println("Transaction rolled back due to error: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                System.err.println("Failed to roll back transaction: " + rollbackEx.getMessage());
            }
            throw e; // Re-throw the original exception
        } finally {
            // Restore the original auto-commit state
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException autoCommitEx) {
                System.err.println("Failed to restore auto-commit state: " + autoCommitEx.getMessage());
            }
        }
    }

    public void deleteTagFromFile(String filePath, String tagName) throws SQLException {
        String sql = """
            DELETE FROM file_tags 
            WHERE file_path = ? AND tag_id = (
                SELECT tag_id FROM tags WHERE tag_name = ?
            )
        """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, tagName);
            pstmt.executeUpdate();
        }
    }

    public List<TaggedFile> getFilesByTags(Set<String> tags) throws SQLException {
        List<TaggedFile> files = new ArrayList<>();
        String sql = "SELECT f.* FROM files f " +
                    "JOIN file_tags ft ON f.file_path = ft.file_path " +
                    "JOIN tags t ON ft.tag_id = t.tag_id " +
                    "WHERE t.tag_name IN (" + String.join(",", Collections.nCopies(tags.size(), "?")) + ") " +
                    "GROUP BY f.file_path HAVING COUNT(DISTINCT t.tag_name) = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int i = 1;
            for (String tag : tags) {
                stmt.setString(i++, tag);
            }
            stmt.setInt(i, tags.size());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    files.add(new TaggedFile(
                        rs.getString("file_name"),
                        rs.getString("file_path"),
                        getTagsForFile(rs.getString("file_path"))
                    ));
                }
            }
        }
        return files;
    }

    private Set<String> getTagsForFile(String filePath) throws SQLException {
        Set<String> tags = new HashSet<>();
        String sql = "SELECT t.tag_name FROM tags t JOIN file_tags ft ON t.tag_id = ft.tag_id WHERE ft.file_path = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tags.add(rs.getString("tag_name"));
                }
            }
        }
        return tags;
    }

    /**
     * Adds a new tag to the database.
     * 
     * @param tag The tag to add
     * @throws SQLException If an error occurs
     */
    public void addTag(Tag tag) throws SQLException {
        validateTag(tag.getName());
        
        // Check if the tag already exists
        boolean tagExists = false;
        String checkSql = "SELECT COUNT(*) FROM tags WHERE tag_name = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, tag.getName());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    tagExists = true;
                    System.out.println("Tag already exists: " + tag.getName());
                }
            }
        }
        
        // Save the current auto-commit state
        boolean originalAutoCommit = connection.getAutoCommit();
        
        try {
            // Disable auto-commit to start a transaction
            connection.setAutoCommit(false);
            
            if (tagExists) {
                // If it's a system tag, update its color to ensure consistency
                if (tag.isSystemTag()) {
                    String updateSql = "UPDATE tags SET color = ? WHERE tag_name = ?";
                    try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                        updateStmt.setString(1, tag.getColorHex());
                        updateStmt.setString(2, tag.getName());
                        int rowsUpdated = updateStmt.executeUpdate();
                        System.out.println("Updated system tag color: " + tag.getName() + ", rows affected: " + rowsUpdated);
                    }
                }
                // For non-system tags, we keep the existing color
            } else {
                // Insert the new tag
                String insertSql = "INSERT INTO tags (tag_name, color) VALUES (?, ?)";
                try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                    insertStmt.setString(1, tag.getName());
                    insertStmt.setString(2, tag.getColorHex());
                    int rowsInserted = insertStmt.executeUpdate();
                    System.out.println("Inserted new tag: " + tag.getName() + ", rows affected: " + rowsInserted);
                }
            }
            
            // Commit the transaction
            connection.commit();
            System.out.println("Successfully added/updated tag: " + tag.getName());
            
        } catch (SQLException e) {
            // If there's an error, roll back the transaction
            try {
                connection.rollback();
                System.err.println("Transaction rolled back due to error: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                System.err.println("Failed to roll back transaction: " + rollbackEx.getMessage());
            }
            throw e; // Re-throw the original exception
        } finally {
            // Restore the original auto-commit state
            try {
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException autoCommitEx) {
                System.err.println("Failed to restore auto-commit state: " + autoCommitEx.getMessage());
            }
        }
    }

    /**
     * Updates the color of an existing tag.
     * 
     * @param tag The tag with updated color
     * @throws SQLException If an error occurs
     */
    public void updateTagColor(Tag tag) throws SQLException {
        String sql = "UPDATE tags SET color = ? WHERE tag_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, tag.getColorHex());
            pstmt.setString(2, tag.getName());
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Gets all tags with their colors.
     * 
     * @return A map of tag names to Tag objects
     * @throws SQLException If an error occurs
     */
    public Map<String, Tag> getAllTagsWithColors() throws SQLException {
        Map<String, Tag> tags = new HashMap<>();
        String sql = "SELECT tag_name, color FROM tags";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("tag_name");
                String color = rs.getString("color");
                
                // If color is null, create a new tag which will generate a random color
                Tag tag = (color != null && !color.isEmpty()) 
                    ? new Tag(name, color) 
                    : new Tag(name);
                
                tags.put(name, tag);
            }
        }
        return tags;
    }

    public List<String> getAllTagsList() throws SQLException {
        List<String> tags = new ArrayList<>();
        String sql = "SELECT tag_name FROM tags";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(rs.getString("tag_name"));
            }
        }
        return tags;
    }
}
