@echo off
chcp 65001 >nul
setlocal

:: ==========================================================
:: Stock Service startup script (Windows)
:: Features:
:: 1. Ensure log directory exists
:: 2. Stop existing stock_service instance (best-effort)
:: 3. Start uvicorn in background
:: 4. Redirect all logs to file
:: ==========================================================

set "BASE_DIR=%~dp0"
set "LOG_DIR=%BASE_DIR%logs"
set "LOG_FILE=%LOG_DIR%\service.log"

if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
)

echo [INFO] Starting Stock Service...

:: Stop existing python process with window title "stock_service"
taskkill /F ^
    /FI "IMAGENAME eq python.exe" ^
    /FI "WINDOWTITLE eq stock_service" ^
    2>NUL

:: Start uvicorn in background (no new console window)
echo [INFO] Logs will be written to %LOG_FILE%

start "stock_service" /B ^
    python -m uvicorn stock_service:app ^
    --host 0.0.0.0 ^
    --port 8000 ^
    --log-level info ^
    > "%LOG_FILE%" 2>&1

echo [INFO] Stock Service start command issued.
echo [INFO] Service should now be running in background.

endlocal
