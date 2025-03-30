# TagEase

TagEase is a file tagging application that helps you organize and find your files using a tag-based system.

## Features

- Tag files with custom colored tags
- Filter files by tags
- Sort files by name, creation date, or last accessed date
- Automatically detect missing files
- System tags (Done, In Progress, New, Missing) with predefined colors
- Modern, clean UI design

## Requirements

- Java 17 or higher

## Installation

### Option 1: Run the JAR file directly

1. Download `TagEase-1.0.jar`
2. Open a terminal or command prompt
3. Run the command: `java -jar TagEase-1.0.jar`

### Option 2: Use the platform-specific installer (if available)

1. Download the installer for your operating system
2. Run the installer and follow the on-screen instructions
3. Launch TagEase from your applications menu or desktop shortcut

## Usage

1. **Add Files**: Click the "Add File" button to add files to TagEase
2. **Tag Files**: Select a file and click "Edit Tags" to add or remove tags
3. **Filter Files**: Use the tag dropdown to filter files by tags
4. **Sort Files**: Use the sort dropdown to sort files by name, creation date, or last access date
5. **Open Files**: Click the folder icon to open a file in its default application

## Database Location

TagEase stores its database in a file named `tagease.db` in the same directory where the application is run. This file contains all your tags and file references.

## Support

If you encounter any issues or have questions, please create an issue on the GitHub repository or contact the developer.

## Testing

TagEase includes a comprehensive automated test suite built with JUnit 5, Mockito, and TestFX. The test suite covers:

- **Unit Tests**: Testing individual components in isolation
- **Integration Tests**: Testing interactions between components
- **Performance Tests**: Measuring application performance metrics

For detailed instructions on running and extending the test suite, see [TESTING.md](TESTING.md).

### Running Tests

To run all tests:
```bash
mvn test
```

To run a specific test class:
```bash
mvn test -Dtest=TagControllerTest
```
