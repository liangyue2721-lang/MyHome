@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ==========================================================
REM test_stock_service.bat
REM 全面验证 stock_service.py 及关联脚本功能（UTF-8 控制台）
REM ==========================================================

REM --------------------
REM 配置
REM --------------------
set "HOST=localhost"
set "PORT=8000"
set "BASE_URL=http://%HOST%:%PORT%"

REM --------------------
REM 统计变量
REM --------------------
set "PASS_COUNT=0"
set "FAIL_COUNT=0"

REM ==========================================================
REM 1) Health Check
REM ==========================================================

echo.
echo ----------------------------------------------------------
echo [TEST] Health Check
echo ----------------------------------------------------------

curl -s "%BASE_URL%/health" > health_out.json

set "LINES=0"
for /f %%A in ('findstr /R /V "^$" health_out.json ^| find /C /V ""') do (
    set "LINES=%%A"
)

if !LINES! GTR 0 (
    echo [PASS] Health returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Health returned no output
    set /a FAIL_COUNT+=1
)

echo Output:
type health_out.json
del health_out.json

REM ==========================================================
REM 2) Stock Realtime
REM ==========================================================

echo.
echo ----------------------------------------------------------
echo [TEST] Stock Realtime
echo ----------------------------------------------------------

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f58,f734,f107,f57,f43,f59,f169,f301,f60,f170,f152,f177,f111,f46,f44,f45,f47,f260,f48,f261,f279,f277,f278,f288,f19,f17,f531,f15,f13,f11,f20,f18,f16,f14,f12,f39,f37,f35,f33,f31,f40,f38,f36,f34,f32,f211,f212,f213,f214,f215,f210,f209,f208,f207,f206,f161,f49,f171,f50,f86,f84,f85,f168,f108,f116,f167,f164,f162,f163,f92,f71,f117,f292,f51,f52,f191,f192,f262,f294,f295,f269,f270,f256,f257,f285,f286,f748,f747&secid=1.601138&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=|0|1|0|web&dect=1"} > stock_realtime_req.json

curl -s -H "Content-Type: application/json" -d @stock_realtime_req.json "%BASE_URL%/stock/realtime" > stock_realtime_out.json

set "LINES=0"
for /f %%A in ('findstr /R /V "^$" stock_realtime_out.json ^| find /C /V ""') do (
    set "LINES=%%A"
)

if !LINES! GTR 0 (
    echo [PASS] Stock Realtime API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Stock Realtime API returned no output
    set /a FAIL_COUNT+=1
)

echo Output:
type stock_realtime_out.json
del stock_realtime_out.json
del stock_realtime_req.json

REM ==========================================================
REM 3) ETF Realtime
REM ==========================================================

echo.
echo ----------------------------------------------------------
echo [TEST] ETF Realtime
echo ----------------------------------------------------------

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.510050"} > etf_realtime_req.json

curl -s -H "Content-Type: application/json" -d @etf_realtime_req.json "%BASE_URL%/etf/realtime" > etf_realtime_out.json

set "LINES=0"
for /f %%A in ('findstr /R /V "^$" etf_realtime_out.json ^| find /C /V ""') do (
    set "LINES=%%A"
)

if !LINES! GTR 0 (
    echo [PASS] ETF Realtime API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] ETF Realtime API returned no output
    set /a FAIL_COUNT+=1
)

echo Output:
type etf_realtime_out.json
del etf_realtime_out.json
del etf_realtime_req.json

REM ==========================================================
REM 4) Stock Kline
REM ==========================================================

echo.
echo ----------------------------------------------------------
echo [TEST] Stock Kline
echo ----------------------------------------------------------

echo {"secid":"1.600000","ndays":5} > kline_req.json

curl -s -H "Content-Type: application/json" -d @kline_req.json "%BASE_URL%/stock/kline" > kline_out.json

set "LINES=0"
for /f %%A in ('findstr /R /V "^$" kline_out.json ^| find /C /V ""') do (
    set "LINES=%%A"
)

if !LINES! GTR 0 (
    echo [PASS] Stock Kline API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Stock Kline API returned no output
    set /a FAIL_COUNT+=1
)

echo Output:
type kline_out.json
del kline_out.json
del kline_req.json

REM ==========================================================
REM 汇总
REM ==========================================================

echo.
echo ==========================================================
echo Test Summary:
echo  Passed: !PASS_COUNT!
echo  Failed: !FAIL_COUNT!
echo ==========================================================
pause
endlocal
