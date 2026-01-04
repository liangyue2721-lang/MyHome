@echo off
@if "%~1" neq "__child" (
    cmd /k "%~f0" __child
    exit /b
)

chcp 65001 >nul
setlocal EnableDelayedExpansion

REM ================= CONFIG =================
set "HOST=localhost"
set "PORT=8000"
set "BASE_URL=http://%HOST%:%PORT%"
set "DEBUG=1"

set PASS_COUNT=0
set FAIL_COUNT=0

goto :MAIN

REM ================= FUNCTION =================
:check_json_field
REM %1=file %2=field
findstr /C:"\"%2\"" "%1" >nul
if errorlevel 1 (
    echo     [FAIL] Missing JSON field: %2
    set /a FAIL_COUNT+=1
    exit /b 1
)
exit /b 0

REM ================= MAIN =================
:MAIN

echo.
echo ==========================================================
echo   Stock Service Self-Test (Windows CMD)
echo ==========================================================
echo   Target : %BASE_URL%
echo ==========================================================
echo.

REM ==========================================================
REM 1/7 Health
REM ==========================================================
echo ----------------------------------------------------------
echo [1/7] Health
echo ----------------------------------------------------------

curl -s "%BASE_URL%/health" -o health.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    call :check_json_field health.json status || goto :h_done
    call :check_json_field health.json browser || goto :h_done
    echo   [PASS] /health
    set /a PASS_COUNT+=1
) else (
    echo   [FAIL] /health http=%STATUS%
    type health.json
    set /a FAIL_COUNT+=1
)
:h_done
del health.json status.txt

REM ==========================================================
REM 2/7 Stock Realtime
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [2/7] Stock Realtime
echo ----------------------------------------------------------

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.601138"} > req.json
curl -s "%BASE_URL%/stock/realtime" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    call :check_json_field out.json stockCode || goto :sr_done
    call :check_json_field out.json price || goto :sr_done
    echo   [PASS] /stock/realtime
    set /a PASS_COUNT+=1
) else (
    echo   [FAIL] /stock/realtime http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
:sr_done
del req.json out.json status.txt

REM ==========================================================
REM 3/7 ETF Realtime
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [3/7] ETF Realtime
echo ----------------------------------------------------------

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.510050"} > req.json
curl -s "%BASE_URL%/etf/realtime" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    call :check_json_field out.json price || goto :etf_done
    echo   [PASS] /etf/realtime
    set /a PASS_COUNT+=1
) else (
    echo   [FAIL] /etf/realtime http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
:etf_done
del req.json out.json status.txt

REM ==========================================================
REM 4/7 Stock Kline
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [4/7] Stock Kline
echo ----------------------------------------------------------

echo {"secid":"1.600000","ndays":5} > req.json
curl -s "%BASE_URL%/stock/kline" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    findstr /C:"[" out.json >nul && (
        echo   [PASS] /stock/kline
        set /a PASS_COUNT+=1
    ) || (
        echo   [FAIL] /stock/kline invalid array
        type out.json
        set /a FAIL_COUNT+=1
    )
) else (
    echo   [FAIL] /stock/kline http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
del req.json out.json status.txt

REM ==========================================================
REM 5/7 Stock Kline Range
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [5/7] Stock Kline Range
echo ----------------------------------------------------------

echo {"secid":"1.600000","beg":"20240101","end":"20240201"} > req.json
curl -s "%BASE_URL%/stock/kline/range" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    findstr /C:"[" out.json >nul && (
        echo   [PASS] /stock/kline/range
        set /a PASS_COUNT+=1
    ) || (
        echo   [FAIL] /stock/kline/range invalid array
        type out.json
        set /a FAIL_COUNT+=1
    )
) else (
    echo   [FAIL] /stock/kline/range http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
del req.json out.json status.txt

REM ==========================================================
REM 6/7 Stock Kline US
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [6/7] Stock Kline US
echo ----------------------------------------------------------

echo {"secid":"MSFT","market":"105"} > req.json
curl -s "%BASE_URL%/stock/kline/us" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    findstr /C:"[" out.json >nul && (
        echo   [PASS] /stock/kline/us
        set /a PASS_COUNT+=1
    ) || (
        echo   [FAIL] /stock/kline/us invalid array
        type out.json
        set /a FAIL_COUNT+=1
    )
) else (
    echo   [FAIL] /stock/kline/us http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
del req.json out.json status.txt

REM ==========================================================
REM 7/7 Proxy JSON
REM ==========================================================
echo.
echo ----------------------------------------------------------
echo [7/7] Proxy JSON
echo ----------------------------------------------------------

echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?secid=1.600000"} > req.json
curl -s "%BASE_URL%/proxy/json" -H "Content-Type: application/json" -d @req.json -o out.json -w "%{http_code}" > status.txt
set /p STATUS=<status.txt

if "%STATUS%"=="200" (
    call :check_json_field out.json data || goto :px_done
    echo   [PASS] /proxy/json
    set /a PASS_COUNT+=1
) else (
    echo   [FAIL] /proxy/json http=%STATUS%
    type out.json
    set /a FAIL_COUNT+=1
)
:px_done
del req.json out.json status.txt

REM ==========================================================
REM SUMMARY
REM ==========================================================
echo.
echo ==========================================================
echo   Self-Test Summary
echo ----------------------------------------------------------
echo   PASSED : %PASS_COUNT%
echo   FAILED : %FAIL_COUNT%
echo ==========================================================
echo.

pause
endlocal
