@echo off
echo ========================================
echo   DocScan Lite - clean Script
echo ========================================
echo.

cd /d "%~dp0"
echo.

call gradlew.bat clean -Dorg.gradle.java.home="C:/Program Files/Microsoft/jdk-21.0.7.6-hotspot"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   CLEAN SUCCESSFUL
    echo ========================================