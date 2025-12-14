#!/bin/bash
# ==========================================================
# 应用管理脚本（仅支持 JDK8）
# 用法： ./ry.sh {start|stop|restart|status} [-f]
# ==========================================================

# 应用名称（Jar 包名）
APP_NAME="money-admin.jar"

# 应用目录（当前目录）
APP_HOME=$(cd "$(dirname "$0")" && pwd)

# 日志目录与日志文件
LOG_DIR="$APP_HOME/logs"
LOG_FILE="$LOG_DIR/${APP_NAME%.jar}.log"

# ==========================================================
# 固定使用 JDK8 路径（CentOS 默认安装位置）
# ==========================================================
JAVA_CMD="/usr/lib/jvm/java-1.8.0-openjdk/bin/java"

if [ ! -x "$JAVA_CMD" ]; then
    echo -e "\033[0;31m[错误]\033[0m 未找到 JDK8，请先执行："
    echo -e "  \033[0;33mdnf -y install java-1.8.0-openjdk-devel\033[0m"
    exit 1
fi

# ==========================================================
# JVM 参数（专为 JDK8 优化）
# ==========================================================
JVM_OPTS="-Dname=$APP_NAME \
-Duser.timezone=Asia/Shanghai \
-Xms512m -Xmx1024m \
-XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:+PrintGCDateStamps -XX:+PrintGCDetails \
-XX:NewRatio=1 -XX:SurvivorRatio=30 \
-XX:+UseParallelGC -XX:+UseParallelOldGC"

# ==========================================================
# 工具函数
# ==========================================================
mkdir -p "$LOG_DIR"

log() {
    echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

get_pid() {
    ps -ef | grep "$APP_NAME" | grep java | grep -v grep | awk '{print $2}'
}

# ==========================================================
# 启动应用
# ==========================================================
start() {
    local PID
    PID=$(get_pid)

    if [ -n "$PID" ]; then
        log "\033[0;33m[警告]\033[0m $APP_NAME 已在运行 (PID: $PID)"
        return 0
    fi

    log "\033[0;32m[启动中]\033[0m 使用 JDK8 启动 $APP_NAME ..."
    nohup "$JAVA_CMD" $JVM_OPTS -jar "$APP_HOME/$APP_NAME" >>"$LOG_FILE" 2>&1 &
    sleep 2

    PID=$(get_pid)
    if [ -n "$PID" ]; then
        log "\033[0;32m[成功]\033[0m 启动成功，PID: $PID"
        log "日志文件：$LOG_FILE"
    else
        log "\033[0;31m[失败]\033[0m 启动失败，请检查日志：$LOG_FILE"
    fi
}

# ==========================================================
# 停止应用
# ==========================================================
stop() {
    local PID
    PID=$(get_pid)

    if [ -z "$PID" ]; then
        log "\033[0;33m[提示]\033[0m $APP_NAME 未运行"
        return 0
    fi

    log "\033[0;31m[停止中]\033[0m PID: $PID"
    kill -TERM "$PID"
    sleep 2

    PID=$(get_pid)
    if [ -n "$PID" ]; then
        if [ "$2" = "-f" ]; then
            log "\033[0;31m[强制停止]\033[0m"
            kill -9 "$PID"
        else
            log "\033[0;33m[警告]\033[0m 未完全停止，可使用 stop -f 强制停止"
            return 1
        fi
    fi

    log "\033[0;32m[已停止]\033[0m $APP_NAME"
}

# ==========================================================
# 重启应用
# ==========================================================
restart() {
    stop "$@"
    sleep 2
    start
}

# ==========================================================
# 查看状态
# ==========================================================
status() {
    local PID
    PID=$(get_pid)

    if [ -n "$PID" ]; then
        log "\033[0;32m[运行中]\033[0m $APP_NAME (PID: $PID)"
    else
        log "\033[0;31m[未运行]\033[0m $APP_NAME"
    fi
}

# ==========================================================
# 命令解析
# ==========================================================
case "$1" in
    start)
        start
        ;;
    stop)
        stop "$@"
        ;;
    restart)
        restart "$@"
        ;;
    status)
        status
        ;;
    *)
        echo -e "\033[0;36m用法:\033[0m ./ry.sh {start|stop|restart|status} [-f]"
        exit 1
        ;;
esac
