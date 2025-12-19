@echo off

REM ===========================================================
REM  Test script for the optimized stock service.
REM  This script sends sample POST requests to your local
REM  FastAPI endpoints and echoes both the request and response.
REM  It properly escapes special characters (& and |) so the
REM  commands run without being split by the Windows shell.
REM  Ensure the service is running on http://localhost:8000.
REM ===========================================================

REM ---- Stock realtime test ----
echo Sending stock realtime request...
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f43,f60,f57,f58&secid=1.688256&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=|0|1|0|web&dect=1"}
curl -X POST -H "Content-Type: application/json" -d "{\"url\":\"https://push2.eastmoney.com/api/qt/stock/get?invt=2^&fltt=1^&fields=f43,f60,f57,f58^&secid=1.688256^&ut=fa5fd1943c7b386f172d6893dbfba10b^&wbp2u=^|0^|1^|0^|web^&dect=1\"}" http://localhost:8000/stock/realtime
echo.

REM ---- ETF realtime test ----
echo Sending ETF realtime request...
echo {"url":"https://push2.eastmoney.com/api/qt/stock/get?invt=2&fltt=1&fields=f43,f60&secid=1.510050&ut=fa5fd1943c7b386f172d6893dbfba10b&wbp2u=|0|1|0|web&dect=1"}
curl -X POST -H "Content-Type: application/json" -d "{\"url\":\"https://push2.eastmoney.com/api/qt/stock/get?invt=2^&fltt=1^&fields=f43,f60^&secid=1.510050^&ut=fa5fd1943c7b386f172d6893dbfba10b^&wbp2u=^|0^|1^|0^|web^&dect=1\"}" http://localhost:8000/etf/realtime
echo.

REM ---- 5-day Kline test ----
echo Sending 5-day Kline request...
echo {"secid":"688256","ndays":5}
curl -X POST -H "Content-Type: application/json" -d "{\"secid\":\"688256\",\"ndays\":5}" http://localhost:8000/stock/kline
echo.

echo Tests complete. Press any key to exit.
pause >nul
