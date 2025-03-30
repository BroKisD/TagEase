package com.tagease.model;

import org.junit.jupiter.api.Test;
import javafx.scene.paint.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Tag class
 */
public class TagTest {
    
    @Test
    public void testConstructorWithNameOnly() {
        // Arrange & Act
        Tag tag = new Tag("CustomTag");
        
        // Assert
        assertEquals("CustomTag", tag.getName());
        assertNotNull(tag.getColorHex());
        assertTrue(tag.getColorHex().startsWith("#"));
        assertEquals(7, tag.getColorHex().length()); // #RRGGBB format
    }
    
    @Test
    public void testConstructorWithNameAndColor() {
        // Arrange & Act
        Tag tag = new Tag("CustomTag", "#FF5733");
        
        // Assert
        assertEquals("CustomTag", tag.getName());
        assertEquals("#FF5733", tag.getColorHex());
    }
    
    @Test
    public void testSystemTagsGetDefaultColors() {
        // Arrange & Act
        Tag doneTag = new Tag(Tag.TAG_DONE);
        Tag inProgressTag = new Tag(Tag.TAG_IN_PROGRESS);
        Tag newTag = new Tag(Tag.TAG_NEW);
        Tag missingTag = new Tag(Tag.TAG_MISSING);
        
        // Assert
        assertEquals(Tag.COLOR_DONE, doneTag.getColorHex());
        assertEquals(Tag.COLOR_IN_PROGRESS, inProgressTag.getColorHex());
        assertEquals(Tag.COLOR_NEW, newTag.getColorHex());
        assertEquals(Tag.COLOR_MISSING, missingTag.getColorHex());
    }
    
    @Test
    public void testIsSystemTag() {
        // Arrange & Act
        Tag doneTag = new Tag(Tag.TAG_DONE);
        Tag customTag = new Tag("CustomTag");
        
        // Assert
        assertTrue(doneTag.isSystemTag());
        assertFalse(customTag.isSystemTag());
    }
    
    @Test
    public void testGetColor() {
        // Arrange
        Tag tag = new Tag("CustomTag", "#FF5733");
        
        // Act
        Color color = tag.getColor();
        
        // Assert
        assertNotNull(color);
        assertEquals(1.0, color.getRed(), 0.01);
        assertEquals(0.34, color.getGreen(), 0.01);
        assertEquals(0.2, color.getBlue(), 0.01);
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // Arrange
        Tag tag1 = new Tag("SameTag", "#FF5733");
        Tag tag2 = new Tag("SameTag", "#00FF00"); // Different color, same name
        Tag tag3 = new Tag("DifferentTag", "#FF5733"); // Same color, different name
        
        // Act & Assert
        assertEquals(tag1, tag2, "Tags with same name should be equal regardless of color");
        assertNotEquals(tag1, tag3, "Tags with different names should not be equal");
        assertEquals(tag1.hashCode(), tag2.hashCode(), "Hash codes should be equal for equal objects");
    }
}
