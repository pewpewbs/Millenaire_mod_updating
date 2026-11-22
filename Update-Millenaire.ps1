
$ErrorActionPreference = "Stop"

# ==========================================
# MILLENAIRE: BUILD AND INSTALL
# ==========================================

# Коренева папка проєкту
$root = $PSScriptRoot

# Скрипт компіляції
$buildScript = Join-Path $root "millenaire-1.18.2-forge\build-local.ps1"

# Готовий JAR
$jarPath = Join-Path $root "millenaire-1.18.2-forge\build\libs\millenaire-8.1.2-port.0.jar"

# ПАПКА MINECRAFT — ЗМІНИ, ЯКЩО ПОТРІБНО
$modsPath = "C:\Users\sviat\AppData\Roaming\ATLauncher\instances\Codefication1182\mods"

# Резервні копії попередніх версій
$backupPath = Join-Path $root "mod-backups"

Write-Host "=== MILLENAIRE AUTO UPDATE ===" -ForegroundColor Cyan

# 1. Перевіряємо папку Minecraft
if (-not (Test-Path $modsPath -PathType Container)) {
    throw "Minecraft mods folder not found: $modsPath"
}

# 2. Компілюємо мод
Write-Host "`n[1/4] Building Millenaire..." -ForegroundColor Yellow

& $buildScript -Tasks build

if ($LASTEXITCODE -ne 0) {
    throw "Build failed. Installation cancelled."
}

# 3. Перевіряємо готовий JAR
if (-not (Test-Path $jarPath -PathType Leaf)) {
    throw "JAR not found: $jarPath"
}

Write-Host "[2/4] Build successful." -ForegroundColor Green

# 4. Готуємо новий файл перед заміною старого
$jarName = Split-Path $jarPath -Leaf
$stagedJar = Join-Path $modsPath ".millenaire-update.tmp"
$destination = Join-Path $modsPath $jarName

Copy-Item -LiteralPath $jarPath -Destination $stagedJar -Force

# 5. Резервуємо старі версії
Write-Host "[3/4] Backing up old Millenaire versions..." -ForegroundColor Yellow

New-Item -ItemType Directory -Path $backupPath -Force | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$oldMods = @(Get-ChildItem -Path $modsPath -Filter "millenaire-*.jar" -File)

foreach ($oldMod in $oldMods) {
    $backupName = "$timestamp-$($oldMod.Name)"
    Move-Item -LiteralPath $oldMod.FullName `
        -Destination (Join-Path $backupPath $backupName)
}

# 6. Встановлюємо новий мод
Write-Host "[4/4] Installing new JAR..." -ForegroundColor Yellow

Move-Item -LiteralPath $stagedJar -Destination $destination

Write-Host "`nSUCCESS! Millenaire installed." -ForegroundColor Green
Write-Host "Location: $destination"
Write-Host "Backups: $backupPath"
