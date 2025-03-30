#!/bin/bash

# This script creates a Linux DEB installer for TagEase

# First, build the application with Maven
echo "Building TagEase with Maven..."
mvn clean package

# Copy the JAR file with a more descriptive name
cp target/demo-1.0-SNAPSHOT.jar target/TagEase-1.0.jar

# Create a directory for the installer files
mkdir -p installer

# Make the run script executable
chmod +x run-tagease.sh

# Create a Linux installer (DEB package)
echo "Creating Linux DEB installer..."
jpackage \
  --input target \
  --name TagEase \
  --main-jar TagEase-1.0.jar \
  --main-class com.tagease.App \
  --type deb \
  --icon src/main/resources/Image/hash_12080727.png \
  --app-version 1.0 \
  --vendor "TagEase" \
  --description "A file tagging application" \
  --dest installer \
  --java-options "-Xmx512m" \
  --add-modules javafx.controls,javafx.fxml,javafx.graphics

echo "Installer created in the 'installer' directory"
