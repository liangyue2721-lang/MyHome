@echo off
setlocal

echo ==========================================
echo Stock Service - Stop Script (Windows)
echo ==========================================

set PORT=8000

echo [INFO] Finding process on port %PORT% ...

REM 查找占用端口的 PID
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%PORT%') do (
    set PID=%%a
    goto FOUND
)

echo [INFO] No process found on port %PORT%
goto END

:FOUND
echo [INFO] Found process PID=%PID%
echo [INFO] Stopping service...

REM 优雅结束进程
taskkill /PID %PID% /F

if errorlevel 1 (
    echo ERROR: Failed to stop process %PID%
    goto END
)

echo [OK] Service stopped successfully.

:END
echo ==========================================
pause
endlocal
