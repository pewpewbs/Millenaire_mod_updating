$ErrorActionPreference = 'Stop'

# Run Update-Millenaire.bat from the repository root.
# No Git operations here: the BAT updates this script before launching it.
$root = $PSScriptRoot
$project = Join-Path $root 'millenaire-1.18.2-forge'
$validator = Join-Path $project 'tools\validate_production.py'
$buildScript = Join-Path $project 'build-local.ps1'
$properties = Join-Path $project 'gradle.properties'
$modsPath = Join-Path $env:APPDATA 'ATLauncher\instances\Codefication1182\mods'
$backupPath = Join-Path $root 'mod-backups'

function Assert-GameClosed {
    # Never replace the JAR while this ATLauncher instance is running.
    $instancePath = (Split-Path -Parent $modsPath).Replace('/', '\').TrimEnd('\')
    $running = @(Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" |
        Where-Object { $_.CommandLine -and $_.CommandLine.Replace('/', '\').IndexOf($instancePath, [StringComparison]::OrdinalIgnoreCase) -ge 0 })
    if ($running.Count -gt 0) {
        throw 'Close Minecraft (Codefication1182) before updating the mod.'
    }
}

Write-Host '=== MILLENAIRE AUTO UPDATE ===' -ForegroundColor Cyan
Write-Host "Minecraft mods: $modsPath"

if (-not (Test-Path -LiteralPath $modsPath -PathType Container)) {
    throw "Minecraft mods folder not found: $modsPath"
}
foreach ($file in @($validator, $buildScript, $properties)) {
    if (-not (Test-Path -LiteralPath $file -PathType Leaf)) {
        throw "Required project file not found: $file"
    }
}
Assert-GameClosed

# Resolve the exact artifact from the same properties used by Gradle.
$modId = $null
$version = $null
foreach ($line in Get-Content -LiteralPath $properties) {
    if ($line -match '^\s*mod_id\s*=\s*([a-z0-9_]+)\s*$') { $modId = $Matches[1] }
    if ($line -match '^\s*mod_version\s*=\s*([A-Za-z0-9][A-Za-z0-9.+_-]*)\s*$') { $version = $Matches[1] }
}
if ($modId -ne 'millenaire' -or -not $version) {
    throw 'Unable to resolve the Millenaire JAR version from gradle.properties.'
}
$jarName = "$modId-$version.jar"
$jarPath = Join-Path $project "build\libs\$jarName"
$destination = Join-Path $modsPath $jarName

$jdkFolder = Get-ChildItem -LiteralPath (Join-Path $root '.millenaire-tools') -Directory -Filter 'jdk-17*' |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') -PathType Leaf } |
    Select-Object -First 1
if (-not $jdkFolder) { throw 'Local JDK 17 was not found.' }
$python = Get-Command python -CommandType Application -ErrorAction Stop | Select-Object -First 1

# Run the full acceptance suite with the same Java 17 used for the Forge build.
# Keep the Gradle invocation in build-local.ps1, exactly once.
$oldJava = $env:JAVA_HOME
$oldPath = $env:PATH
try {
    $env:JAVA_HOME = $jdkFolder.FullName
    $env:PATH = (Join-Path $jdkFolder.FullName 'bin') + ';' + $oldPath
    Write-Host "`n[1/4] Running production validation and regression tests..." -ForegroundColor Yellow
    Push-Location $project
    try {
        & $python.Source -u $validator
        $validationExit = $LASTEXITCODE
    } finally { Pop-Location }
    if ($validationExit -ne 0) {
        throw "Validation failed (exit code $validationExit). Installation cancelled."
    }

    Write-Host "`n[2/4] Building Millenaire with Forge..." -ForegroundColor Yellow
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $buildScript -Tasks build
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) {
        throw "Gradle build failed (exit code $buildExit). Installation cancelled."
    }
} finally {
    $env:JAVA_HOME = $oldJava
    $env:PATH = $oldPath
}
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Built JAR not found: $jarPath"
}

# Verify the archive before changing the installed mods.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    if ($null -eq $archive.GetEntry('META-INF/mods.toml')) {
        throw "Built archive has no Forge mod metadata: $jarPath"
    }
} finally { $archive.Dispose() }

Assert-GameClosed
$stagedJar = Join-Path $modsPath ('.millenaire-update-' + [Guid]::NewGuid().ToString('N') + '.tmp')
$moved = New-Object System.Collections.ArrayList
$installed = $false
try {
    Write-Host "`n[3/4] Copying and verifying JAR..." -ForegroundColor Yellow
    Copy-Item -LiteralPath $jarPath -Destination $stagedJar
    $sourceHash = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash
    $stagedHash = (Get-FileHash -LiteralPath $stagedJar -Algorithm SHA256).Hash
    if ($sourceHash -ne $stagedHash) {
        throw 'JAR integrity check failed. Installation cancelled.'
    }

    New-Item -ItemType Directory -Path $backupPath -Force | Out-Null
    $timestamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
    $oldMods = @(Get-ChildItem -LiteralPath $modsPath -Filter 'millenaire-*.jar' -File)
    Assert-GameClosed

    foreach ($oldMod in $oldMods) {
        $backup = Join-Path $backupPath "$timestamp-$($oldMod.Name)"
        Move-Item -LiteralPath $oldMod.FullName -Destination $backup -ErrorAction Stop
        [void]$moved.Add(@{ Original = $oldMod.FullName; Backup = $backup })
    }

    Write-Host '[4/4] Installing new JAR...' -ForegroundColor Yellow
    Move-Item -LiteralPath $stagedJar -Destination $destination -ErrorAction Stop
    $installed = $true
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $sourceHash) {
        throw 'Installed JAR integrity check failed.'
    }
} catch {
    $failure = $_
    # Roll back only our installation and the files we actually moved.
    if ($installed -and (Test-Path -LiteralPath $destination -PathType Leaf)) {
        try { Remove-Item -LiteralPath $destination -Force -ErrorAction Stop }
        catch { Write-Warning "Could not remove failed installation: $destination" }
    }
    foreach ($entry in $moved) {
        try {
            if (Test-Path -LiteralPath $entry.Backup -PathType Leaf) {
                if (Test-Path -LiteralPath $entry.Original) {
                    throw "Restore destination already exists: $($entry.Original)"
                }
                Move-Item -LiteralPath $entry.Backup -Destination $entry.Original -ErrorAction Stop
            }
        } catch { Write-Warning "Rollback needs manual attention: $($entry.Backup) -> $($entry.Original): $_" }
    }
    throw $failure
} finally {
    if (Test-Path -LiteralPath $stagedJar -PathType Leaf) {
        Remove-Item -LiteralPath $stagedJar -Force -ErrorAction SilentlyContinue
    }
}

Write-Host "`nSUCCESS! Millenaire installed." -ForegroundColor Green
Write-Host "Location: $destination"
Write-Host "SHA256: $sourceHash"
Write-Host "Previous versions: $backupPath"
exit 0
