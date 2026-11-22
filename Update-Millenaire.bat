@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo   MILLENAIRE - BUILD AND INSTALL
echo ==========================================
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-Millenaire.ps1"
set "RESULT=%ERRORLEVEL%"

echo.
if not "%RESULT%"=="0" (
    echo ERROR: Update failed with code %RESULT%.
    echo Old installed mod should remain available if build failed.
) else (
    echo Update completed. You can start ATLauncher.
)
echo.
pause
exit /b %RESULT%
