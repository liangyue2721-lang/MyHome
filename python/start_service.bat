@echo off
setlocal

set "BASE_DIR=%~dp0"
set "LOG_DIR=%BASE_DIR%logs"
set "LOG_FILE=%LOG_DIR%\service.log"

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

echo [INFO] Starting Stock Service...

:: Kill existing python uvicorn process (rough check, might kill others)
taskkill /F /FI "IMAGENAME eq python.exe" /FI "WINDOWTITLE eq stock_service" 2>NUL

:: Start service
echo [INFO] Logs will be written to %LOG_FILE%
start "stock_service" /B python -m uvicorn stock_service:app --host 0.0.0.0 --port 8000 --log-level info > "%LOG_FILE%" 2>&1

echo [INFO] Stock Service start command issued.
endlocal
