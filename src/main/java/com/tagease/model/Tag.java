package com.tagease.model;

import javafx.scene.paint.Color;

/**
 * Represents a tag with a name and color.
 */
public class Tag {
    private String name;
    private String colorHex;
    
    // Default system tags
    public static final String TAG_DONE = "Done";
    public static final String TAG_IN_PROGRESS = "In Progress";
    public static final String TAG_NEW = "New";
    public static final String TAG_MISSING = "Missing";
    
    // Default colors for system tags
    public static final String COLOR_DONE = "#4CAF50";      // Green
    public static final String COLOR_IN_PROGRESS = "#FFC107"; // Yellow
    public static final String COLOR_NEW = "#2196F3";       // Blue
    public static final String COLOR_MISSING = "#F44336";   // Red
    
    /**
     * Creates a new tag with the given name and a random color.
     * 
     * @param name The name of the tag
     */
    public Tag(String name) {
        this.name = name;
        
        // Assign default colors for system tags, or generate a random color
        if (TAG_DONE.equals(name)) {
            this.colorHex = COLOR_DONE;
        } else if (TAG_IN_PROGRESS.equals(name)) {
            this.colorHex = COLOR_IN_PROGRESS;
        } else if (TAG_NEW.equals(name)) {
            this.colorHex = COLOR_NEW;
        } else if (TAG_MISSING.equals(name)) {
            this.colorHex = COLOR_MISSING;
        } else {
            this.colorHex = generateRandomColor();
        }
    }
    
    /**
     * Creates a new tag with the given name and color.
     * 
     * @param name The name of the tag
     * @param colorHex The hex color code for the tag
     */
    public Tag(String name, String colorHex) {
        this.name = name;
        this.colorHex = colorHex;
    }
    
    /**
     * Generates a random color for a tag.
     * 
     * @return A hex color string
     */
    private String generateRandomColor() {
        // Generate a random pastel color
        double hue = Math.random();
        double saturation = 0.5 + Math.random() * 0.3; // 0.5-0.8 for pastel colors
        double brightness = 0.7 + Math.random() * 0.2; // 0.7-0.9 for pastel colors
        
        Color color = Color.hsb(hue * 360, saturation, brightness);
        
        // Convert to hex format
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
    
    /**
     * Gets the name of the tag.
     * 
     * @return The tag name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the name of the tag.
     * 
     * @param name The new tag name
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Gets the hex color code of the tag.
     * 
     * @return The hex color code
     */
    public String getColorHex() {
        return colorHex;
    }
    
    /**
     * Sets the hex color code of the tag.
     * 
     * @param colorHex The new hex color code
     */
    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
    
    /**
     * Gets the JavaFX Color object for this tag.
     * 
     * @return The Color object
     */
    public Color getColor() {
        return Color.web(colorHex);
    }
    
    /**
     * Checks if this tag is a system tag that cannot be deleted.
     * 
     * @return true if this is a system tag, false otherwise
     */
    public boolean isSystemTag() {
        return TAG_DONE.equals(name) || 
               TAG_IN_PROGRESS.equals(name) || 
               TAG_NEW.equals(name) || 
               TAG_MISSING.equals(name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name.equals(tag.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return name;
    }
}
