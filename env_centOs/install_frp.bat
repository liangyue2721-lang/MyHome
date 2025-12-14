@echo off
:: 可选：切换代码页为 UTF-8
:: 注意：部分 Windows 版本对 65001 支持不完善，可注释或删除此行
chcp 65001 >nul

:: ==============================
:: 配置信息
:: ==============================
set FRP_VERSION=0.65.0
set BASE_DIR=C:\Me\FRP
set FRP_ZIP=%BASE_DIR%\frp_%FRP_VERSION%_windows_amd64.zip
set FRP_EXTRACT_DIR=frp_%FRP_VERSION%_windows_amd64
set FRP_DIR=%BASE_DIR%\frp

echo ==================================================
echo Installing FRP version %FRP_VERSION%
echo Install dir: %FRP_DIR%
echo Zip file: %FRP_ZIP%
echo ==================================================

:: 检查管理员权限
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo.
    echo [ERROR] This script must be run as administrator!
    pause
    exit /b
)

:: 创建 BASE_DIR（如果不存在）
if not exist "%BASE_DIR%" (
    mkdir "%BASE_DIR%"
    echo Created directory %BASE_DIR%
)

:: 如果 ZIP 不存在，则尝试下载
if not exist "%FRP_ZIP%" (
    echo.
    echo ZIP not found — attempt to download...
    curl -L -o "%FRP_ZIP%" "https://github.com/fatedier/frp/releases/download/v%FRP_VERSION%/frp_%FRP_VERSION%_windows_amd64.zip"
    if not exist "%FRP_ZIP%" (
        echo.
        echo [ERROR] Download failed. Please place ZIP manually into %BASE_DIR%
        pause
        exit /b
    ) else (
        echo Download succeeded: %FRP_ZIP%
    )
) else (
    echo ZIP already exists.
)

:: 删除旧安装
if exist "%FRP_DIR%" (
    echo Removing old install dir %FRP_DIR%
    rmdir /s /q "%FRP_DIR%"
)

:: 解压 ZIP（使用 PowerShell 的 Expand-Archive）
echo.
echo Extracting %FRP_ZIP% to %BASE_DIR%
powershell -Command "Expand-Archive -LiteralPath '%FRP_ZIP%' -DestinationPath '%BASE_DIR%' -Force"

:: 移动解压后的目录
if exist "%BASE_DIR%\%FRP_EXTRACT_DIR%" (
    move "%BASE_DIR%\%FRP_EXTRACT_DIR%" "%FRP_DIR%" >nul
    echo Extract & move success: %FRP_DIR%
) else (
    echo [ERROR] Extraction failed — could not find %BASE_DIR%\%FRP_EXTRACT_DIR%
    pause
    exit /b
)

:: 选择 模式：服务端 / 客户端
echo.
echo Choose mode:
echo 1) frps (server)
echo 2) frpc (client)
set /p MODE=Select (1 or 2):

if "%MODE%"=="1" (
    set BIN=frps.exe
    set CONF=frps.ini
    set SERVICE=frps
) else if "%MODE%"=="2" (
    set BIN=frpc.exe
    set CONF=frpc.ini
    set SERVICE=frpc
) else (
    echo.
    echo [ERROR] Invalid selection, exit.
    pause
    exit /b
)

set CONF_PATH=%FRP_DIR%\%CONF%

:: 生成配置文件（如果不存在）
if not exist "%CONF_PATH%" (
    echo.
    echo Creating config file %CONF_PATH%
    if "%MODE%"=="1" (
        (
            echo [common]
            echo bind_port = 7000
            echo dashboard_port = 7500
            echo dashboard_user = admin
            echo dashboard_pwd = admin123
            echo token = 123456
        )>%CONF_PATH%
    ) else (
        (
            echo [common]
            echo server_addr = x.x.x.x
            echo server_port = 7000
            echo token = 123456

            echo [web]
            echo type = tcp
            echo local_ip = 127.0.0.1
            echo local_port = 80
            echo remote_port = 8080
        )>%CONF_PATH%
    )
    echo Config file created: %CONF_PATH%
) else (
    echo Config file already exists, skip.
)

:: 注册为 Windows Service
echo.
echo Register Windows service %SERVICE%
sc stop %SERVICE% >nul 2>&1
sc delete %SERVICE% >nul 2>&1

set EXEC="%FRP_DIR%\%BIN%" -c "%CONF_PATH%"
sc create %SERVICE% binPath= %EXEC% start= auto >nul
sc description %SERVICE% "FRP %SERVICE% (v%FRP_VERSION%)" >nul

:: 启动服务
echo.
echo Starting service %SERVICE%
sc start %SERVICE%

echo.
echo ==================================================
echo FRP installation complete!
echo Install dir: %FRP_DIR%
echo Config: %CONF_PATH%
echo Service name: %SERVICE%
echo ==================================================
echo.
echo To check status: sc query %SERVICE%
echo To stop: sc stop %SERVICE%
echo To uninstall: sc delete %SERVICE%
pause
endlocal
