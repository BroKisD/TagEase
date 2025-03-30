#!/bin/bash

# This script creates platform-specific installers for TagEase

# Ensure we have the JAR file
if [ ! -f "target/TagEase-1.0.jar" ]; then
  echo "Error: TagEase-1.0.jar not found. Run 'mvn clean package' first."
  exit 1
fi

# Create a directory for the installer files
mkdir -p installer

# Make the run script executable
chmod +x run-tagease.sh

# Create a Windows installer (MSI package)
echo "Creating Windows MSI installer..."
jpackage \
  --input target \
  --name TagEase \
  --main-jar TagEase-1.0.jar \
  --main-class com.tagease.App \
  --type msi \
  --icon src/main/resources/Image/hash_12080727.png \
  --app-version 1.0 \
  --vendor "TagEase" \
  --description "A file tagging application" \
  --dest installer \
  --java-options "-Xmx512m" \
  --win-dir-chooser \
  --win-shortcut \
  --win-menu

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
  --java-options "-Xmx512m"

# Create a Linux installer (RPM package)
echo "Creating Linux RPM installer..."
jpackage \
  --input target \
  --name TagEase \
  --main-jar TagEase-1.0.jar \
  --main-class com.tagease.App \
  --type rpm \
  --icon src/main/resources/Image/hash_12080727.png \
  --app-version 1.0 \
  --vendor "TagEase" \
  --description "A file tagging application" \
  --dest installer \
  --java-options "-Xmx512m"

echo "Installers created in the 'installer' directory"
