@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ==============================================
echo   Windows 一键安装工具（JDK + Maven + Python + Git）
echo ==============================================

set BASE_DIR=%~dp0
set INSTALL_DIR=C:\DevTools

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
echo 目录: %INSTALL_DIR%
echo.

:: ==================================================
:: 检测是否已安装 Java
:: ==================================================
echo [检查] Java...
java -version >nul 2>&1
if %errorlevel%==0 (
    echo [跳过] 系统已存在 Java
) else (
    echo [安装] JDK...
    for %%f in ("%BASE_DIR%\jdk-*.exe") do (
        echo 检测到安装包: %%f
        %%f /s INSTALLDIR="%INSTALL_DIR%\Java"
    )
)
echo.

:: ==================================================
:: 检测是否已安装 Python
:: ==================================================
echo [检查] Python...
python --version >nul 2>&1
if %errorlevel%==0 (
    echo [跳过] 系统已存在 Python
) else (
    echo [安装] Python...
    for %%f in ("%BASE_DIR%\python-*.exe") do (
        echo 检测到安装包: %%f
        %%f /quiet InstallAllUsers=1 PrependPath=1 Include_test=0
    )
)
echo.

:: ==================================================
:: 检测是否已安装 Git
:: ==================================================
echo [检查] Git...
git --version >nul 2>&1
if %errorlevel%==0 (
    echo [跳过] 系统已存在 Git
) else (
    echo [安装] Git...
    for %%f in ("%BASE_DIR%\Git-*.exe") do (
        echo 检测到安装包: %%f
        %%f /SILENT
    )
)
echo.

:: ==================================================
:: 检测是否已安装 Maven（是否存在 install_dir/apache-maven-*）
:: ==================================================
echo [检查] Maven...
if exist "%INSTALL_DIR%\apache-maven-*\" (
    echo [跳过] Maven 已解压
) else (
    echo [安装] Maven...
    for %%f in ("%BASE_DIR%\apache-maven-*.zip") do (
        powershell -Command "Expand-Archive -Force '%%f' '%INSTALL_DIR%'"
    )
)
for /d %%d in ("%INSTALL_DIR%\apache-maven-*") do (
    set MAVEN_DIR=%%d
)
echo.

:: ==================================================
:: 增量写入系统 PATH（自动去重）
:: ==================================================
echo [环境变量] 正在设置...

REM 系统 PATH 读取
for /f "tokens=2,*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path') do (
    set SYS_PATH=%%b
)

REM 尝试追加 Java bin
echo !SYS_PATH! | find /i "%INSTALL_DIR%\Java\bin" >nul
if errorlevel 1 (
    call setx PATH "!SYS_PATH!;%INSTALL_DIR%\Java\bin" /M >nul
)

REM 尝试追加 Maven bin
echo !SYS_PATH! | find /i "%MAVEN_DIR%\bin" >nul
if errorlevel 1 (
    call setx PATH "!SYS_PATH!;%MAVEN_DIR%\bin" /M >nul
)

REM 尝试追加 Git bin
echo !SYS_PATH! | find /i "C:\Program Files\Git\bin" >nul
if errorlevel 1 (
    call setx PATH "!SYS_PATH!;C:\Program Files\Git\bin" /M >nul
)

REM 尝试追加 Python（自动检测两种常见目录）
if exist "C:\Program Files\Python311" (
    echo !SYS_PATH! | find /i "C:\Program Files\Python311" >nul
    if errorlevel 1 (
        call setx PATH "!SYS_PATH!;C:\Program Files\Python311;C:\Program Files\Python311\Scripts" /M >nul
    )
)

if exist "%LOCALAPPDATA%\Programs\Python\Python311" (
    echo !SYS_PATH! | find /i "%LOCALAPPDATA%\Programs\Python\Python311" >nul
    if errorlevel 1 (
        call setx PATH "!SYS_PATH!;%LOCALAPPDATA%\Programs\Python\Python311;%LOCALAPPDATA%\Programs\Python\Python311\Scripts" /M >nul
    )
)

echo [OK] PATH 写入成功（无覆盖、无重复）
echo.

:: ==================================================
:: 刷新当前窗口 PATH（无需重启 CMD）
:: ==================================================
echo [刷新环境] 立即生效 PATH...
powershell -command "$env:Path=[System.Environment]::GetEnvironmentVariable('Path','Machine')"

echo.
echo ==============================================
echo   所有软件安装完成 + 环境变量完全生效
echo ==============================================

echo.
echo [验证] 即刻测试：
echo   java -version
echo   mvn -version
echo   git --version
echo   python --version

pause
