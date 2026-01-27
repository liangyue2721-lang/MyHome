@echo off
@if "%~1" neq "__child" (
    cmd /k "%~f0" __child
    exit /b
)

chcp 65001 >nul
setlocal EnableDelayedExpansion

set BASE_URL=http://localhost:8000

echo ==========================================
echo Stock Service Self-Test (Windows CMD)
echo ==========================================
echo Target: %BASE_URL%
echo.

REM =================================================
echo [1/7] Health
echo -------------------------------------------------
echo Request:
echo   GET %BASE_URL%/health
echo.

curl -s %BASE_URL%/health > test.tmp
if errorlevel 1 goto FAIL1

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL1
echo Result: PASS
goto NEXT1
:FAIL1
echo Result: FAIL
:NEXT1
del test.tmp 2>nul
echo.

REM =================================================
echo [2/7] Stock Realtime
echo -------------------------------------------------
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/stock/realtime > test.tmp
if errorlevel 1 goto FAIL2

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL2
echo Result: PASS
goto NEXT2
:FAIL2
echo Result: FAIL
:NEXT2
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [3/7] ETF Realtime
echo -------------------------------------------------
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.510300"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/etf/realtime > test.tmp
if errorlevel 1 goto FAIL3

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL3
echo Result: PASS
goto NEXT3
:FAIL3
echo Result: FAIL
:NEXT3
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [4/7] Stock Kline
echo -------------------------------------------------
echo {"secid":"600000","ndays":5} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/stock/kline > test.tmp
if errorlevel 1 goto FAIL4

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL4
echo Result: PASS
goto NEXT4
:FAIL4
echo Result: FAIL
:NEXT4
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [5/7] Stock Kline Range
echo -------------------------------------------------
echo {"secid":"600000","beg":"20240101","end":"20240201"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/stock/kline/range > test.tmp
if errorlevel 1 goto FAIL5

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL5
echo Result: PASS
goto NEXT5
:FAIL5
echo Result: FAIL
:NEXT5
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [6/7] Stock Kline US
echo -------------------------------------------------
echo {"secid":"AAPL","market":"105"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/stock/kline/us > test.tmp
if errorlevel 1 goto FAIL6

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL6
echo Result: PASS
goto NEXT6
:FAIL6
echo Result: FAIL
:NEXT6
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [7/7] Proxy JSON
echo -------------------------------------------------
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/proxy/json > test.tmp
if errorlevel 1 goto FAIL7

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL7
echo Result: PASS
goto NEXT7
:FAIL7
echo Result: FAIL
:NEXT7
del test.tmp body.json 2>nul
echo.

REM =================================================
echo [8/8] Stock Ticks
echo -------------------------------------------------
echo {"secid":"1.600000"} > body.json

echo Request:
type body.json
echo.

curl -s -X POST -H "Content-Type: application/json" -d @body.json %BASE_URL%/stock/ticks > test.tmp
if errorlevel 1 goto FAIL8

echo Response:
type test.tmp
echo.

for %%A in (test.tmp) do if %%~zA EQU 0 goto FAIL8
echo Result: PASS
goto NEXT8
:FAIL8
echo Result: FAIL
:NEXT8
del test.tmp body.json 2>nul
echo.

echo ==========================================
echo Self-Test Finished
echo ==========================================
pause
