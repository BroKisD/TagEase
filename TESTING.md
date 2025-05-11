# TagEase Testing Guide

This document provides instructions for running and extending the automated test suite for the TagEase application.

## Table of Contents

- [Overview](#overview)
- [Test Structure](#test-structure)
- [Running Tests](#running-tests)
- [Writing New Tests](#writing-new-tests)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

## Overview

TagEase uses JUnit 5, Mockito, and TestFX for testing. The test suite includes:

- **Unit Tests**: Test individual components in isolation
- **Integration Tests**: Test interactions between components
- **Performance Tests**: Measure application performance metrics

## Test Structure

The test suite is organized as follows:

```
src/test/java/com/tagease/
├── controller/
│   └── TagControllerTest.java       # Tests for TagController
├── database/
│   └── TaggedFileDAOIntegrationTest.java  # Database integration tests
├── model/
│   ├── TaggedFileTest.java          # Tests for TaggedFile model
│   └── TagTest.java                 # Tests for Tag model
└── performance/
    └── StartupPerformanceTest.java  # Application startup performance tests
```

## Running Tests

### Prerequisites

- Java 11 or higher
- Maven 3.6 or higher

### Running All Tests

```bash
mvn test
```

### Running Specific Test Classes

```bash
# Run a specific test class
mvn test -Dtest=TagControllerTest

# Run multiple test classes
mvn test -Dtest=TagControllerTest,TaggedFileTest
```

### Running Specific Test Methods

```bash
# Run a specific test method
mvn test -Dtest=TagControllerTest#testAddFile
```

### Running Tests by Category

```bash
# Run all model tests
mvn test -Dtest=com.tagease.model.*Test

# Run all integration tests
mvn test -Dtest=*IntegrationTest
```

## Writing New Tests

### Best Practices

1. **Follow the AAA Pattern**:
   - **Arrange**: Set up test data and preconditions
   - **Act**: Perform the action being tested
   - **Assert**: Verify the expected outcome

2. **Isolate Tests**:
   - Use mocking for external dependencies
   - Each test should be independent and not rely on other tests

3. **Meaningful Names**:
   - Test names should describe what is being tested
   - Use format: `testMethodName_StateUnderTest_ExpectedBehavior`

### Example Unit Test

```java
@Test
public void testAddTag_NewTag_TagAdded() {
    // Arrange
    TaggedFile file = new TaggedFile("test.txt", "/path/to/test.txt");
    
    // Act
    file.addTag("important");
    
    // Assert
    assertTrue(file.getTags().contains("important"));
}
```

### Example Integration Test with Mocking

```java
@Test
public void testAddFile_ValidFile_FileAddedToDatabase() throws SQLException {
    // Arrange
    TaggedFile file = new TaggedFile("test.txt", "/path/to/test.txt");
    file.addTag("important");
    Set<String> existingTags = new HashSet<>();
    
    // Act
    controller.addFile(file, existingTags);
    
    // Assert
    verify(mockFileDAO).addFile(file, existingTags);
}
```

## CI/CD Integration

The test suite is designed to be integrated with CI/CD pipelines. Here's how to configure it:

### GitHub Actions Example

Create a file `.github/workflows/test.yml`:

```yaml
name: Java Tests

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v2
    
    - name: Set up JDK 11
      uses: actions/setup-java@v2
      with:
        java-version: '11'
        distribution: 'adopt'
        
    - name: Build with Maven
      run: mvn -B package --file pom.xml
      
    - name: Test with Maven
      run: mvn test
      
    - name: Publish Test Report
      uses: mikepenz/action-junit-report@v2
      if: always()
      with:
        report_paths: '**/target/surefire-reports/TEST-*.xml'
```

## Performance Testing

### Database Test Data Generator

TagEase includes a utility for generating test data to evaluate application performance with large datasets:

```
src/main/java/com/tagease/utils/DatabaseTestDataGenerator.java
```

This utility creates simulated files with random properties:
- Random file names with various extensions (.txt, .pdf, .doc, .jpg, etc.)
- Random file paths with subdirectories
- Random tags (including system tags and custom tags)
- Randomized creation and last accessed dates

#### Running the Test Data Generator

A convenience script is provided to run the generator:

```bash
./generate-test-data.sh
```

By default, this generates 100 test files. You can specify a different number as a parameter:

```bash
./generate-test-data.sh 500  # Generates 500 files
```

#### Performance Metrics

The generator reports the time taken to add files to the database, which can be used to:

1. Measure database insertion performance
2. Test application responsiveness with large datasets
3. Evaluate sorting and filtering performance with many files
4. Identify potential bottlenecks in the UI rendering with extensive data

#### Example Results

On a standard development machine, the generator can add 1000 files in approximately 3-4 seconds, demonstrating the efficiency of the SQLite database implementation.

## Troubleshooting

### Common Issues

#### JavaFX Initialization Error

If you encounter `java.lang.ExceptionInInitializerError` with `Caused by: java.lang.IllegalStateException: Toolkit not initialized`, you need to:

1. Override the `showErrorDialog` method in your test to avoid JavaFX initialization:

```java
@Override
protected void showErrorDialog(String title, String header, String content) {
    // Skip showing dialog in tests
    System.out.println("Error dialog would show: " + title + " - " + content);
    
    // For tests that expect exceptions, throw the appropriate exception
    if (content.contains("Tag can only contain")) {
        throw new RuntimeException("Invalid tag: " + content);
    }
}
```

#### Database Connection Issues

For database tests, use an in-memory database:

```java
@BeforeEach
public void setUp() throws SQLException {
    // Use in-memory SQLite database for testing
    connection = DriverManager.getConnection("jdbc:sqlite::memory:");
    
    // Create tables
    try (Statement stmt = connection.createStatement()) {
        stmt.execute("CREATE TABLE IF NOT EXISTS files (...)");
        stmt.execute("CREATE TABLE IF NOT EXISTS tags (...)");
    }
    
    dao = new TaggedFileDAO(connection);
}
```

### Getting Help

If you encounter issues not covered here, please:

1. Check the JUnit 5 documentation: https://junit.org/junit5/docs/current/user-guide/
2. Check the Mockito documentation: https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html
3. Open an issue in the TagEase repository with details about your problem
