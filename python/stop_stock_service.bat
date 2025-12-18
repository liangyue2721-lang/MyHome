@echo off
chcp 65001 >nul
setlocal

set PORT=8000
set FOUND=0

echo [INFO] Stopping Stock Service on port %PORT% ...

for /f "tokens=5" %%p in ('netstat -ano ^| findstr :%PORT% ^| findstr LISTENING') do (
    echo [INFO] Found process PID=%%p
    taskkill /F /PID %%p
    set FOUND=1
)

if "%FOUND%"=="0" (
    echo [WARN] No process found listening on port %PORT%.
) else (
    echo [INFO] Stock Service stopped successfully.
)

endlocal
