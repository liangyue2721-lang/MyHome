@echo off
rem Set encoding to UTF-8
chcp 65001 >nul
setlocal enabledelayedexpansion

title Money-Admin-Server

rem Create logs directory if it doesn't exist
if not exist "%~dp0logs" mkdir "%~dp0logs"

set "LOG_FILE=%~dp0logs\run_monitor.log"
set "APP_DIR=%~dp0..\money-admin\target"
set "JAR_NAME=money-admin.jar"
set "JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m"

:start
echo [%date% %time%] [INFO] Starting application... >> "%LOG_FILE%"
echo [%date% %time%] [INFO] Starting application...

cd /d "%APP_DIR%"
if %errorlevel% neq 0 (
    echo [%date% %time%] [ERROR] Target directory not found: %APP_DIR% >> "%LOG_FILE%"
    echo [ERROR] Target directory not found: %APP_DIR%
    timeout /t 10
    goto start
)

if not exist "%JAR_NAME%" (
    echo [%date% %time%] [ERROR] Jar file not found: %JAR_NAME% >> "%LOG_FILE%"
    echo [ERROR] Jar file not found: %JAR_NAME%
    timeout /t 10
    goto start
)

java %JAVA_OPTS% -jar %JAR_NAME%
set "EXIT_CODE=%errorlevel%"

echo [%date% %time%] [WARN] Application exited with code %EXIT_CODE%. Restarting in 10 seconds... >> "%LOG_FILE%"
echo [WARN] Application exited with code %EXIT_CODE%. Restarting in 10 seconds...

timeout /t 10
goto start
