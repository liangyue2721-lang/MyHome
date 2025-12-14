@echo off
setlocal enabledelayedexpansion

echo ======================================
echo   Playwright Environment Installer
echo ======================================

:: Check Python
echo [10%%] Checking Python...
python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found. Please install Python 3.10+
    pause
    exit /b
)
for /f "tokens=*" %%i in ('python --version') do set PYV=%%i
echo [OK] Python: %PYV%

:: Check pip
echo [25%%] Checking pip...
pip --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] pip not found. Make sure Python added to PATH
    pause
    exit /b
)
echo [OK] pip is available.

:: Upgrade pip
echo [40%%] Upgrading pip...
python -m pip install --upgrade pip >nul 2>&1
if errorlevel 1 (
    echo [WARN] Failed to upgrade pip (ignored)
) else (
    echo [OK] pip upgraded
)

:: Install Playwright
echo [60%%] Installing Playwright...
pip install playwright >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Failed to install Playwright.
    pause
    exit /b
)
echo [OK] Playwright installed

:: Install browser drivers
echo [80%%] Installing browser drivers...
python -m playwright install
if errorlevel 1 (
    echo [ERROR] Failed to install browser drivers.
    pause
    exit /b
)
echo [OK] Browser drivers installed.

echo [100%%] COMPLETE!
echo ======================================
echo Playwright Installation Successful!
echo Run: python -m playwright codegen
echo ======================================

pause
