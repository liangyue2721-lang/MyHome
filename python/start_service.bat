@echo off
chcp 65001
setlocal enabledelayedexpansion

echo =========================================
echo Stock Service - FOREGROUND MODE (Windows)
echo =========================================

REM ==========================================================
REM 1. Detect Python automatically from PATH
REM ==========================================================
set "PYTHON_EXE="

for /f "delims=" %%i in ('where python 2^>nul') do (
    set "PYTHON_EXE=%%i"
    goto :python_found
)

echo [ERROR] Python not found in PATH
echo [HINT ] Please install Python 3.9+ and ensure it is added to PATH
pause
exit /b 1

:python_found
echo [INFO] Using Python: %PYTHON_EXE%

REM ==========================================================
REM 2. Basic config
REM ==========================================================
set "APP_MODULE=stock_service:app"
set "HOST=0.0.0.0"
set "PORT=8000"

set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

echo [INFO] Base directory : %BASE_DIR%
echo [INFO] App module     : %APP_MODULE%
echo.

REM ==========================================================
REM 3. Check Python is runnable
REM ==========================================================
"%PYTHON_EXE%" -V >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python executable is not runnable
    pause
    exit /b 1
)

REM ==========================================================
REM 4. Check uvicorn
REM ==========================================================
echo [INFO] Checking uvicorn...
"%PYTHON_EXE%" -c "import uvicorn; print('uvicorn OK')" 2>nul
if errorlevel 1 (
    echo [ERROR] uvicorn is not installed
    echo [HINT ] Run:
    echo         "%PYTHON_EXE%" -m pip install uvicorn fastapi
    pause
    exit /b 1
)

REM ==========================================================
REM 5. Check port availability
REM ==========================================================
netstat -ano | findstr LISTENING | findstr :%PORT% >nul
if not errorlevel 1 (
    echo [ERROR] Port %PORT% is already in use
    pause
    exit /b 1
)

REM ==========================================================
REM 6. Start service (FOREGROUND)
REM ==========================================================
echo.
echo =========================================
echo Starting uvicorn (JSON access log enabled)
echo URL: http://127.0.0.1:%PORT%
echo Press Ctrl+C to stop
echo =========================================
echo.

"%PYTHON_EXE%" -m uvicorn %APP_MODULE% ^
    --host %HOST% ^
    --port %PORT% ^
    --log-level info ^
    --no-access-log

echo.
echo =========================================
echo Service stopped
echo =========================================
pause
