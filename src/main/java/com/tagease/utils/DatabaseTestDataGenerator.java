package com.tagease.utils;

import com.tagease.database.DatabaseConfig;
import com.tagease.database.TaggedFileDAO;
import com.tagease.model.Tag;
import com.tagease.model.TaggedFile;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class to generate test data for the TagEase database.
 * This class adds a specified number of simulated files with random tags to the database
 * for performance testing purposes.
 */
public class DatabaseTestDataGenerator {
    private static final String[] SAMPLE_EXTENSIONS = {".txt", ".pdf", ".doc", ".jpg", ".png", ".mp3", ".mp4", ".java", ".html", ".css"};
    private static final String[] SAMPLE_TAG_PREFIXES = {"project", "work", "personal", "important", "urgent", "review", "archive", "temp", "shared", "backup"};
    private static final String[] SAMPLE_TAG_SUFFIXES = {"2023", "2024", "2025", "high", "medium", "low", "draft", "final", "v1", "v2"};
    private static final String[] SYSTEM_TAGS = {Tag.TAG_DONE, Tag.TAG_IN_PROGRESS, Tag.TAG_NEW};
    
    private static final String BASE_PATH = System.getProperty("user.home") + File.separator + "TagEase_TestFiles";
    private static final int MAX_TAGS_PER_FILE = 5;
    private static final int NUM_POSSIBLE_TAGS = 30;
    
    private final TaggedFileDAO fileDAO;
    private final List<String> generatedTags;
    private final Random random;
    private final Set<String> existingTags;
    private static final int MAX_TAG_LENGTH = 50;
    private static final String TAG_REGEX = "^[a-zA-Z0-9 _-]+$";
    
    /**
     * Creates a new DatabaseTestDataGenerator.
     */
    public DatabaseTestDataGenerator() {
        try {
            // Initialize database connection directly
            this.fileDAO = new TaggedFileDAO(DatabaseConfig.getConnection());
            this.generatedTags = generatePossibleTags();
            this.random = new Random();
            this.existingTags = new HashSet<>(fileDAO.getAllTags());
            System.out.println("Database connection established successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
    
    /**
     * Generates a list of possible tags to use for test files.
     * 
     * @return A list of tag names
     */
    private List<String> generatePossibleTags() {
        List<String> tags = new ArrayList<>();
        
        // Add system tags
        tags.addAll(Arrays.asList(SYSTEM_TAGS));
        
        // Generate combinations of prefixes and suffixes
        for (String prefix : SAMPLE_TAG_PREFIXES) {
            tags.add(prefix);
            for (String suffix : SAMPLE_TAG_SUFFIXES) {
                if (tags.size() < NUM_POSSIBLE_TAGS) {
                    tags.add(prefix + "-" + suffix);
                }
            }
        }
        
        // Add some standalone suffixes if needed
        for (String suffix : SAMPLE_TAG_SUFFIXES) {
            if (tags.size() < NUM_POSSIBLE_TAGS) {
                tags.add(suffix);
            }
        }
        
        return tags;
    }
    
    /**
     * Generates a random file name with a random extension.
     * 
     * @return A random file name
     */
    private String generateFileName() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", random.nextInt(10000));
        String extension = SAMPLE_EXTENSIONS[random.nextInt(SAMPLE_EXTENSIONS.length)];
        return "test_file_" + timestamp + "_" + randomPart + extension;
    }
    
    /**
     * Generates a random file path based on the file name.
     * 
     * @param fileName The file name
     * @return A random file path
     */
    private String generateFilePath(String fileName) {
        // Create subdirectories to make it more realistic
        int subDirLevel = random.nextInt(3);
        StringBuilder path = new StringBuilder(BASE_PATH);
        
        for (int i = 0; i < subDirLevel; i++) {
            path.append(File.separator).append("dir").append(random.nextInt(5));
        }
        
        return path.append(File.separator).append(fileName).toString();
    }
    
    /**
     * Generates a random set of tags for a file.
     * 
     * @return A set of random tags
     */
    private Set<String> generateRandomTags() {
        int numTags = random.nextInt(MAX_TAGS_PER_FILE) + 1; // At least 1 tag
        Set<String> tags = new HashSet<>();
        
        // Add random tags from our list
        while (tags.size() < numTags) {
            tags.add(generatedTags.get(random.nextInt(generatedTags.size())));
        }
        
        return tags;
    }
    
    /**
     * Generates a random creation date within the last year.
     * 
     * @return A random LocalDateTime
     */
    private LocalDateTime generateRandomCreationDate() {
        long minDay = LocalDateTime.now().minusYears(1).toLocalDate().toEpochDay();
        long maxDay = LocalDateTime.now().toLocalDate().toEpochDay();
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay);
        
        return LocalDateTime.of(
            LocalDate.ofEpochDay(randomDay),
            LocalDateTime.now().toLocalTime()
        );
    }
    
    /**
     * Generates a random last accessed date after the creation date.
     * 
     * @param createdAt The creation date
     * @return A random LocalDateTime after the creation date
     */
    private LocalDateTime generateRandomLastAccessedDate(LocalDateTime createdAt) {
        long minDay = createdAt.toLocalDate().toEpochDay();
        long maxDay = LocalDateTime.now().toLocalDate().toEpochDay();
        
        // If the file was created today, return the same time
        if (minDay >= maxDay) {
            return createdAt;
        }
        
        long randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay + 1);
        
        return LocalDateTime.of(
            LocalDate.ofEpochDay(randomDay),
            LocalDateTime.now().toLocalTime()
        );
    }
    
    /**
     * Validates a tag name.
     * 
     * @param tag The tag to validate
     * @throws IllegalArgumentException If the tag is invalid
     */
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
    
    /**
     * Creates a single test file with random properties.
     */
    private void createTestFile() {
        String fileName = generateFileName();
        String filePath = generateFilePath(fileName);
        Set<String> tags = generateRandomTags();
        
        TaggedFile file = new TaggedFile(fileName, filePath, tags);
        
        // Set random dates
        LocalDateTime createdAt = generateRandomCreationDate();
        file.setCreatedAt(createdAt);
        file.setLastAccessedAt(generateRandomLastAccessedDate(createdAt));
        
        try {
            // Validate tags before adding
            for (String tag : tags) {
                if (!existingTags.contains(tag)) {
                    validateTag(tag);
                }
            }
            
            fileDAO.addFile(file, existingTags);
        } catch (Exception e) {
            System.err.println("Error adding test file: " + e.getMessage());
        }
    }
    
    /**
     * Generates the specified number of test files.
     * 
     * @param count The number of test files to generate
     */
    public void generateTestFiles(int count) {
        System.out.println("Starting to generate " + count + " test files...");
        long startTime = System.currentTimeMillis();
        
        try {
            // Add our generated tags to the database first
            for (String tag : generatedTags) {
                if (!existingTags.contains(tag)) {
                    try {
                        fileDAO.addTag(new Tag(tag));
                        existingTags.add(tag);
                    } catch (Exception e) {
                        System.err.println("Error adding tag '" + tag + "': " + e.getMessage());
                    }
                }
            }
            
            // Create the test files
            for (int i = 0; i < count; i++) {
                createTestFile();
                
                // Print progress every 100 files
                if ((i + 1) % 100 == 0 || i == count - 1) {
                    System.out.println("Generated " + (i + 1) + " of " + count + " files");
                }
            }
            
            long endTime = System.currentTimeMillis();
            System.out.println("Successfully generated " + count + " test files in " + (endTime - startTime) / 1000.0 + " seconds");
            
        } catch (Exception e) {
            System.err.println("Error generating test data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Close the database connection
                DatabaseConfig.closeConnection();
                System.out.println("Database connection closed");
            } catch (Exception e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Main method to run the test data generator.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        int count = 100; // Default number of files to generate
        
        // Allow specifying the count via command line
        if (args.length > 0) {
            try {
                count = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid count argument. Using default: " + count);
            }
        }
        
        DatabaseTestDataGenerator generator = new DatabaseTestDataGenerator();
        generator.generateTestFiles(count);
    }
}
