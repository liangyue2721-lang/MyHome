@echo off
setlocal enabledelayedexpansion

echo ===============================
echo  Configure JDK 17 Environment
echo ===============================

REM ===== 修改为你自己的 JDK 17 路径 =====
set "NEW_JAVA_HOME=C:\Program Files\Java\jdk-17"

REM ===== 检查 JDK 17 是否存在 =====
if not exist "%NEW_JAVA_HOME%\bin\java.exe" (
    echo ERROR: JDK 17 not found at:
    echo %NEW_JAVA_HOME%
    pause
    exit /b 1
)

REM ===== 备份原 JAVA_HOME =====
for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME 2^>nul') do (
    set "OLD_JAVA_HOME=%%B"
)

if defined OLD_JAVA_HOME (
    echo Backup old JAVA_HOME: %OLD_JAVA_HOME%
    setx JAVA_HOME_BACKUP "%OLD_JAVA_HOME%" /M
)

REM ===== 设置新的 JAVA_HOME =====
echo Setting JAVA_HOME to %NEW_JAVA_HOME%
setx JAVA_HOME "%NEW_JAVA_HOME%" /M

REM ===== 处理 PATH（去重并追加 %JAVA_HOME%\bin）=====
for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v Path') do (
    set "SYS_PATH=%%B"
)

echo %SYS_PATH% | find /I "%NEW_JAVA_HOME%\bin" >nul
if errorlevel 1 (
    echo Adding JAVA_HOME\bin to PATH
    setx Path "%SYS_PATH%;%NEW_JAVA_HOME%\bin" /M
) else (
    echo JAVA_HOME\bin already exists in PATH
)

REM ===== 提示重开窗口 =====
echo.
echo ===============================
echo JDK 17 configured successfully
echo Please reopen CMD or PowerShell
echo ===============================

REM ===== 验证（当前窗口可能仍是旧 PATH）=====
"%NEW_JAVA_HOME%\bin\java.exe" -version

pause
endlocal
