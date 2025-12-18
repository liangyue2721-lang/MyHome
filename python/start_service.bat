@echo off
setlocal

REM ==========================================================
REM  Stock Service 启动脚本（Windows）
REM  功能：
REM  1. 创建日志目录
REM  2. 尝试停止已存在的 stock_service 实例（粗略方式）
REM  3. 后台启动 uvicorn 服务
REM  4. 所有日志写入 logs/service.log
REM ==========================================================

REM 当前脚本所在目录（末尾自动带 \）
set "BASE_DIR=%~dp0"

REM 日志目录
set "LOG_DIR=%BASE_DIR%logs"

REM 日志文件路径
set "LOG_FILE=%LOG_DIR%\service.log"

REM 若日志目录不存在则创建
if not exist "%LOG_DIR%" (
    mkdir "%LOG_DIR%"
)

echo [INFO] Starting Stock Service...

REM ----------------------------------------------------------
REM 尝试结束已存在的 Python 进程
REM ⚠ 注意：
REM  - 这是“粗略匹配”，可能会误杀其他 python.exe
REM  - 仅适合本机专用环境
REM ----------------------------------------------------------
taskkill /F ^
    /FI "IMAGENAME eq python.exe" ^
    /FI "WINDOWTITLE eq stock_service" ^
    2>NUL

REM ----------------------------------------------------------
REM 后台启动 FastAPI / uvicorn
REM 说明：
REM  - start /B：后台运行，不新开窗口
REM  - "stock_service"：窗口标题（用于 taskkill 过滤）
REM  - stdout / stderr 重定向到日志文件
REM ----------------------------------------------------------
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
