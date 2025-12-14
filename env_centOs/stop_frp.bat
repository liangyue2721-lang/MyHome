@echo off
setlocal

:: 停止并删除 FRP 服务

echo Stopping FRP services (if exist)...

sc stop frps >nul 2>&1
sc delete frps >nul 2>&1

sc stop frpc >nul 2>&1
sc delete frpc >nul 2>&1

echo FRP services stopped and removed (if existed).
pause
endlocal
