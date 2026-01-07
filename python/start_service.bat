@echo off
setlocal

echo ==========================================
echo Stock Service - Windows Stable Startup
echo ==========================================

REM ------------------------------------------------
REM Step 1: switch to script directory
REM ------------------------------------------------
cd /d %~dp0
if errorlevel 1 goto FAIL

REM ------------------------------------------------
REM Step 2: check python
REM ------------------------------------------------
echo [CHECK] Python
python --version
if errorlevel 1 (
    echo ERROR: Python not found in PATH
    goto FAIL
)

REM ------------------------------------------------
REM Step 3: ensure venv exists
REM ------------------------------------------------
if not exist venv\Scripts\activate.bat (
    echo [INFO] venv not found, creating virtual environment...
    python -m venv venv
    if errorlevel 1 (
        echo ERROR: failed to create venv
        goto FAIL
    )
)

REM ------------------------------------------------
REM Step 4: activate venv
REM ------------------------------------------------
echo [INFO] activate venv
call venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: failed to activate venv
    goto FAIL
)

REM ------------------------------------------------
REM Step 5: upgrade pip
REM ------------------------------------------------
echo [INFO] upgrade pip
python -m pip install --upgrade pip >nul
if errorlevel 1 (
    echo ERROR: pip upgrade failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 6: install required packages
REM ------------------------------------------------
echo [INFO] install python dependencies
pip install fastapi uvicorn playwright >nul
if errorlevel 1 (
    echo ERROR: dependency installation failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 7: install playwright chromium
REM ------------------------------------------------
echo [INFO] install playwright chromium
playwright install chromium
if errorlevel 1 (
    echo ERROR: playwright chromium install failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 8: start service
REM ------------------------------------------------
echo.
echo ==========================================
echo Starting Stock Service...
echo ==========================================
echo.

python -m uvicorn stock_service:app ^
  --host 0.0.0.0 ^
  --port 8000 ^
  --workers 1 ^
  --log-level info

goto END

:FAIL
echo.
echo ==========================================
echo FAILED TO START SERVICE
echo ==========================================
echo Please check the error message above.
echo.
pause
goto END

:END
endlocal
