@echo off
echo ---------------------------------------
echo Stopping stock service in WSL2...
echo ---------------------------------------

wsl -d Ubuntu -- pkill -f gunicorn

echo Done.
pause
