param(
    [string]$Device = "",
    [string]$Config = "config/uat1.json",
    [ValidateSet("activate", "deactivate", "cancel-deactivation")]
    [string]$ArmModeAction = "activate"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$configPath = if ([System.IO.Path]::IsPathRooted($Config)) { $Config } else { Join-Path $projectRoot $Config }
if (-not (Test-Path -LiteralPath $configPath)) { throw "Test configuration not found: $configPath" }
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) { throw "adb is not available on PATH." }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw "Maven is not available on PATH." }

$connectedDevices = @(& adb devices | Select-Object -Skip 1 | Where-Object { $_ -match "\sdevice$" })
if ($connectedDevices.Count -eq 0) {
    throw "No ready Android device found. Run scripts/start-emulator.ps1 or connect a phone."
}

try {
    $appiumStatus = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:4723/status" -TimeoutSec 2
} catch {
    $appiumCommand = Get-Command appium.cmd -ErrorAction SilentlyContinue
    if (-not $appiumCommand) { throw "Appium is not running and appium.cmd was not found on PATH." }
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot "reports") | Out-Null
    Start-Process -FilePath $appiumCommand.Source -ArgumentList "--log-timestamp" `
        -RedirectStandardOutput (Join-Path $projectRoot "reports\appium.log") `
        -RedirectStandardError (Join-Path $projectRoot "reports\appium-error.log") `
        -WindowStyle Hidden | Out-Null
    Write-Host "Starting Appium..."
    $appiumReady = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $appiumStatus = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:4723/status" -TimeoutSec 2
            if ($appiumStatus.StatusCode -eq 200) { $appiumReady = $true; break }
        } catch {}
        Start-Sleep -Seconds 2
    }
    if (-not $appiumReady) { throw "Appium did not become ready. Check reports/appium-error.log." }
}

$mavenArguments = @("test", "-DtestConfig=$configPath")
if ($Device) { $mavenArguments += "-Ddevice=$Device" }
$mavenArguments += "-DarmModeAction=$ArmModeAction"
Push-Location $projectRoot
try { & mvn @mavenArguments } finally { Pop-Location }
