@echo off
setlocal
chcp 65001 >nul

:: =================配置区域=================
set BASE_URL=http://localhost:8000
:: =========================================

echo.
echo ==================================================
echo   Stock Service Self-Test (Stability Fix)
echo   Target: %BASE_URL%
echo ==================================================
echo.

:: --------------------------------------------------
:: 1. Health Check (GET)
:: --------------------------------------------------
call :StartCase "1/9 Health Check"
curl -s -w "%%{http_code}" "%BASE_URL%/health" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 2. Stock Snapshot (NEW INTERFACE)
:: --------------------------------------------------
call :StartCase "2/9 Stock Snapshot (New)"
echo {"secid": "1.600000"} > body.json
echo Request Body:
type body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/snapshot" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 3. Stock Realtime (Legacy)
:: --------------------------------------------------
call :StartCase "3/9 Stock Realtime (Legacy)"
echo {"url": "https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/realtime" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 4. ETF Realtime
:: --------------------------------------------------
call :StartCase "4/9 ETF Realtime"
echo {"url": "https://push2.eastmoney.com/api/qt/stock/get?secid=1.510300"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/etf/realtime" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 5. Stock Kline (Day)
:: --------------------------------------------------
call :StartCase "5/9 Stock Kline (Day)"
echo {"secid": "1.600000", "ndays": 5} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/kline" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 6. Stock Kline Range
:: --------------------------------------------------
call :StartCase "6/9 Stock Kline Range"
echo {"secid": "1.600000", "beg": "20240101", "end": "20240201"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/kline/range" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 7. Stock Kline US
:: --------------------------------------------------
call :StartCase "7/9 US Stock Kline"
echo {"secid": "AAPL", "market": "105"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/kline/us" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 8. Stock Ticks
:: --------------------------------------------------
call :StartCase "8/9 Stock Ticks"
echo {"secid": "1.600000"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/stock/ticks" -o response.tmp > status.tmp
call :CheckResult
echo.

:: --------------------------------------------------
:: 9. Proxy JSON
:: --------------------------------------------------
call :StartCase "9/9 Proxy JSON"
echo {"url": "https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > body.json
curl -s -w "%%{http_code}" -X POST -H "Content-Type: application/json" -d @body.json "%BASE_URL%/proxy/json" -o response.tmp > status.tmp
call :CheckResult
echo.

echo ==================================================
echo   All Tests Finished.
echo ==================================================
if exist body.json del body.json
if exist response.tmp del response.tmp
if exist status.tmp del status.tmp
pause
exit /b

:: ==================================================
:: 辅助函数
:: ==================================================

:StartCase
echo Testing: %~1
echo --------------------------------------------------
exit /b

:CheckResult
set /p HTTP_CODE=<status.tmp
echo Response Code: %HTTP_CODE%
echo Response Body:
type response.tmp
echo.

if "%HTTP_CODE%"=="200" (
    echo Result: [ PASS ]
) else (
    echo Result: [ FAIL ]
)
if exist body.json del body.json
exit /b