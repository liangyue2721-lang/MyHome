#!/bin/bash
# ==========================================================
# Stock Service startup script (Production, Gunicorn)
# Linux / macOS / WSL2
# ==========================================================

set -euo pipefail

# -------------------- Config --------------------
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_FILE="$BASE_DIR/stock_service.pid"

LOG_DIR="$BASE_DIR/logs"
SERVICE_LOG="$LOG_DIR/service.log"

APP_MODULE="stock_service_prod:app"
HOST="0.0.0.0"
PORT="8000"

VENV_PY="$BASE_DIR/.venv/bin/python"
SYS_PY="$(command -v python3 || true)"

# -------------------- Init --------------------
cd "$BASE_DIR"
mkdir -p "$LOG_DIR"

# -------------------- Python detection --------------------
if [ -x "$VENV_PY" ]; then
    PYTHON_BIN="$VENV_PY"
    echo "[INFO] Using venv Python: $PYTHON_BIN"
elif [ -x "$SYS_PY" ]; then
    PYTHON_BIN="$SYS_PY"
    echo "[WARN] Using system Python: $PYTHON_BIN"
else
    echo "[ERROR] Python not found"
    exit 1
fi

# -------------------- Dependency check --------------------
"$PYTHON_BIN" - <<'EOF'
import importlib.util, sys
need = ("gunicorn", "uvicorn", "fastapi", "playwright")
missing = [m for m in need if importlib.util.find_spec(m) is None]
if missing:
    print("Missing:", missing)
    sys.exit(1)
EOF

if [ $? -ne 0 ]; then
    echo "[INFO] Installing dependencies..."
    "$PYTHON_BIN" -m pip install --upgrade pip
    "$PYTHON_BIN" -m pip install gunicorn uvicorn fastapi playwright
fi

# -------------------- Playwright browser --------------------
"$PYTHON_BIN" - <<'EOF'
from pathlib import Path
cache = Path.home() / ".cache" / "ms-playwright"
exit(0 if cache.exists() and any(cache.iterdir()) else 1)
EOF

if [ $? -ne 0 ]; then
    echo "[INFO] Installing Playwright Chromium..."
    "$PYTHON_BIN" -m playwright install chromium
fi

# -------------------- Stop old process --------------------
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" >/dev/null 2>&1; then
        echo "[INFO] Stopping old service (PID=$OLD_PID)"
        kill "$OLD_PID" || true
        sleep 3
        ps -p "$OLD_PID" >/dev/null 2>&1 && kill -9 "$OLD_PID" || true
    fi
    rm -f "$PID_FILE"
fi

# -------------------- Start service --------------------
echo "[INFO] Starting Gunicorn service..."

nohup "$PYTHON_BIN" -m gunicorn \
    -c "$BASE_DIR/gunicorn_conf.py" \
    "$APP_MODULE" \
    >> "$SERVICE_LOG" 2>&1 &

NEW_PID=$!
sleep 2

if ps -p "$NEW_PID" >/dev/null 2>&1; then
    echo "$NEW_PID" > "$PID_FILE"
    echo "[INFO] Service started successfully (PID=$NEW_PID)"
else
    echo "[ERROR] Service failed to start"
    exit 1
fi
