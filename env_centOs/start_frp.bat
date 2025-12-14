@echo off
setlocal

:: 配置 —— 你当前目录
set FRP_ROOT=C:\Me\FRP\frp

:: 选择模式
:select
echo Select FRP mode:
echo 1) frps (server)
echo 2) frpc (client)
set /p MODE=Enter 1 or 2:

if "%MODE%"=="1" (
    set CMD_LINE="%FRP_ROOT%\frps.exe" -c "%FRP_ROOT%\frps.ini"
) else if "%MODE%"=="2" (
    set CMD_LINE="%FRP_ROOT%\frpc.exe" -c "%FRP_ROOT%\frpc.ini"
) else (
    echo Invalid selection. Try again.
    goto select
)

echo Running: %CMD_LINE%
cd /d "%FRP_ROOT%"
%CMD_LINE%

:: 执行完之后，不自动关闭窗口，以便查看日志
pause
endlocal
