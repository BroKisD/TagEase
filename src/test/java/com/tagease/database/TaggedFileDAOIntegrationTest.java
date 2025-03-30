package com.tagease.database;

import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the TaggedFileDAO class.
 * These tests use a temporary in-memory SQLite database.
 */
public class TaggedFileDAOIntegrationTest {

    private Connection connection;
    private TaggedFileDAO dao;
    private static final String TEST_DB_URL = "jdbc:sqlite::memory:";

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws SQLException {
        // Create an in-memory database for testing
        connection = DriverManager.getConnection(TEST_DB_URL);
        connection.setAutoCommit(false);
        
        // Create the schema
        try (Statement stmt = connection.createStatement()) {
            // Enable foreign key support
            stmt.execute("PRAGMA foreign_keys = ON");

            // Create files table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS files (
                    file_path TEXT PRIMARY KEY,
                    file_name TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create tags table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tags (
                    tag_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tag_name TEXT UNIQUE NOT NULL,
                    color TEXT
                )
            """);

            // Create file_tags table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS file_tags (
                    file_path TEXT,
                    tag_id INTEGER,
                    PRIMARY KEY (file_path, tag_id),
                    FOREIGN KEY (file_path) REFERENCES files(file_path) ON DELETE CASCADE,
                    FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
                )
            """);

            // Create file_relationships table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS file_relationships (
                    source_file_path TEXT,
                    related_file_path TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (source_file_path, related_file_path),
                    FOREIGN KEY (source_file_path) REFERENCES files(file_path) ON DELETE CASCADE,
                    FOREIGN KEY (related_file_path) REFERENCES files(file_path) ON DELETE CASCADE
                )
            """);

            connection.commit();
        }
        
        // Create the DAO
        dao = new TaggedFileDAO(connection);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    public void testAddFile() throws SQLException {
        // Arrange
        File testFile = tempDir.resolve("test.txt").toFile();
        TaggedFile file = new TaggedFile(testFile.getName(), testFile.getAbsolutePath());
        file.addTag("important");
        
        // Act
        dao.addFile(file, new HashSet<>());
        
        // Assert
        List<TaggedFile> files = dao.getAllFiles();
        assertEquals(1, files.size());
        assertEquals(testFile.getName(), files.get(0).getFileName());
        assertEquals(testFile.getAbsolutePath(), files.get(0).getFilePath());
        assertTrue(files.get(0).getTags().contains("important"));
    }

    @Test
    public void testAddTag() throws SQLException {
        // Arrange
        Tag tag = new Tag("important", "#FF5733");
        
        // Act
        dao.addTag(tag);
        
        // Assert
        Set<String> tags = dao.getAllTags();
        assertTrue(tags.contains("important"));
        
        // Also check the color was stored
        Map<String, Tag> tagsWithColors = dao.getAllTagsWithColors();
        assertTrue(tagsWithColors.containsKey("important"));
        assertEquals("#FF5733", tagsWithColors.get("important").getColorHex());
    }

    @Test
    public void testGetFilesByTags() throws SQLException {
        // Arrange - Add two files with different tags
        File testFile1 = tempDir.resolve("test1.txt").toFile();
        TaggedFile file1 = new TaggedFile(testFile1.getName(), testFile1.getAbsolutePath());
        file1.addTag("important");
        file1.addTag("document");
        
        File testFile2 = tempDir.resolve("test2.txt").toFile();
        TaggedFile file2 = new TaggedFile(testFile2.getName(), testFile2.getAbsolutePath());
        file2.addTag("document");
        
        dao.addFile(file1, new HashSet<>());
        dao.addFile(file2, new HashSet<>());
        
        // Act - Filter by "important" tag
        Set<String> filterTags = new HashSet<>();
        filterTags.add("important");
        List<TaggedFile> filteredFiles = dao.getFilesByTags(filterTags);
        
        // Assert
        assertEquals(1, filteredFiles.size());
        assertEquals(testFile1.getAbsolutePath(), filteredFiles.get(0).getFilePath());
        
        // Act - Filter by "document" tag
        filterTags.clear();
        filterTags.add("document");
        filteredFiles = dao.getFilesByTags(filterTags);
        
        // Assert
        assertEquals(2, filteredFiles.size());
    }

    @Test
    public void testUpdateFileTags() throws SQLException {
        // Arrange - Add a file with an initial tag
        File testFile = tempDir.resolve("test.txt").toFile();
        TaggedFile file = new TaggedFile(testFile.getName(), testFile.getAbsolutePath());
        file.addTag("initial");
        dao.addFile(file, new HashSet<>());
        
        // Act - Update the file's tags
        file.getTags().clear();
        file.addTag("updated");
        dao.updateFileTags(file);
        
        // Assert
        List<TaggedFile> files = dao.getAllFiles();
        assertEquals(1, files.size());
        assertEquals(1, files.get(0).getTags().size());
        assertTrue(files.get(0).getTags().contains("updated"));
        assertFalse(files.get(0).getTags().contains("initial"));
    }

    @Test
    public void testDeleteTag() throws SQLException {
        // Arrange - Add a file with multiple tags
        File testFile = tempDir.resolve("test.txt").toFile();
        TaggedFile file = new TaggedFile(testFile.getName(), testFile.getAbsolutePath());
        file.addTag("tag1");
        file.addTag("tag2");
        dao.addFile(file, new HashSet<>());
        
        // Act - Delete one tag
        dao.deleteTag("tag1");
        
        // Assert
        Set<String> tags = dao.getAllTags();
        assertFalse(tags.contains("tag1"));
        assertTrue(tags.contains("tag2"));
        
        // Also check the file no longer has the deleted tag
        List<TaggedFile> files = dao.getAllFiles();
        assertEquals(1, files.size());
        assertFalse(files.get(0).getTags().contains("tag1"));
        assertTrue(files.get(0).getTags().contains("tag2"));
    }

    @Test
    public void testSystemTagsAreInitialized() throws SQLException {
        // Act - Simply creating the DAO should initialize system tags
        // (This was done in setUp)
        
        // Assert
        Set<String> tags = dao.getAllTags();
        assertTrue(tags.contains(Tag.TAG_DONE));
        assertTrue(tags.contains(Tag.TAG_IN_PROGRESS));
        assertTrue(tags.contains(Tag.TAG_NEW));
        assertTrue(tags.contains(Tag.TAG_MISSING));
        
        // Also check their colors
        Map<String, Tag> tagsWithColors = dao.getAllTagsWithColors();
        assertEquals(Tag.COLOR_DONE, tagsWithColors.get(Tag.TAG_DONE).getColorHex());
        assertEquals(Tag.COLOR_IN_PROGRESS, tagsWithColors.get(Tag.TAG_IN_PROGRESS).getColorHex());
        assertEquals(Tag.COLOR_NEW, tagsWithColors.get(Tag.TAG_NEW).getColorHex());
        assertEquals(Tag.COLOR_MISSING, tagsWithColors.get(Tag.TAG_MISSING).getColorHex());
    }
}
