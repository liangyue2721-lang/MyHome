@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

REM ==========================================================
REM ANSI Color Setup
REM ==========================================================
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "C_RESET=%ESC%[0m"
set "C_GREEN=%ESC%[32m"
set "C_RED=%ESC%[31m"
set "C_YELLOW=%ESC%[33m"
set "C_CYAN=%ESC%[36m"
set "C_WHITE=%ESC%[37m"

echo %C_CYAN%=========================================%C_RESET%
echo %C_CYAN%Stock Service - START (Windows)%C_RESET%
echo %C_CYAN%=========================================%C_RESET%

REM ==========================================================
REM Python Detection
REM ==========================================================
where python >nul 2>nul
if errorlevel 1 (
    echo %C_RED%[ERROR]%C_RESET% Python not found
    pause
    exit /b 1
)

set APP_MODULE=stock_service:app
set HOST=0.0.0.0
set PORT=8000

REM ==========================================================
REM Start Service
REM ==========================================================
echo.
echo %C_GREEN%[INFO]%C_RESET% Starting uvicorn...
echo %C_WHITE%URL: http://127.0.0.1:%PORT%%C_RESET%
echo.

start "" cmd /c python -m uvicorn %APP_MODULE% --host %HOST% --port %PORT% --log-level info

REM ==========================================================
REM Wait & Health Check
REM ==========================================================
timeout /t 3 >nul

curl -s http://127.0.0.1:%PORT%/health > health.json

echo.
echo %C_CYAN%[BOOT]%C_RESET% Health Check Result:
type health.json
echo.

echo %C_YELLOW%Press Ctrl+C in uvicorn window to stop service%C_RESET%
pause
endlocal
