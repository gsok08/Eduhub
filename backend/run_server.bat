@echo off
title EduHub AI Flask Backend Server
echo ========================================================
echo Starting EduHub AI Flask Backend Server...
echo ========================================================
cd /d "%~dp0"

echo Installing required Python packages...
pip install -r requirements.txt

echo.
echo Starting Flask server on 0.0.0.0:5000...
python app.py
pause
