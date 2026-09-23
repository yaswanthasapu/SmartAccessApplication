param(
    [string]$AvdName = "Pixel_8a",
    [switch]$ColdBoot
)

$ErrorActionPreference = "Stop"
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$emulatorPath = Join-Path $sdkRoot "emulator\emulator.exe"

if (-not (Test-Path -LiteralPath $emulatorPath)) {
    throw "Android emulator not found at $emulatorPath"
}

$arguments = @("-avd", $AvdName, "-no-snapshot-save")
if ($ColdBoot) { $arguments += "-no-snapshot-load" }

Start-Process -FilePath $emulatorPath -ArgumentList $arguments -WindowStyle Hidden
Write-Host "Waiting for $AvdName to boot..."
& adb wait-for-device

do {
    Start-Sleep -Seconds 2
    $bootCompleted = (& adb shell getprop sys.boot_completed 2>$null).Trim()
} until ($bootCompleted -eq "1")

Write-Host "Emulator is ready."

