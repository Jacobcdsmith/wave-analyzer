# smoke.ps1 — build the APK or capture a screenshot via Roborazzi
# Run from the project root:  .\.claude\skills\run-wave-analyzer\smoke.ps1
#
# Tasks:
#   assembleDebug          - build the APK (default)
#   recordRoborazziDebug   - render composables and save screenshots (no device needed)
#   verifyRoborazziDebug   - compare rendered output against saved baselines
#   test                   - run all unit + Robolectric tests

param(
  [string]$JavaHome    = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot",
  [string]$AndroidHome = "C:\Android\Sdk",
  [string]$Task        = "assembleDebug"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$env:JAVA_HOME    = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path","User") + ";" +
            "$AndroidHome\platform-tools;$AndroidHome\cmdline-tools\latest\bin"

# Ensure local.properties exists
if (-not (Test-Path "local.properties")) {
  Set-Content "local.properties" "sdk.dir=$($AndroidHome -replace '\\','\\\\')"
  Write-Host "Created local.properties -> sdk.dir=$AndroidHome"
}

# Ensure .env exists (Secrets plugin requires it for BuildConfig injection)
if (-not (Test-Path ".env")) {
  Set-Content ".env" "GEMINI_API_KEY=PLACEHOLDER_FOR_BUILD"
  Write-Host "Created placeholder .env (replace GEMINI_API_KEY value for Gemini AI features)"
}

# For screenshot tasks, filter to just GreetingScreenshotTest to skip unrelated failures
$extraArgs = @()
if ($Task -in "recordRoborazziDebug","verifyRoborazziDebug") {
  $extraArgs = @("--tests", "com.example.GreetingScreenshotTest")
}

Write-Host "=> .\gradlew.bat $Task $extraArgs"
& ".\gradlew.bat" $Task @extraArgs

if ($LASTEXITCODE -eq 0) {
  if ($Task -eq "assembleDebug") {
    $apk = Get-ChildItem "app\build\outputs\apk\debug\*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($apk) { Write-Host "APK -> $($apk.FullName)  ($([math]::Round($apk.Length/1MB,1)) MB)" }
  }
  if ($Task -eq "recordRoborazziDebug") {
    $shots = Get-ChildItem "app\src\test\screenshots\*.png" -ErrorAction SilentlyContinue
    foreach ($s in $shots) { Write-Host "Screenshot -> $($s.FullName)" }
  }
}
