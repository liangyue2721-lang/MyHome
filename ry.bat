@echo off
REM ===============================================================
REM  脚本名称：ruoyi-admin 服务管理脚本
REM  功能说明：用于启动、停止、重启、查看 ruoyi-admin.jar 的运行状态
REM  运行环境：Windows（需配置好 JDK 并可使用 jps 命令）
REM ===============================================================

REM ------------------------------
REM 配置应用 JAR 包名称
REM ------------------------------
set AppName=ruoyi-admin.jar

REM ------------------------------
REM JVM 启动参数
REM -Duser.timezone=Asia/Shanghai    设置时区为上海
REM -Xms512m                         初始堆大小
REM -Xmx1024m                        最大堆大小
REM -XX:MetaspaceSize=128m           元空间初始大小
REM -XX:MaxMetaspaceSize=512m        元空间最大大小
REM -XX:+HeapDumpOnOutOfMemoryError  内存溢出时生成堆转储文件
REM -XX:+PrintGCDateStamps           打印 GC 时间戳
REM -XX:+PrintGCDetails              打印 GC 详细信息
REM -XX:NewRatio=1                   新生代与老年代比例 1:1
REM -XX:SurvivorRatio=30             Eden 区与 Survivor 区比例
REM -XX:+UseParallelGC               启用并行垃圾收集器
REM -XX:+UseParallelOldGC            老年代使用并行 GC
REM ------------------------------
set JVM_OPTS="-Dname=%AppName% -Duser.timezone=Asia/Shanghai -Xms512m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"

REM ------------------------------
REM 菜单展示
REM ------------------------------
echo.
echo  [1] 启动 %AppName%
echo  [2] 停止 %AppName%
echo  [3] 重启 %AppName%
echo  [4] 查看状态 %AppName%
echo  [5] 退出
echo.

echo 请输入选项编号并按回车：
set /p ID=

if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" exit

pause
goto:eof

REM ------------------------------
REM 启动应用
REM ------------------------------
:start
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if defined pid (
    echo %AppName% 已经在运行，PID=%pid%
    pause
    goto:eof
)

start javaw %JVM_OPTS% -jar %AppName%
echo 正在启动 %AppName% ...
echo 启动成功！
goto:eof

REM ------------------------------
REM 停止应用
REM ------------------------------
:stop
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if not defined pid (
    echo 进程 %AppName% 未运行
) else (
    echo 准备结束进程 %image_name%，PID=%pid%
    taskkill /f /pid %pid%
    echo 已结束 %AppName% 进程
)
goto:eof

REM ------------------------------
REM 重启应用
REM ------------------------------
:restart
call :stop
call :start
goto:eof

REM ------------------------------
REM 查看状态
REM ------------------------------
:status
for /f "usebackq tokens=1-2" %%a in (`jps -l ^| findstr %AppName%`) do (
    set pid=%%a
    set image_name=%%b
)
if not defined pid (
    echo %AppName% 当前未运行
) else (
    echo %AppName% 正在运行，PID=%pid%
)
goto:eof
