package com.tagease.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the TaggedFile class
 */
public class TaggedFileTest {
    
    private TaggedFile taggedFile;
    private final String testFileName = "test.txt";
    private final String testFilePath = "/path/to/test.txt";
    
    @BeforeEach
    public void setUp() {
        taggedFile = new TaggedFile(testFileName, testFilePath);
    }
    
    @Test
    public void testConstructor() {
        // Arrange & Act - done in setUp()
        
        // Assert
        assertEquals(testFileName, taggedFile.getFileName());
        assertEquals(testFilePath, taggedFile.getFilePath());
        assertNotNull(taggedFile.getTags());
        assertTrue(taggedFile.getTags().isEmpty());
        assertNotNull(taggedFile.getCreatedAt());
        assertNotNull(taggedFile.getLastAccessedAt());
        assertNotNull(taggedFile.getRelatedFiles());
        assertTrue(taggedFile.getRelatedFiles().isEmpty());
    }
    
    @Test
    public void testConstructorWithTags() {
        // Arrange
        Set<String> tags = new HashSet<>();
        tags.add("tag1");
        tags.add("tag2");
        
        // Act
        TaggedFile fileWithTags = new TaggedFile(testFileName, testFilePath, tags);
        
        // Assert
        assertEquals(testFileName, fileWithTags.getFileName());
        assertEquals(testFilePath, fileWithTags.getFilePath());
        assertEquals(2, fileWithTags.getTags().size());
        assertTrue(fileWithTags.getTags().contains("tag1"));
        assertTrue(fileWithTags.getTags().contains("tag2"));
    }
    
    @Test
    public void testAddTag() {
        // Arrange - done in setUp()
        
        // Act
        taggedFile.addTag("important");
        
        // Assert
        assertEquals(1, taggedFile.getTags().size());
        assertTrue(taggedFile.getTags().contains("important"));
    }
    
    @Test
    public void testRemoveTag() {
        // Arrange
        taggedFile.addTag("important");
        taggedFile.addTag("urgent");
        
        // Act
        taggedFile.removeTag("important");
        
        // Assert
        assertEquals(1, taggedFile.getTags().size());
        assertFalse(taggedFile.getTags().contains("important"));
        assertTrue(taggedFile.getTags().contains("urgent"));
    }
    
    @Test
    public void testUpdateLastAccessed() {
        // Arrange
        LocalDateTime beforeUpdate = taggedFile.getLastAccessedAt();
        
        // Wait a small amount of time to ensure timestamp changes
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Act
        taggedFile.updateLastAccessed();
        
        // Assert
        LocalDateTime afterUpdate = taggedFile.getLastAccessedAt();
        assertTrue(afterUpdate.isAfter(beforeUpdate), "Last accessed time should be updated");
    }
    
    @Test
    public void testAddRelatedFile() {
        // Arrange
        TaggedFile relatedFile = new TaggedFile("related.txt", "/path/to/related.txt");
        
        // Act
        taggedFile.addRelatedFile(relatedFile);
        
        // Assert
        assertEquals(1, taggedFile.getRelatedFiles().size());
        assertTrue(taggedFile.getRelatedFiles().contains(relatedFile));
    }
    
    @Test
    public void testRemoveRelatedFile() {
        // Arrange
        TaggedFile relatedFile = new TaggedFile("related.txt", "/path/to/related.txt");
        taggedFile.addRelatedFile(relatedFile);
        
        // Act
        taggedFile.removeRelatedFile(relatedFile);
        
        // Assert
        assertTrue(taggedFile.getRelatedFiles().isEmpty());
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // Arrange
        TaggedFile samePathFile = new TaggedFile("different.txt", testFilePath);
        TaggedFile differentPathFile = new TaggedFile(testFileName, "/different/path.txt");
        
        // Act & Assert
        assertEquals(taggedFile, samePathFile, "Files with same path should be equal");
        assertNotEquals(taggedFile, differentPathFile, "Files with different paths should not be equal");
        assertEquals(taggedFile.hashCode(), samePathFile.hashCode(), "Hash codes should be equal for equal objects");
    }
}
