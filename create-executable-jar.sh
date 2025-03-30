#!/bin/bash

# This script creates a self-contained executable JAR for TagEase
# that includes all dependencies including JavaFX

echo "Building TagEase executable JAR..."
mvn clean package

# Copy the JAR file with a more descriptive name
cp target/demo-1.0-SNAPSHOT.jar target/TagEase-1.0.jar

echo "Creating run scripts..."

# Create a Windows batch file to run the application
cat > TagEase-1.0.bat << 'EOF'
@echo off
echo Starting TagEase...
java -jar TagEase-1.0.jar
pause
EOF

# Create a Linux/Mac shell script to run the application
cat > TagEase-1.0.sh << 'EOF'
#!/bin/bash
echo "Starting TagEase..."
java -jar TagEase-1.0.jar
EOF
chmod +x TagEase-1.0.sh

# Create a ZIP file with everything needed
echo "Creating distribution ZIP file..."
mkdir -p dist
cp target/TagEase-1.0.jar dist/
cp TagEase-1.0.bat dist/
cp TagEase-1.0.sh dist/
cp README.md dist/

# Create the ZIP file
cd dist
zip -r ../TagEase-1.0.zip *
cd ..

echo "Done! Distribution package created at: TagEase-1.0.zip"
echo "Users can extract this ZIP file and run TagEase-1.0.bat (Windows) or TagEase-1.0.sh (Linux/Mac)"
