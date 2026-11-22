param([string[]]$Tasks = @('compileJava'))
$ErrorActionPreference = 'Stop'
$jdkFolder = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '../.millenaire-tools') -Directory -Filter 'jdk-17*' | Select-Object -First 1
if (-not $jdkFolder) { throw 'Local JDK 17 was not found.' }
$previousJava = $env:JAVA_HOME
$previousGradle = $env:GRADLE_USER_HOME
try {
    $env:JAVA_HOME = $jdkFolder.FullName
    $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle-user-home'
    Push-Location $PSScriptRoot
    try { & ./gradlew.bat @Tasks --console=plain; $buildExit = $LASTEXITCODE }
    finally { Pop-Location }
} finally {
    $env:JAVA_HOME = $previousJava
    $env:GRADLE_USER_HOME = $previousGradle
}
exit $buildExit
