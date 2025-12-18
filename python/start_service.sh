#!/bin/bash
# start_service.sh

set -e

BASE_DIR=$(cd "$(dirname "$0")" && pwd)
PID_FILE="$BASE_DIR/stock_service.pid"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/service.log"

cd "$BASE_DIR"
mkdir -p "$LOG_DIR"

echo "[INFO] Starting Stock Service..."

# Stop existing process safely
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p $PID > /dev/null 2>&1; then
        echo "[INFO] Stopping existing service (PID=$PID)"
        kill $PID
        sleep 2
    fi
    rm -f "$PID_FILE"
fi

# Start service
nohup uvicorn stock_service:app \
    --host 0.0.0.0 \
    --port 8000 \
    --log-level info \
    >> "$LOG_FILE" 2>&1 &

NEW_PID=$!
echo $NEW_PID > "$PID_FILE"

sleep 1

if ps -p $NEW_PID > /dev/null 2>&1; then
    echo "[INFO] Stock Service started successfully (PID=$NEW_PID)"
    echo "[INFO] Log file: $LOG_FILE"
else
    echo "[ERROR] Failed to start Stock Service"
    exit 1
fi
