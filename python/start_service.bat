@echo off
setlocal
chcp 65001 >nul

echo ==========================================
echo Stock Service - Windows Stable Startup
echo ==========================================

REM ------------------------------------------------
REM Step 1: 切换到脚本所在目录
REM ------------------------------------------------
cd /d %~dp0
if errorlevel 1 goto FAIL

REM ------------------------------------------------
REM Step 2: 检查 Python 环境
REM ------------------------------------------------
echo [CHECK] Python Version
python --version
if errorlevel 1 (
    echo ERROR: Python not found in PATH
    goto FAIL
)

REM ------------------------------------------------
REM Step 3: 检查并创建虚拟环境 (venv)
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
REM Step 4: 激活虚拟环境
REM ------------------------------------------------
echo [INFO] Activating venv...
call venv\Scripts\activate.bat
if errorlevel 1 (
    echo ERROR: failed to activate venv
    goto FAIL
)

REM ------------------------------------------------
REM Step 5: 升级 pip (静默模式)
REM ------------------------------------------------
echo [INFO] Upgrading pip...
python -m pip install --upgrade pip >nul
if errorlevel 1 (
    echo ERROR: pip upgrade failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 6: 安装/更新依赖 (关键：增加了 httpx)
REM ------------------------------------------------
echo [INFO] Installing dependencies (fastapi, uvicorn, playwright, httpx)...
pip install fastapi uvicorn playwright httpx >nul
if errorlevel 1 (
    echo ERROR: dependency installation failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 7: 安装 Playwright 内核
REM ------------------------------------------------
echo [INFO] Checking Playwright Chromium...
playwright install chromium
if errorlevel 1 (
    echo ERROR: playwright chromium install failed
    goto FAIL
)

REM ------------------------------------------------
REM Step 8: 启动服务
REM ------------------------------------------------
echo.
echo ==========================================
echo Starting Stock Service v2.1...
echo ==========================================
echo.

REM 使用 Python 直接运行脚本，以便触发代码中的 __main__ 入口
python stock_service.py

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