@echo off
REM ==========================================================
REM test_stock_service.bat
REM 全面验证 stock_service.py 及关联脚本功能
REM ==========================================================

setlocal EnableDelayedExpansion

REM --------------------
REM 配置
REM --------------------
set HOST=localhost
set PORT=8000
set BASE_URL=http://%HOST%:%PORT%

REM --------------------
REM 统计结果
REM --------------------
set PASS_COUNT=0
set FAIL_COUNT=0

REM ==========================================================
REM 函数: 调用 URL 并检查非空输出
REM 参数1: URL
REM 参数2: 描述
REM ==========================================================
:CHECK_CURL
set "URL=%~1"
set "DESC=%~2"

echo.
echo ----------------------------------------------------------
echo [TEST] %DESC%
echo ----------------------------------------------------------

REM 请求并保存输出
curl -s "%URL%" > tmp_out.json

REM 清除旧变量
set "LINES="

REM 统计非空输出行数
for /f "usebackq" %%A in (`type tmp_out.json ^| findstr /r /v "^$" ^| find /c /v ""`) do (
    set LINES=%%A
)

if defined LINES (
    echo [PASS] %DESC% returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] %DESC% returned no output
    set /a FAIL_COUNT+=1
)

echo Output:
type tmp_out.json
echo.

del tmp_out.json

goto :EOF

REM ==========================================================
REM 1) Health Check
REM ==========================================================
call :CHECK_CURL "%BASE_URL%/health" "Health Check"

REM ==========================================================
REM 2) Stock Realtime
REM ==========================================================

REM 写入请求 JSON 到临时文件
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > tmp.json

curl -s -H "Content-Type: application/json" -d @tmp.json "%BASE_URL%/stock/realtime" > tmp_out.json

set "LINES="
for /f "usebackq" %%A in (`type tmp_out.json ^| findstr /r /v "^$" ^| find /c /v ""`) do (
    set LINES=%%A
)

if defined LINES (
    echo [PASS] Stock Realtime API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Stock Realtime API returned no output
    set /a FAIL_COUNT+=1
)

echo tmp_out.json:
type tmp_out.json
del tmp_out.json
del tmp.json

REM ==========================================================
REM 3) ETF Realtime
REM ==========================================================

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.510050"} > tmp.json

curl -s -H "Content-Type: application/json" -d @tmp.json "%BASE_URL%/etf/realtime" > tmp_out.json

set "LINES="
for /f "usebackq" %%A in (`type tmp_out.json ^| findstr /r /v "^$" ^| find /c /v ""`) do (
    set LINES=%%A
)

if defined LINES (
    echo [PASS] ETF Realtime API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] ETF Realtime API returned no output
    set /a FAIL_COUNT+=1
)

echo tmp_out.json:
type tmp_out.json
del tmp_out.json
del tmp.json

REM ==========================================================
REM 4) Stock Kline
REM ==========================================================

echo {"secid":"1.600000","ndays":5} > tmp.json

curl -s -H "Content-Type: application/json" -d @tmp.json "%BASE_URL%/stock/kline" > tmp_out.json

set "LINES="
for /f "usebackq" %%A in (`type tmp_out.json ^| findstr /r /v "^$" ^| find /c /v ""`) do (
    set LINES=%%A
)

if defined LINES (
    echo [PASS] Stock Kline API returned !LINES! lines
    set /a PASS_COUNT+=1
) else (
    echo [FAIL] Stock Kline API returned no output
    set /a FAIL_COUNT+=1
)

echo tmp_out.json:
type tmp_out.json
del tmp_out.json
del tmp.json

REM ==========================================================
REM 结果统计
REM ==========================================================
echo.
echo ==========================================================
echo Test Summary:
echo Passed: !PASS_COUNT!
echo Failed: !FAIL_COUNT!
echo ==========================================================
pause

endlocal
