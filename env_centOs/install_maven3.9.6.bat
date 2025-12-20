@echo off
setlocal enabledelayedexpansion

REM ===============================
REM Apache Maven 3.9.6 安装脚本
REM ===============================

set MAVEN_VERSION=3.9.6
set INSTALL_DIR=D:\JavaTool
set MAVEN_HOME=%INSTALL_DIR%\apache-maven-%MAVEN_VERSION%
set DOWNLOAD_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip
set ZIP_FILE=%INSTALL_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip

echo.
echo ====== Installing Apache Maven %MAVEN_VERSION% ======
echo Target Directory: %MAVEN_HOME%
echo.

REM 1. 创建安装目录
if not exist "%INSTALL_DIR%" (
    mkdir "%INSTALL_DIR%"
)

REM 2. 下载 Maven
echo Downloading Maven...
powershell -Command ^
    "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%'"

if not exist "%ZIP_FILE%" (
    echo [ERROR] Maven download failed.
    pause
    exit /b 1
)

REM 3. 解压 Maven
echo Extracting Maven...
powershell -Command ^
    "Expand-Archive -Force '%ZIP_FILE%' '%INSTALL_DIR%'"

REM 4. 删除 zip 文件
del "%ZIP_FILE%"

REM 5. 设置环境变量（用户级）
echo Setting MAVEN_HOME and updating PATH...

setx MAVEN_HOME "%MAVEN_HOME%"
setx PATH "%MAVEN_HOME%\bin;%PATH%"

echo.
echo ====== Maven %MAVEN_VERSION% installation completed ======
echo.
echo Please CLOSE all terminals and open a new one, then run:
echo.
echo     mvn -v
echo.
pause
