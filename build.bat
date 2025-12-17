@echo off
echo ========================================
echo   DocScan Lite - Build Script
echo ========================================
echo.

cd /d "%~dp0"

echo Building Debug APK...
echo.

call gradlew.bat assembleDebug -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESSFUL
    echo ========================================
    echo APK location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo ========================================
    echo   BUILD FAILED
    echo ========================================
)

echo.
pause
