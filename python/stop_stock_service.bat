@echo off
setlocal

REM ==========================================================
REM  Stock Service 停止脚本
REM  通过窗口标题终止后台 uvicorn 进程
REM ==========================================================

echo [INFO] Stopping Stock Service...

REM 终止标题为 stock_service 的 python 进程
taskkill /F ^
    /FI "IMAGENAME eq python.exe" ^
    /FI "WINDOWTITLE eq stock_service"

if %ERRORLEVEL% EQU 0 (
    echo [INFO] Stock Service stopped successfully.
) else (
    echo [WARN] No running Stock Service found.
)

endlocal
