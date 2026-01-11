#!/bin/bash
# Quick script to build and install the app to your Android device

echo "=== Audio Recorder - Device Installation Script ==="
echo ""

# Check if device is connected
echo "Checking for connected devices..."
adb devices -l

if ! adb devices | grep -q "device$"; then
    echo ""
    echo "ERROR: No device detected!"
    echo "Please ensure:"
    echo "  1. Your phone is connected via USB"
    echo "  2. USB debugging is enabled"
    echo "  3. You authorized this computer on your phone"
    exit 1
fi

echo ""
echo "Building APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "Installing to device..."
    ./gradlew installDebug

    if [ $? -eq 0 ]; then
        echo ""
        echo "✓ App installed successfully!"
        echo ""
        echo "You can now launch 'Audio Recorder' from your phone's app drawer"
        echo ""
        echo "To launch immediately from command line, run:"
        echo "  adb shell am start -n com.audiorecorder/.MainActivity"
    else
        echo ""
        echo "ERROR: Installation failed"
        exit 1
    fi
else
    echo ""
    echo "ERROR: Build failed"
    exit 1
fi
