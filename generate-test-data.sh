#!/bin/bash

echo "Running TagEase Database Test Data Generator..."

# Get the directory where this script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "$SCRIPT_DIR"

# Check if a count argument was provided
COUNT=100
if [ "$#" -eq 1 ] && [[ "$1" =~ ^[0-9]+$ ]]; then
    COUNT=$1
fi

# Run the generator with Maven
mvn exec:java -Dexec.mainClass="com.tagease.utils.DatabaseTestDataGenerator" -Dexec.args="$COUNT"

echo "Test data generation complete!"
