@echo off
chcp 65001 >nul

echo.
echo [信息] 使用 Jar 启动 Web 工程...
echo.

cd %~dp0
cd ../ruoyi-admin/target

set JAVA_OPTS=-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m

java %JAVA_OPTS% -jar money-admin.jar

cd bin
pause
