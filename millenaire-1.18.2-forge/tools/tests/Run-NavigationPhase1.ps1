param([switch]$BuildForge)

$ErrorActionPreference = "Stop"
$project = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
$tools = Join-Path $project "..\.millenaire-tools"

$jdk = Get-ChildItem -LiteralPath $tools -Directory -Filter "jdk-17*" |
        Select-Object -First 1
if ($null -eq $jdk) {
    throw "Local JDK 17 not found at $tools. Run the repository setup first."
}

$javac = Join-Path $jdk.FullName "bin\javac.exe"
$java = Join-Path $jdk.FullName "bin\java.exe"
$classes = Join-Path $project "build\navigation-phase1-smokes"
New-Item -ItemType Directory -Path $classes -Force | Out-Null

$common = "src/main/java/org/millenaire/common/pathing"
$sources = @(
    "$common/RouteMemory.java",
    "$common/NavigationRequestKey.java",
    "$common/NavigationRateWindow.java",
    "$common/SnapshotSectionPool.java",
    "$common/SnapshotCaptureIndex.java",
    "$common/SnapshotColumnMemo.java",
    "$common/FailedRouteBackoff.java",
    "$common/NavigationGoalDriftWindow.java",
    "$common/atomicstryker/PathSearchExecutor.java"
)
$tests = @(
    "RouteMemorySmoke",
    "NavigationRequestKeySmoke",
    "PathSearchExecutorSmoke",
    "NavigationRateWindowSmoke",
    "SnapshotSectionPoolSmoke",
    "SnapshotCaptureIndexSmoke",
    "SnapshotReuseProtocolSmoke",
    "SnapshotColumnMemoSmoke",
    "FailedRouteBackoffSmoke",
    "FailedRouteBackoffEventsSmoke",
    "NavigationGoalDriftWindowSmoke"
)
Push-Location $project
try {
    Write-Host "[1/3] Compiling standalone Java 17 smoke tests..." -ForegroundColor Cyan
    $compileInputs = @($sources) + @($tests | ForEach-Object { "tools/tests/$_.java" })
    & $javac --release 17 -d $classes @compileInputs
    if ($LASTEXITCODE -ne 0) { throw "javac failed (exit $LASTEXITCODE)" }

    Write-Host "[2/3] Running $($tests.Count) Java smoke tests..." -ForegroundColor Cyan
    foreach ($test in $tests) {
        & $java -cp $classes $test
        if ($LASTEXITCODE -ne 0) { throw "$test FAILED (exit $LASTEXITCODE)" }
    }

    Write-Host "[3/3] Running Python source-wiring regression suites..." -ForegroundColor Cyan
    $py = Get-Command "py" -ErrorAction SilentlyContinue
    if ($null -ne $py) {
        & $py.Source -3 tools/tests/test_navigation_priority.py
        if ($LASTEXITCODE -ne 0) { throw "test_navigation_priority.py failed" }
        & $py.Source -3 tools/tests/test_navigation_optimization_wiring.py
    } else {
        $python = Get-Command "python" -ErrorAction SilentlyContinue
        if ($null -eq $python) { throw "Python 3 is needed to run source-level tests." }
        & $python.Source tools/tests/test_navigation_priority.py
        if ($LASTEXITCODE -ne 0) { throw "test_navigation_priority.py failed" }
        & $python.Source tools/tests/test_navigation_optimization_wiring.py
    }
    if ($LASTEXITCODE -ne 0) { throw "test_navigation_optimization_wiring.py failed" }

    if ($BuildForge) {
        Write-Host "[optional] Building full Forge module..." -ForegroundColor Yellow
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $project "build-local.ps1") -Tasks build
        if ($LASTEXITCODE -ne 0) { throw "Forge build failed (exit $LASTEXITCODE)" }
    }

    Write-Host "PASS: 11 Java smokes and both Python source-level suites." -ForegroundColor Green
    if (-not $BuildForge) {
        Write-Host "Forge compilation and Minecraft playtesting were NOT run." -ForegroundColor Yellow
    }
} finally {
    Pop-Location
}
