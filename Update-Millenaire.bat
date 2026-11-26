@echo off
setlocal
cd /d "%~dp0" || exit /b 1

echo ==========================================
echo   MILLENAIRE - UPDATE, TEST, BUILD, INSTALL
echo ==========================================
echo.

set "CURRENT_BRANCH="
for /f "delims=" %%B in ('git branch --show-current') do set "CURRENT_BRANCH=%%B"
if not "%CURRENT_BRANCH%"=="fix/villager-door-access-and-village-territory" (
    echo ERROR: Incorrect Git branch: %CURRENT_BRANCH%
    echo Expected: fix/villager-door-access-and-village-territory
    pause
    exit /b 1
)

echo [1/2] Updating Millenaire+ from GitHub...
git pull --ff-only
if errorlevel 1 (
    echo ERROR: Git update failed. Build and installation cancelled.
    pause
    exit /b 1
)

echo.
echo [2/2] Validating, building and installing Millenaire+...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-Millenaire.ps1"
set "RESULT=%ERRORLEVEL%"

echo.
if not "%RESULT%"=="0" (
    echo ERROR: Update failed with code %RESULT%.
    echo Check the error above. Installation is skipped if validation or build fails.
) else (
    echo Update completed. You can start ATLauncher.
)
echo.
pause
exit /b %RESULT%
