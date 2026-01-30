@echo off
setlocal
chcp 65001 >nul

echo ==========================================
echo Stock Service - Stop Script (Safe Kill)
echo ==========================================

set PORT=8000

echo [INFO] Finding process on port %PORT% ...

REM 查找占用端口的 PID
set PID=
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :%PORT%') do (
    set PID=%%a
    goto FOUND
)

echo [INFO] No process found on port %PORT%
goto END

:FOUND
echo [INFO] Found process PID=%PID%
echo [INFO] Stopping service and child processes...

REM 优雅结束进程树 (/T 杀死子进程，/F 强制)
taskkill /PID %PID% /F /T

if errorlevel 1 (
    echo [WARN] Could not kill process %PID% (Verify if it is already gone)
    goto END
)

echo [OK] Service stopped successfully.

:END
echo ==========================================
pause
endlocal