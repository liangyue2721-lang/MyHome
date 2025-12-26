@echo off
setlocal enabledelayedexpansion

echo ==================================================
echo TEST START : Stock Data Validation
echo ==================================================
echo.

REM ==================================================
REM PHASE 1 - stock_service.py API validation
REM ==================================================
echo ========== PHASE 1 : stock_service.py APIs ==========
echo.

echo [SERVICE] Health Check
curl -s http://localhost:8000/health || echo [FAIL] /health
echo.

echo [SERVICE] /stock/realtime
curl -s -X POST http://localhost:8000/stock/realtime ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://push2.eastmoney.com/api/qt/stock/get?secid=1.601138\"}" ^
  || echo [FAIL] /stock/realtime
echo.

echo [SERVICE] /etf/realtime
curl -s -X POST http://localhost:8000/etf/realtime ^
  -H "Content-Type: application/json" ^
  -d "{\"url\":\"https://push2.eastmoney.com/api/qt/stock/get?secid=1.510300\"}" ^
  || echo [FAIL] /etf/realtime
echo.

echo [SERVICE] /stock/kline
curl -s -X POST http://localhost:8000/stock/kline ^
  -H "Content-Type: application/json" ^
  -d "{\"secid\":\"1.601138\",\"ndays\":5}" ^
  || echo [FAIL] /stock/kline
echo.

REM ==================================================
REM PHASE 2 - standalone python scripts
REM ==================================================
echo ========== PHASE 2 : Standalone Scripts ==========
echo.

echo [SCRIPT] fetch_stock_realtime.py
python fetch_stock_realtime.py "https://push2.eastmoney.com/api/qt/stock/get?secid=1.601138" ^
  || echo [FAIL] fetch_stock_realtime.py
echo.

echo [SCRIPT] etf_realtime_fetcher.py
python etf_realtime_fetcher.py "https://push2.eastmoney.com/api/qt/stock/get?secid=1.510300" ^
  || echo [FAIL] etf_realtime_fetcher.py
echo.

echo [SCRIPT] eastmoney_kline_fetcher.py
python eastmoney_kline_fetcher.py 601138 1 ^
  || echo [FAIL] eastmoney_kline_fetcher.py
echo.

echo [SCRIPT] hybrid_kline_trends.py
python hybrid_kline_trends.py 1.601138 5 ^
  || echo [FAIL] hybrid_kline_trends.py
echo.

echo [SCRIPT] kline_playwright.py
python kline_playwright.py ^
  || echo [FAIL] kline_playwright.py
echo.

REM ==================================================
REM FINISH
REM ==================================================
echo ==================================================
echo TEST END
echo ==================================================
pause
