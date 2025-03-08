package com.tagease.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String DB_NAME = "tagease.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;
    private static Connection connection = null;

    static {
        try {
            // Load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver: " + e.getMessage());
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Check if database file exists
                File dbFile = new File(DB_NAME);
                boolean needsInit = !dbFile.exists();
                
                // Create connection
                System.out.println("Attempting to connect to database at: " + DB_URL);
                connection = DriverManager.getConnection(DB_URL);
                connection.setAutoCommit(false);
                
                System.out.println("Database connection established successfully");
                
                if (needsInit) {
                    System.out.println("Initializing new database...");
                    initializeDatabase();
                } else {
                    initializeDatabase();
                }
            } catch (SQLException e) {
                System.err.println("Failed to establish database connection: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }

    private static void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Enable foreign key support
            stmt.execute("PRAGMA foreign_keys = ON");

            // Create files table with last_accessed_at column
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
                    tag_name TEXT UNIQUE NOT NULL
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

            // Add indexes for better performance
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_last_accessed ON files(last_accessed_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_files_created_at ON files(created_at)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(tag_name)");

            connection.commit();
            System.out.println("Database schema initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            connection.rollback();
            throw e;
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    if (!connection.getAutoCommit()) {
                        try {
                            connection.rollback();
                        } catch (SQLException e) {
                            System.err.println("Failed to rollback on connection close: " + e.getMessage());
                        }
                    }
                    connection.close();
                    System.out.println("Database connection closed successfully");
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            } finally {
                connection = null;
            }
        }
    }
}
