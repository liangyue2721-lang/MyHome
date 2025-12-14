@echo off
chcp 65001 >nul
title Git 账户配置工具(固定账户信息)

echo ===============================================
echo         Git 账户自动配置（固定信息）
echo ===============================================
echo.

:: =======【在此修改你的账户信息】===================
set GIT_USER="YourUserName"
set GIT_EMAIL="your_email@example.com"
:: ================================================

:: ===== 检查 Git 是否安装 =====
where git >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Git，请先安装 Git。
    pause
    exit /b 1
)

echo [检测到 Git] ✓
echo.

:: ===== 显示将要设置的账户信息 =====
echo [将要配置 Git 全局账户]：
echo 用户名: %GIT_USER%
echo 邮箱:   %GIT_EMAIL%
echo.

:: ===== 执行配置 =====
echo [设置中] ...
git config --global user.name %GIT_USER%
git config --global user.email %GIT_EMAIL%

:: ===== 可选：自动配置 VSCode 为编辑器 =====
where code >nul 2>&1
if %errorlevel%==0 (
    git config --global core.editor "code --wait"
)

:: ===== 最终结果展示 =====
echo.
echo [最终 Git 账户配置]：
echo -----------------------------------------
git config --global user.name
git config --global user.email
git config --global core.editor
echo -----------------------------------------

echo.
echo [完成] Git 账户已配置成功。
pause
