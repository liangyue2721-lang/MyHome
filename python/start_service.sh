#!/bin/bash
# ==========================================================
# Stock Service startup script (Linux / macOS)
# Features:
# 1. Bind to a specific Python interpreter
# 2. Check & auto-install uvicorn / fastapi
# 3. Prevent duplicate start (port check)
# 4. Graceful stop with PID file
# 5. Background run with nohup
# ==========================================================

set -euo pipefail

# -------------------- Config --------------------
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$BASE_DIR/stock_service.pid"
LOG_DIR="$BASE_DIR/logs"
LOG_FILE="$LOG_DIR/service.log"

APP_MODULE="stock_service:app"
HOST="0.0.0.0"
PORT="8000"

# Python interpreter (change if using venv)
PYTHON_BIN="/usr/bin/python3"

# -------------------- Init --------------------
cd "$BASE_DIR"
mkdir -p "$LOG_DIR"

echo "[INFO] Using Python: $PYTHON_BIN"
echo "[INFO] Starting Stock Service..."

# -------------------- Check Python --------------------
if [ ! -x "$PYTHON_BIN" ]; then
    echo "[ERROR] Python not found: $PYTHON_BIN"
    exit 1
fi

# -------------------- Check pip --------------------
if ! "$PYTHON_BIN" -m pip --version >/dev/null 2>&1; then
    echo "[ERROR] pip is not available in this Python environment"
    exit 1
fi

# -------------------- Check & install dependencies --------------------
echo "[INFO] Checking Python dependencies..."

"$PYTHON_BIN" - <<'EOF'
import importlib.util, sys
missing = []
for m in ("uvicorn", "fastapi"):
    if importlib.util.find_spec(m) is None:
        missing.append(m)
sys.exit(1 if missing else 0)
EOF

if [ $? -ne 0 ]; then
    echo "[INFO] Missing dependencies detected, installing..."
    "$PYTHON_BIN" -m pip install --upgrade pip
    "$PYTHON_BIN" -m pip install uvicorn fastapi
fi

# -------------------- Port check --------------------
if ss -lnt 2>/dev/null | grep -q ":$PORT "; then
    echo "[WARN] Port $PORT is already in use. Service may already be running."
    exit 0
fi

# -------------------- Stop existing process --------------------
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" >/dev/null 2>&1; then
        echo "[INFO] Stopping existing service (PID=$OLD_PID)"
        kill "$OLD_PID" || true
        sleep 3
        if ps -p "$OLD_PID" >/dev/null 2>&1; then
            echo "[WARN] Force killing PID=$OLD_PID"
            kill -9 "$OLD_PID" || true
        fi
    fi
    rm -f "$PID_FILE"
fi

# -------------------- Start service --------------------
nohup "$PYTHON_BIN" -m uvicorn "$APP_MODULE" \
    --host "$HOST" \
    --port "$PORT" \
    --log-level info \
    >> "$LOG_FILE" 2>&1 &

NEW_PID=$!
sleep 1

# -------------------- Verify start --------------------
if ps -p "$NEW_PID" >/dev/null 2>&1; then
    echo "$NEW_PID" > "$PID_FILE"
    echo "[INFO] Stock Service started successfully (PID=$NEW_PID)"
    echo "[INFO] Log file: $LOG_FILE"
else
    echo "[ERROR] Failed to start Stock Service"
    exit 1
fi
