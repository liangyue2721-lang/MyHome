#!/bin/bash
# ==========================================================
# Stock Service startup script (Production Grade)
#
# Features:
# 1. Auto-detect & bind venv Python
# 2. Ensure uvicorn / fastapi / playwright
# 3. Auto-install Playwright browsers (once)
# 4. Prevent duplicate start (port + pid)
# 5. Graceful stop
# 6. Background run with nohup
# 7. Playwright-safe for servers / containers
# ==========================================================

set -euo pipefail

# -------------------- Config --------------------
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$BASE_DIR/stock_service.pid"

LOG_DIR="$BASE_DIR/logs"
SERVICE_LOG="$LOG_DIR/service.log"

APP_MODULE="stock_service:app"
HOST="0.0.0.0"
PORT="8000"

# Preferred venv path
VENV_PY="$BASE_DIR/.venv/bin/python"
SYS_PY="/usr/bin/python3"

# -------------------- Init --------------------
cd "$BASE_DIR"
mkdir -p "$LOG_DIR"

# -------------------- Python detection --------------------
if [ -x "$VENV_PY" ]; then
    PYTHON_BIN="$VENV_PY"
    echo "[INFO] Using venv Python: $PYTHON_BIN"
else
    PYTHON_BIN="$SYS_PY"
    echo "[WARN] venv not found, using system Python: $PYTHON_BIN"
fi

if [ ! -x "$PYTHON_BIN" ]; then
    echo "[ERROR] Python not found: $PYTHON_BIN"
    exit 1
fi

# -------------------- pip check --------------------
if ! "$PYTHON_BIN" -m pip --version >/dev/null 2>&1; then
    echo "[ERROR] pip not available in this Python"
    exit 1
fi

# -------------------- Dependency check --------------------
echo "[INFO] Checking Python dependencies..."

"$PYTHON_BIN" - <<'EOF'
import importlib.util, sys

required = ("uvicorn", "fastapi", "playwright")
missing = [m for m in required if importlib.util.find_spec(m) is None]

if missing:
    print("Missing:", ", ".join(missing))
    sys.exit(1)
sys.exit(0)
EOF

if [ $? -ne 0 ]; then
    echo "[INFO] Installing missing Python packages..."
    "$PYTHON_BIN" -m pip install --upgrade pip
    "$PYTHON_BIN" -m pip install uvicorn fastapi playwright
fi

# -------------------- Playwright browser check --------------------
echo "[INFO] Checking Playwright browser installation..."

"$PYTHON_BIN" - <<'EOF'
from pathlib import Path
import os, sys

cache = Path.home() / ".cache" / "ms-playwright"
if not cache.exists() or not any(cache.iterdir()):
    sys.exit(1)
sys.exit(0)
EOF

if [ $? -ne 0 ]; then
    echo "[INFO] Installing Playwright Chromium (first run may be slow)..."
    "$PYTHON_BIN" -m playwright install chromium
else
    echo "[INFO] Playwright browsers already installed"
fi

# -------------------- Port check --------------------
if command -v ss >/dev/null 2>&1; then
    if ss -lnt | grep -q ":$PORT "; then
        echo "[WARN] Port $PORT already in use. Service may be running."
        exit 0
    fi
else
    if lsof -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
        echo "[WARN] Port $PORT already in use. Service may be running."
        exit 0
    fi
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
echo "[INFO] Launching uvicorn..."

nohup "$PYTHON_BIN" -m uvicorn "$APP_MODULE" \
    --host "$HOST" \
    --port "$PORT" \
    --log-level info \
    --no-access-log \
    >> "$SERVICE_LOG" 2>&1 &

NEW_PID=$!
sleep 2

# -------------------- Verify start --------------------
if ps -p "$NEW_PID" >/dev/null 2>&1; then
    echo "$NEW_PID" > "$PID_FILE"
    echo "[INFO] Stock Service started successfully"
    echo "[INFO] PID        : $NEW_PID"
    echo "[INFO] Service log: $SERVICE_LOG"
else
    echo "[ERROR] Failed to start Stock Service"
    exit 1
fi
