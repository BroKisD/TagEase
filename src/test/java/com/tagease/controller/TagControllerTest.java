package com.tagease.controller;

import com.tagease.database.DatabaseConfig;
import com.tagease.database.TaggedFileDAO;
import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the TagController class
 */
@ExtendWith(MockitoExtension.class)
public class TagControllerTest {

    @Mock
    private Connection mockConnection;
    
    @Mock
    private TaggedFileDAO mockFileDAO;
    
    private TagController controller;
    
    @BeforeEach
    public void setUp() throws SQLException, NoSuchFieldException, IllegalAccessException {
        // Create a custom controller that doesn't actually initialize the database
        // and doesn't try to show JavaFX dialogs
        controller = new TagController() {
            @Override
            protected void initializeDatabase() {
                // Skip database initialization
                this.fileDAO = mockFileDAO;
            }
            
            @Override
            protected void showErrorDialog(String title, String header, String content) {
                // Skip showing dialog in tests to avoid JavaFX initialization issues
                System.out.println("Error dialog would show: " + title + " - " + header + " - " + content);
                
                // For invalid tag test, we need to throw the expected exception
                if (content.contains("Tag can only contain")) {
                    throw new RuntimeException("Invalid tag: " + content);
                }
            }
        };
    }
    
    @Test
    public void testAddFile() throws SQLException {
        // Arrange
        TaggedFile file = new TaggedFile("test.txt", "/path/to/test.txt");
        file.addTag("important");
        Set<String> existingTags = new HashSet<>();
        
        // Act
        controller.addFile(file, existingTags);
        
        // Assert
        verify(mockFileDAO).addFile(file, existingTags);
    }
    
    @Test
    public void testAddFileWithInvalidTag() {
        // Arrange
        TaggedFile file = new TaggedFile("test.txt", "/path/to/test.txt");
        file.addTag("invalid@tag"); // Contains invalid character
        Set<String> existingTags = new HashSet<>();
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            controller.addFile(file, existingTags);
        });
        
        assertTrue(exception.getMessage().contains("Invalid tag"));
    }
    
    @Test
    public void testUpdateFileTags() throws SQLException {
        // Arrange
        TaggedFile file = new TaggedFile("test.txt", "/path/to/test.txt");
        file.addTag("important");
        
        // Act
        controller.updateFileTags(file);
        
        // Assert
        verify(mockFileDAO).updateFileTags(file);
    }
    
    @Test
    public void testGetAllFiles() throws SQLException {
        // Arrange
        List<TaggedFile> expectedFiles = Arrays.asList(
            new TaggedFile("file1.txt", "/path/to/file1.txt"),
            new TaggedFile("file2.txt", "/path/to/file2.txt")
        );
        when(mockFileDAO.getAllFiles()).thenReturn(expectedFiles);
        
        // Act
        List<TaggedFile> actualFiles = controller.getAllFiles();
        
        // Assert
        assertEquals(expectedFiles, actualFiles);
        verify(mockFileDAO).getAllFiles();
    }
    
    @Test
    public void testGetAllTags() throws SQLException {
        // Arrange
        Set<String> expectedTags = new HashSet<>(Arrays.asList("important", "urgent"));
        when(mockFileDAO.getAllTags()).thenReturn(expectedTags);
        
        // Act
        Set<String> actualTags = controller.getAllTags();
        
        // Assert
        assertEquals(expectedTags, actualTags);
        verify(mockFileDAO).getAllTags();
    }
    
    @Test
    public void testGetFilesByTags() throws SQLException {
        // Arrange
        Set<String> tags = new HashSet<>(Arrays.asList("important"));
        List<TaggedFile> expectedFiles = Arrays.asList(
            new TaggedFile("file1.txt", "/path/to/file1.txt")
        );
        when(mockFileDAO.getFilesByTags(tags)).thenReturn(expectedFiles);
        
        // Act
        List<TaggedFile> actualFiles = controller.getFilesByTags(tags);
        
        // Assert
        assertEquals(expectedFiles, actualFiles);
        verify(mockFileDAO).getFilesByTags(tags);
    }
    
    @Test
    public void testCheckForMissingFiles() throws SQLException {
        // Arrange
        TaggedFile existingFile = new TaggedFile("exists.txt", System.getProperty("user.dir") + "/pom.xml");
        TaggedFile missingFile = new TaggedFile("missing.txt", "/path/to/nonexistent/file.txt");
        
        List<TaggedFile> files = Arrays.asList(existingFile, missingFile);
        when(mockFileDAO.getAllFiles()).thenReturn(files);
        
        // Act
        controller.checkForMissingFiles();
        
        // Assert - The missing file should have the Missing tag added
        verify(mockFileDAO, times(1)).updateFileTags(any(TaggedFile.class));
    }
}
