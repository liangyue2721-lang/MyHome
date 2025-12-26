@echo off
REM =====================================================================
REM Windows 一键 WSL2 + Ubuntu 安装 + 启动生产级 Stock Service
REM ---------------------------------------------------------------------
REM 功能：
REM   1) 检测是否支持 WSL2
REM   2) 自动启用 WSL2 / 虚拟机组件 / 安装 Ubuntu
REM   3) 进入 WSL2 安装 Python / Playwright 等依赖
REM   4) 启动 Linux 生产级服务 (Gunicorn)
REM =====================================================================

REM -------- 检查管理员权限 --------
openfiles >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] 请以管理员权限运行此脚本！
    pause
    exit /b 1
)

REM -------- WSL2 是否已启用 --------
wsl -l -v > wsl_list.txt 2>&1
findstr /c:"WSL2" wsl_list.txt >nul 2>&1
del wsl_list.txt

if %ERRORLEVEL% EQU 0 (
    echo [INFO] 检测到已安装 WSL2。
) else (
    echo [INFO] 未检测到 WSL2，正在启用 WSL2 环境...
    pause
    dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
    dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
    echo [INFO] WSL2 功能已启用。请重启电脑后重新运行本脚本。
    pause
    exit /b 0
)

REM -------- 检查是否安装 Ubuntu --------
wsl -l -v > wsl_list.txt 2>&1
findstr /i "Ubuntu" wsl_list.txt > nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo [INFO] 检测到已安装 Ubuntu 子系统。
) else (
    echo [INFO] 未检测到 Ubuntu 子系统，正在安装...
    echo [INFO] 如果无法自动安装，请手动从 Microsoft Store 安装 Ubuntu。
    wsl --install -d Ubuntu
    echo [INFO] 安装完成后请重新运行本脚本。
    pause
    exit /b 0
)
del wsl_list.txt

REM -------- 进入 WSL2 安装依赖与启动服务 --------
echo [INFO] 进入 WSL2 环境，自动安装依赖并启动服务...

wsl -d Ubuntu -u root bash -ic "

# =============================================================
# 自动安装 Python3 + pip + venv + 项目依赖 + Playwright
# =============================================================

set -e

echo '[WSL] 更新系统...'
apt update && apt upgrade -y

echo '[WSL] 安装 Python3 和基础工具...'
apt install -y python3 python3-venv python3-pip build-essential

PROJECT_DIR=/root/stock_service_ws
mkdir -p \$PROJECT_DIR

echo '[WSL] 拷贝项目到 WSL2...'
cp -r /mnt/c/%cd%/* \$PROJECT_DIR/

cd \$PROJECT_DIR

echo '[WSL] 创建并激活虚拟环境...'
python3 -m venv .venv
source .venv/bin/activate

echo '[WSL] 安装 Python 依赖...'
pip install --upgrade pip
pip install fastapi uvicorn gunicorn playwright

echo '[WSL] 安装 Playwright Chromium...'
python3 -m playwright install chromium

echo '[WSL] 启动生产级服务 (Gunicorn)...'
export CONTEXT_POOL_SIZE=4
export MAX_INFLIGHT_PER_PROVIDER=6

nohup gunicorn -c gunicorn_conf.py stock_service_prod:app > service.log 2>&1 &

echo '[WSL] 服务启动完毕！'
echo '[WSL] 运行日志输出 service.log'
echo '健康检查: curl http://localhost:8000/health'
"

echo [INFO] WSL2 启动命令已执行。
echo [INFO] 若第一次启用 WSL2，则需重启 Windows 并重新运行此脚本。
pause
