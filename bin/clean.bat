@echo off
rem Set encoding to UTF-8
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ===========================
echo  Project Clean Tool
echo ===========================

rem Create logs directory if it doesn't exist
if not exist "%~dp0logs" mkdir "%~dp0logs"
set "LOG_FILE=%~dp0logs\clean_history.log"

echo [%date% %time%] [INFO] Starting clean process... >> "%LOG_FILE%"
echo [INFO] Cleaning Project...

cd /d "%~dp0.."

call mvn clean
set "EXIT_CODE=%errorlevel%"

if %EXIT_CODE% equ 0 (
    echo.
    echo [SUCCESS] Clean Finished Successfully.
    echo [%date% %time%] [SUCCESS] Clean Finished Successfully. >> "%LOG_FILE%"
) else (
    echo.
    echo [ERROR] Clean Failed with exit code %EXIT_CODE%.
    echo [%date% %time%] [ERROR] Clean Failed with exit code %EXIT_CODE%. >> "%LOG_FILE%"
    color 47
)

pause
