@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ==========================================================
REM Stock Service startup script (Windows)
REM Features:
REM 1. Bind to a specific Python interpreter
REM 2. Check uvicorn / fastapi availability
REM 3. Auto-install missing dependencies
REM 4. Start uvicorn in background
REM ==========================================================

REM ---------- Python interpreter (DO NOT rely on PATH) ----------
set "PYTHON_EXE=C:\Program Files\Python311\python.exe"

REM ---------- App config ----------
set "APP_MODULE=stock_service:app"
set "HOST=0.0.0.0"
set "PORT=8000"

REM ---------- Paths ----------
set "BASE_DIR=%~dp0"
set "LOG_DIR=%BASE_DIR%logs"
set "LOG_FILE=%LOG_DIR%\service.log"

if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
)

echo [INFO] Using Python: %PYTHON_EXE%

REM ==========================================================
REM 1. Check Python exists
REM ==========================================================
if not exist %PYTHON_EXE% (
    echo [ERROR] Python not found: %PYTHON_EXE%
    exit /b 1
)

REM ==========================================================
REM 2. Ensure pip is available
REM ==========================================================
%PYTHON_EXE% -m pip --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] pip is not available in this Python environment
    exit /b 1
)

REM ==========================================================
REM 3. Check & install uvicorn
REM ==========================================================
%PYTHON_EXE% - <<EOF >nul 2>&1
import importlib.util, sys
sys.exit(0 if importlib.util.find_spec("uvicorn") else 1)
EOF

if errorlevel 1 (
    echo [INFO] uvicorn not found, installing...
    %PYTHON_EXE% -m pip install --upgrade pip
    %PYTHON_EXE% -m pip install uvicorn fastapi
    if errorlevel 1 (
        echo [ERROR] Failed to install uvicorn / fastapi
        exit /b 1
    )
) else (
    echo [INFO] uvicorn already installed
)

REM ==========================================================
REM 4. Prevent duplicate start (port check)
REM ==========================================================
netstat -ano | findstr LISTENING | findstr :%PORT% >nul && (
    echo [WARN] Port %PORT% already in use. Service may already be running.
    exit /b 0
)

REM ==========================================================
REM 5. Start service in background
REM ==========================================================
echo [INFO] Starting Stock Service...
echo [INFO] Logs -> %LOG_FILE%

start "stock_service" /B ^
    %PYTHON_EXE% -m uvicorn %APP_MODULE% ^
    --host %HOST% ^
    --port %PORT% ^
    --log-level info ^
    > "%LOG_FILE%" 2>&1

echo [INFO] Stock Service start command issued.
echo [INFO] Service should now be running in background.

endlocal
