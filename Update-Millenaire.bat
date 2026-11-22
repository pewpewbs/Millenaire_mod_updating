<CodeBlock language="bat" editable>
@echo off
cd /d "%~dp0"


powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Update-Millenaire.ps1"

echo.
pause
</CodeBlock>