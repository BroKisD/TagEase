# TagEase Installation Guide

This guide provides detailed instructions for installing and running TagEase on both Ubuntu Linux and Windows operating systems.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation Methods](#installation-methods)
  - [Method 1: Running from JAR](#method-1-running-from-jar)
  - [Method 2: Using Installers](#method-2-using-installers)
- [Ubuntu Installation](#ubuntu-installation)
  - [Building the DEB Package](#building-the-deb-package)
  - [Installing the DEB Package](#installing-the-deb-package)
- [Windows Installation](#windows-installation)
  - [Building the MSI Package](#building-the-msi-package)
  - [Installing the MSI Package](#installing-the-msi-package)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Before installing TagEase, ensure you have the following:

- **Java Runtime Environment (JRE) 17 or higher**
  - Ubuntu: `sudo apt install openjdk-17-jre`
  - Windows: Download and install from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [AdoptOpenJDK](https://adoptopenjdk.net/)

- **For building installers:**
  - JDK 17 or higher
  - Maven 3.6 or higher
  - jpackage tool (included with JDK 16+)

## Installation Methods

TagEase can be installed and run in two ways:

### Method 1: Running from JAR

This is the simplest method and works on any platform with Java installed.

1. Download the `TagEase-1.0.jar` file
2. Run it using the provided scripts:
   - Ubuntu: `./run-tagease.sh`
   - Windows: `run-tagease.bat`

The application will create a SQLite database (`tagease.db`) in the directory where it's run.

### Method 2: Using Installers

For a more integrated experience, you can build and install platform-specific packages.

## Ubuntu Installation

### Building the DEB Package

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/TagEase.git
   cd TagEase
   ```

2. Build the application:
   ```bash
   mvn clean package
   ```

3. Create the JavaFX runtime image:
   ```bash
   mvn javafx:jlink
   ```

4. Build the DEB installer:
   ```bash
   chmod +x build-installer.sh
   ./build-installer.sh
   ```

   This creates a DEB package in the `installer` directory.

### Installing the DEB Package

1. Install the DEB package:
   ```bash
   sudo dpkg -i installer/tagease_1.0-1_amd64.deb
   ```

2. If there are dependency issues:
   ```bash
   sudo apt-get install -f
   ```

3. Launch TagEase from your applications menu or run:
   ```bash
   tagease
   ```

## Windows Installation

### Building the MSI Package

**Note:** The Windows installer must be built on a Windows system. The `build-installer-win.sh` script is provided as a reference but cannot be run directly on Ubuntu.

1. On a Windows system with JDK 17+ and Maven installed:

2. Clone the repository:
   ```cmd
   git clone https://github.com/yourusername/TagEase.git
   cd TagEase
   ```

3. Build the application:
   ```cmd
   mvn clean package
   ```

4. Create the JavaFX runtime image:
   ```cmd
   mvn javafx:jlink
   ```

5. Build the MSI installer (using PowerShell or Command Prompt):
   ```cmd
   jpackage --input target\image\lib --name TagEase --main-jar TagEase-1.0.jar --main-class com.tagease.App --type msi --icon src\main\resources\Image\hash_12080727.png --app-version 1.0 --vendor "TagEase" --description "A file tagging application" --dest installer --runtime-image target\image --java-options "-Xmx512m"
   ```

### Installing the MSI Package

1. Double-click the MSI file in the `installer` directory
2. Follow the installation wizard
3. Launch TagEase from the Start Menu

## Alternative Installation for Windows

If you don't have access to a Windows system to build the installer:

1. Copy the `TagEase-1.0.jar` file to your Windows system
2. Create a batch file named `TagEase.bat` with the following content:
   ```batch
   @echo off
   start javaw -jar TagEase-1.0.jar
   ```
3. Place both files in the same directory
4. Double-click `TagEase.bat` to run the application

## Troubleshooting

### Java Version Issues

If you encounter errors like `UnsupportedClassVersionError`, ensure you have Java 17 or higher:

```bash
java -version
```

### Database Errors

If you see database-related errors:

1. Ensure the application has write permissions to the directory
2. If upgrading from a previous version, back up your `tagease.db` file

### Missing JavaFX Components

If you see errors about missing JavaFX components:

1. Ensure you're using the provided scripts to run the application
2. If building from source, verify that the Maven build includes JavaFX dependencies

### Linux Display Issues

If the application UI appears corrupted on Linux:

1. Try running with the following option:
   ```bash
   java -Dprism.order=sw -jar TagEase-1.0.jar
   ```

### Windows Firewall Alerts

If Windows Firewall shows an alert when running TagEase:

1. This is normal for Java applications
2. You can safely allow access (TagEase does not require network access for basic functionality)
