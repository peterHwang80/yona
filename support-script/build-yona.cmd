@echo off
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-yona.ps1" %*
exit /b %ERRORLEVEL%
