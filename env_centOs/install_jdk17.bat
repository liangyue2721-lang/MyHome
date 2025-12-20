@echo off
setlocal enabledelayedexpansion

echo ===============================
echo Install JDK 17
echo ===============================

REM ===== 目标安装目录 =====
set "BASE_DIR=C:\Java"
set "JDK_HOME=%BASE_DIR%\jdk-17"
set "ZIP_FILE=%TEMP%\jdk17.zip"

REM ===== Microsoft OpenJDK 17 =====
set "JDK_URL=https://aka.ms/download-jdk/microsoft-jdk-17.0.10-windows-x64.zip"

REM ===== 如果已存在 java.exe，直接配置环境变量 =====
if exist "%JDK_HOME%\bin\java.exe" (
    echo JDK 17 already installed: %JDK_HOME%
    goto SET_ENV
)

echo JDK 17 not found, downloading...

powershell -Command ^
"Invoke-WebRequest -Uri '%JDK_URL%' -OutFile '%ZIP_FILE%'"

if not exist "%ZIP_FILE%" (
    echo ERROR: Download failed
    pause
    exit /b 1
)

REM ===== 解压到临时目录 =====
set "TMP_DIR=%BASE_DIR%\_jdk17_tmp"
rd /s /q "%TMP_DIR%" >nul 2>&1
mkdir "%TMP_DIR%"

echo Extracting JDK...
powershell -Command ^
"Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%TMP_DIR%' -Force"

REM ===== 自动识别真正的 JDK 目录 =====
for /d %%D in ("%TMP_DIR%\*") do (
    if exist "%%D\bin\java.exe" (
        rd /s /q "%JDK_HOME%" >nul 2>&1
        move "%%D" "%JDK_HOME%" >nul
    )
)

rd /s /q "%TMP_DIR%"
del "%ZIP_FILE%"

if not exist "%JDK_HOME%\bin\java.exe" (
    echo ERROR: JDK installation failed
    pause
    exit /b 1
)

echo JDK 17 installed successfully

:SET_ENV
echo.
echo Configuring environment variables...

REM ===== 备份 JAVA_HOME =====
for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME 2^>nul') do (
    set "OLD_JAVA_HOME=%%B"
)

if defined OLD_JAVA_HOME (
    setx JAVA_HOME_BACKUP "%OLD_JAVA_HOME%" /M >nul
)

REM ===== 设置 JAVA_HOME =====
setx JAVA_HOME "%JDK_HOME%" /M >nul

REM ===== 设置 PATH（去重）=====
for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path') do (
    set "SYS_PATH=%%B"
)

echo %SYS_PATH% | find /I "%JDK_HOME%\bin" >nul
if errorlevel 1 (
    setx Path "%SYS_PATH%;%JDK_HOME%\bin" /M >nul
)

echo.
echo ===============================
echo JDK 17 setup completed
echo Please reopen CMD or PowerShell
echo ===============================

REM ===== 直接用绝对路径验证 =====
"%JDK_HOME%\bin\java.exe" -version

pause
endlocal
