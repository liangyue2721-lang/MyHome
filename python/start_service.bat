@echo off
chcp 65001
setlocal enabledelayedexpansion

REM ==========================================================
REM Enable ANSI colors
REM ==========================================================
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "C_RESET=%ESC%[0m"
set "C_RED=%ESC%[31m"
set "C_GREEN=%ESC%[32m"
set "C_YELLOW=%ESC%[33m"
set "C_CYAN=%ESC%[36m"
set "C_WHITE=%ESC%[37m"

echo %C_CYAN%=========================================%C_RESET%
echo %C_CYAN%Stock Service - FOREGROUND MODE (Windows)%C_RESET%
echo %C_CYAN%=========================================%C_RESET%

REM ==========================================================
REM 1. Detect Python (python.exe OR py.exe)
REM ==========================================================
set "PYTHON_EXE="

for /f "delims=" %%i in ('where python 2^>nul') do (
    set "PYTHON_EXE=python"
    goto :python_found
)

for /f "delims=" %%i in ('where py 2^>nul') do (
    set "PYTHON_EXE=py"
    goto :python_found
)

echo %C_RED%[ERROR]%C_RESET% Python not found
echo %C_YELLOW%[HINT ]%C_RESET% Install Python 3.9+ or ensure py launcher exists
pause
exit /b 1

:python_found

REM ==========================================================
REM 2. Basic config
REM ==========================================================
set "APP_MODULE=stock_service:app"
set "HOST=0.0.0.0"
set "PORT=8000"

set "BASE_DIR=%~dp0"
cd /d "%BASE_DIR%"

REM ==========================================================
REM 3. Environment Info
REM ==========================================================
echo.
echo %C_CYAN%================= Environment Info =================%C_RESET%
echo %C_WHITE%[OS      ]%C_RESET% %OS%
echo %C_WHITE%[USER    ]%C_RESET% %USERNAME%
echo %C_WHITE%[TIME    ]%C_RESET% %DATE% %TIME%
echo %C_WHITE%[WORKDIR ]%C_RESET% %CD%
echo.

echo %C_WHITE%[PYTHON ]%C_RESET% %PYTHON_EXE%
"%PYTHON_EXE%" --version

for /f "delims=" %%i in ('"%PYTHON_EXE%" -c "import sys; print(sys.executable)"') do (
    echo %C_WHITE%[PYTHON PATH]%C_RESET% %%i
)

"%PYTHON_EXE%" -m pip --version 2>nul
if errorlevel 1 (
    echo %C_YELLOW%[PIP     ] not available%C_RESET%
)

if exist requirements.txt (
    echo %C_GREEN%[REQ FILE]%C_RESET% requirements.txt found
) else (
    echo %C_RED%[REQ FILE]%C_RESET% requirements.txt NOT found
)

echo %C_CYAN%====================================================%C_RESET%
echo.

REM ==========================================================
REM 4. Check uvicorn
REM ==========================================================
echo %C_CYAN%[INFO]%C_RESET% Checking uvicorn...
"%PYTHON_EXE%" -c "import uvicorn; print('uvicorn OK')" 2>nul
if errorlevel 1 (
    echo %C_RED%[ERROR]%C_RESET% uvicorn is not installed
    echo %C_YELLOW%[HINT ]%C_RESET% "%PYTHON_EXE%" -m pip install -r requirements.txt
    pause
    exit /b 1
)

REM ==========================================================
REM 5. Check port availability
REM ==========================================================
netstat -ano | findstr LISTENING | findstr :%PORT% >nul
if not errorlevel 1 (
    echo %C_RED%[ERROR]%C_RESET% Port %PORT% is already in use
    pause
    exit /b 1
)

REM ==========================================================
REM 6. Start service (COLOR ENABLED)
REM ==========================================================
echo.
echo %C_GREEN%=========================================%C_RESET%
echo %C_GREEN%Starting uvicorn%C_RESET%
echo %C_WHITE%URL: http://127.0.0.1:%PORT%%C_RESET%
echo %C_YELLOW%Press Ctrl+C to stop%C_RESET%
echo %C_GREEN%=========================================%C_RESET%
echo.

"%PYTHON_EXE%" -m uvicorn %APP_MODULE% ^
    --host %HOST% ^
    --port %PORT% ^
    --log-level info ^
    --use-colors

echo.
echo %C_CYAN%=========================================%C_RESET%
echo %C_CYAN%Service stopped%C_RESET%
echo %C_CYAN%=========================================%C_RESET%
pause
