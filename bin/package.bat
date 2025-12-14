@echo off
chcp 65001 >nul

echo ===========================
echo  Web 构建工具
echo ===========================

echo [信息] 正在构建 Web 项目...
echo.

%~d0
cd %~dp0
cd ..

call mvn clean package -Dmaven.test.skip=true

echo.
echo [完成] 构建结束
pause
