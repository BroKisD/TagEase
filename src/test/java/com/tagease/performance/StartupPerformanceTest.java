package com.tagease.performance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;

import com.tagease.controller.TagController;
import com.tagease.database.DatabaseConfig;
import com.tagease.database.TaggedFileDAO;

/**
 * Performance test to measure the initialization time of key TagEase components.
 */
public class StartupPerformanceTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    public void setUpStreams() {
        System.setOut(new PrintStream(outContent));
    }
    
    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }
    
    /**
     * Measures the performance of initializing the database connection and DAO.
     */
    @Test
    public void testDatabaseInitializationPerformance() throws SQLException {
        // Print the start time
        long startTime = System.currentTimeMillis();
        System.out.println("Starting database initialization at: " + startTime);
        
        // Initialize the database connection
        long dbStartTime = System.currentTimeMillis();
        Connection connection = DatabaseConfig.getConnection();
        long dbEndTime = System.currentTimeMillis();
        
        // Initialize the DAO
        long daoStartTime = System.currentTimeMillis();
        TaggedFileDAO dao = new TaggedFileDAO(connection);
        long daoEndTime = System.currentTimeMillis();
        
        // Close resources
        DatabaseConfig.closeConnection();
        
        // Calculate and print the performance metrics
        long dbInitTime = dbEndTime - dbStartTime;
        long daoInitTime = daoEndTime - daoStartTime;
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.setOut(originalOut); // Restore original output for the report
        
        System.out.println("=== TagEase Database Performance Report ===");
        System.out.println("Database connection time: " + dbInitTime + " ms");
        System.out.println("DAO initialization time: " + daoInitTime + " ms");
        System.out.println("Total initialization time: " + totalTime + " ms");
        
        // Provide recommendations based on the results
        System.out.println("\nPerformance Recommendations:");
        if (dbInitTime > 300) {
            System.out.println("- Database connection is taking more than 300ms. Consider optimizing database access.");
        }
        
        if (daoInitTime > 100) {
            System.out.println("- DAO initialization is taking more than 100ms. Consider optimizing table creation.");
        }
    }
    
    /**
     * Measures the performance of controller operations like retrieving files and tags.
     */
    @Test
    public void testControllerOperationsPerformance() throws SQLException {
        // Initialize controller
        TagController controller = new TagController();
        
        // Measure getAllFiles performance
        long filesStartTime = System.currentTimeMillis();
        controller.getAllFiles();
        long filesEndTime = System.currentTimeMillis();
        
        // Measure getAllTags performance
        long tagsStartTime = System.currentTimeMillis();
        controller.getAllTags();
        long tagsEndTime = System.currentTimeMillis();
        
        // Measure checkForMissingFiles performance
        long checkStartTime = System.currentTimeMillis();
        controller.checkForMissingFiles();
        long checkEndTime = System.currentTimeMillis();
        
        // Close resources
        controller.close();
        
        // Calculate and print the performance metrics
        long filesTime = filesEndTime - filesStartTime;
        long tagsTime = tagsEndTime - tagsStartTime;
        long checkTime = checkEndTime - checkStartTime;
        
        System.out.println("=== TagEase Controller Operations Performance Report ===");
        System.out.println("Get all files time: " + filesTime + " ms");
        System.out.println("Get all tags time: " + tagsTime + " ms");
        System.out.println("Check for missing files time: " + checkTime + " ms");
        
        // Provide recommendations based on the results
        System.out.println("\nPerformance Recommendations:");
        if (filesTime > 200) {
            System.out.println("- Getting all files is taking more than 200ms. Consider optimizing the query or adding indexes.");
        }
        
        if (tagsTime > 100) {
            System.out.println("- Getting all tags is taking more than 100ms. Consider caching frequently used tags.");
        }
        
        if (checkTime > 500) {
            System.out.println("- Checking for missing files is taking more than 500ms. Consider running this check in a background thread.");
        }
    }
}
