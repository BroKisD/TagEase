#!/bin/bash

# This script creates a Linux DEB installer for TagEase

# Ensure we have the JAR file
if [ ! -f "target/TagEase-1.0.jar" ]; then
  echo "Error: TagEase-1.0.jar not found. Run 'mvn clean package' first."
  exit 1
fi

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
  --java-options "-Xmx512m"

echo "Installer created in the 'installer' directory"
