# gunicorn_conf.py
# Production-ish defaults for a single host running multiple Uvicorn workers.
# Adjust based on CPU/RAM and expected QPS.

import os
import multiprocessing

bind = os.getenv("BIND", "0.0.0.0:8000")

# Playwright is CPU+RAM heavy. Start conservatively:
# - If you have 4 vCPU: try workers=2 first.
# - If you have 8 vCPU: try workers=3-4 first.
workers = int(os.getenv("WORKERS", "2"))

worker_class = "uvicorn.workers.UvicornWorker"

# Avoid preload_app with Playwright (browser/context pools must be created per worker).
preload_app = False

# Keep-alive and timeouts
keepalive = int(os.getenv("KEEPALIVE", "5"))
timeout = int(os.getenv("TIMEOUT", "60"))
graceful_timeout = int(os.getenv("GRACEFUL_TIMEOUT", "30"))

# Restart workers periodically to mitigate long-running Chromium memory growth.
max_requests = int(os.getenv("MAX_REQUESTS", "2000"))
max_requests_jitter = int(os.getenv("MAX_REQUESTS_JITTER", "200"))

# Logging
accesslog = "-"   # stdout
errorlog = "-"    # stderr
loglevel = os.getenv("LOGLEVEL", "info")

# If you use /dev/shm issues in containers, set a writable tmp dir:
# worker_tmp_dir = "/dev/shm"
