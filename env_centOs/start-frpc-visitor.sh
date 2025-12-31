#!/bin/bash
#
# 访问端 frpc 启动脚本（当前目录）
# 用法：./start-frpc-visitor.sh
#
# 当前目录：脚本所在目录
# 要求：frpc 和 frpc-visitor.ini 在同一目录

# 切换到脚本所在目录
cd "$(dirname "$0")"

FRPC="./frpc"
CONF="frpc-visitor.ini"
LOG="frpc-visitor.log"

echo "========================================"
echo "Starting frpc visitor (访问端)"
echo "Directory: $(pwd)"
echo "frpc binary: $FRPC"
echo "Config file: $CONF"
echo "Log file: $LOG"
echo "========================================"

# 检查 frpc 是否存在
if [ ! -f "$FRPC" ]; then
  echo "ERROR: frpc not found in current directory!"
  exit 1
fi

# 检查配置文件
if [ ! -f "$CONF" ]; then
  echo "ERROR: frpc-visitor.ini not found in current directory!"
  exit 1
fi

# 启动 frpc，并将输出写入日志
nohup "$FRPC" -c "$CONF" > "$LOG" 2>&1 &

echo "frpc visitor started in background."
echo "Use: tail -f $LOG to watch logs."
