$ErrorActionPreference = "Stop"

# Millenaire 1.18.2: build and install into the dedicated ATLauncher instance.
# This script should be launched with Update-Millenaire.bat.
$root = $PSScriptRoot
$buildScript = Join-Path $root "millenaire-1.18.2-forge\build-local.ps1"
$jarPath = Join-Path $root "millenaire-1.18.2-forge\build\libs\millenaire-8.1.2-port.0.jar"

# Change this only if you move your ATLauncher instance.
$modsPath = "C:\Users\sviat\AppData\Roaming\ATLauncher\instances\Codefication1182\mods"
$backupPath = Join-Path $root "mod-backups"

Write-Host "=== MILLENAIRE AUTO UPDATE ===" -ForegroundColor Cyan
Write-Host "Minecraft mods: $modsPath"

if (-not (Test-Path -LiteralPath $modsPath -PathType Container)) {
    throw "Minecraft mods folder not found: $modsPath"
}
if (-not (Test-Path -LiteralPath $buildScript -PathType Leaf)) {
    throw "Build script not found: $buildScript"
}

Write-Host "`n[1/4] Building Millenaire..." -ForegroundColor Yellow

# IMPORTANT: build-local.ps1 calls 'exit' with Gradle's result. Run it as a
# separate PowerShell process, so it cannot terminate this installer script.
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript -Tasks build
$buildExit = $LASTEXITCODE
if ($buildExit -ne 0) {
    throw "Gradle build failed (exit code $buildExit). Installation cancelled."
}

if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Built JAR not found: $jarPath"
}
Write-Host "[2/4] Build successful." -ForegroundColor Green

$jarName = Split-Path $jarPath -Leaf
$destination = Join-Path $modsPath $jarName
$stagedJar = Join-Path $modsPath ".millenaire-update.tmp"

# Stage and validate the new JAR *before* moving any installed mod.
Write-Host "[3/4] Copying and verifying JAR..." -ForegroundColor Yellow
Copy-Item -LiteralPath $jarPath -Destination $stagedJar -Force
$sourceHash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
$stagedHash = (Get-FileHash -LiteralPath $stagedJar -Algorithm SHA256).Hash
if ($sourceHash -ne $stagedHash) {
    Remove-Item -LiteralPath $stagedJar -Force -ErrorAction SilentlyContinue
    throw "JAR integrity check failed. Installation cancelled."
}

New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
$oldMods = @(Get-ChildItem -LiteralPath $modsPath -Filter "millenaire-*.jar" -File)
$moved = New-Object System.Collections.ArrayList

try {
    foreach ($oldMod in $oldMods) {
        $backup = Join-Path $backupPath "$timestamp-$($oldMod.Name)"
        Move-Item -LiteralPath $oldMod.FullName -Destination $backup -ErrorAction Stop
        [void]$moved.Add(@{ Original = $oldMod.FullName; Backup = $backup })
    }

    Write-Host "[4/4] Installing new JAR..." -ForegroundColor Yellow
    Move-Item -LiteralPath $stagedJar -Destination $destination -ErrorAction Stop
} catch {
    # If installation fails, put each former mod back where it came from.
    foreach ($entry in $moved) {
        if (Test-Path -LiteralPath $entry.Backup -PathType Leaf) {
            Move-Item -LiteralPath $entry.Backup -Destination $entry.Original -Force -ErrorAction SilentlyContinue
        }
    }
    throw
} finally {
    if (Test-Path -LiteralPath $stagedJar -PathType Leaf) {
        Remove-Item -LiteralPath $stagedJar -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "`nSUCCESS! Millenaire installed." -ForegroundColor Green
Write-Host "Location: $destination"
Write-Host "Previous versions: $backupPath"
