@echo off
echo Creating Windows installer for TagEase...

REM Ensure we have the JAR file
if not exist "target\TagEase-1.0.jar" (
  echo Error: TagEase-1.0.jar not found. Run 'mvn clean package' first.
  exit /b 1
)

REM Create a directory for the installer files
mkdir installer 2>nul

REM Create a Windows installer (MSI package)
echo Creating Windows MSI installer...
jpackage ^
  --input target ^
  --name TagEase ^
  --main-jar TagEase-1.0.jar ^
  --main-class com.tagease.App ^
  --type msi ^
  --icon src\main\resources\Image\hash_12080727.png ^
  --app-version 1.0 ^
  --vendor "TagEase" ^
  --description "A file tagging application" ^
  --dest installer ^
  --java-options "-Xmx512m" ^
  --win-dir-chooser ^
  --win-shortcut ^
  --win-menu

echo Installer created in the 'installer' directory
pause
