@echo off
rem Set encoding to UTF-8
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================
echo  Web Build Tool
echo ===========================

rem Create logs directory if it doesn't exist
if not exist "%~dp0logs" mkdir "%~dp0logs"
set "LOG_FILE=%~dp0logs\build_history.log"

echo [%date% %time%] [INFO] Starting build process... >> "%LOG_FILE%"
echo [INFO] Starting Web Project Build...

cd /d "%~dp0.."

call mvn clean package -Dmaven.test.skip=true
set "EXIT_CODE=%errorlevel%"

if %EXIT_CODE% equ 0 (
    echo.
    echo [SUCCESS] Build Finished Successfully.
    echo [%date% %time%] [SUCCESS] Build Finished Successfully. >> "%LOG_FILE%"
) else (
    echo.
    echo [ERROR] Build Failed with exit code %EXIT_CODE%.
    echo [%date% %time%] [ERROR] Build Failed with exit code %EXIT_CODE%. >> "%LOG_FILE%"
    color 47
)

pause
