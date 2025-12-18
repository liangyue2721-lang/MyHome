#!/bin/bash
# start_service.sh

cd $(dirname $0)

# Kill existing instance if any
pkill -f "uvicorn stock_service:app" || true

# Install deps if needed (optional)
# pip install -r requirements.txt

# Start in background
nohup uvicorn stock_service:app --host 0.0.0.0 --port 8000 > service.log 2>&1 &

echo "Stock Service started on port 8000"
