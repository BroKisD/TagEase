#!/bin/bash

# This script creates a Windows installer for TagEase

# Ensure we have the JAR file
if [ ! -f "target/TagEase-1.0.jar" ]; then
  echo "Error: TagEase-1.0.jar not found. Run 'mvn clean package' first."
  exit 1
fi

# Ensure we have the JavaFX runtime
if [ ! -d "target/image" ]; then
  echo "Error: JavaFX runtime image not found. Run 'mvn javafx:jlink' first."
  exit 1
fi

# Ensure the main jar is copied to the runtime image lib directory
if [ ! -f "target/image/lib/TagEase-1.0.jar" ]; then
  echo "Copying TagEase-1.0.jar to target/image/lib..."
  cp target/TagEase-1.0.jar target/image/lib/
fi

# Create a directory for the installer files
mkdir -p installer

# Create Windows MSI installer using jpackage
echo "Creating Windows MSI installer..."
jpackage \
  --input target/image/lib \
  --name TagEase \
  --main-jar TagEase-1.0.jar \
  --main-class com.tagease.App \
  --type msi \
  --icon src/main/resources/Image/hash_12080727.png \
  --app-version 1.0 \
  --vendor "TagEase" \
  --description "A file tagging application" \
  --dest installer \
  --runtime-image target/image \
  --java-options "-Xmx512m"

echo "Installer created in the 'installer' directory"
