---
name: run-wave-analyzer
description: Run, build, screenshot, or test the wave-analyzer Android app. Use when asked to build the APK, take a screenshot of the UI, run tests, verify the app compiles, or interact with the WaveAnalyzer/OmniWave app.
---

# run-wave-analyzer

OmniWave is an Android app (Kotlin + Jetpack Compose). It cannot run in a browser or as a desktop process — it runs on an Android device or emulator. The interaction harness is `smoke.ps1` (in this skill directory), which drives `gradlew.bat` for building and screenshot capture.

**Screenshots are taken via Roborazzi** (JVM-based Compose rendering, no device needed). Installing on a physical device uses `adb install`. An emulator requires hardware virtualization — see Gotchas.

Paths below are relative to the project root (`wave-analyzer/`).

---

## Prerequisites

Install once on a clean machine. All commands verified on Windows 11.

```powershell
# 1. JDK 21
winget install --id Microsoft.OpenJDK.21 --silent --accept-package-agreements --accept-source-agreements

# Refresh session PATH after install
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
java -version   # must print openjdk 21

# 2. Android command-line tools (SDK)
New-Item -ItemType Directory -Force -Path "C:\Android\Sdk\cmdline-tools" | Out-Null
Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip" `
  -OutFile "$env:TEMP\cmdline-tools.zip" -UseBasicParsing
Expand-Archive "$env:TEMP\cmdline-tools.zip" "C:\Android\Sdk\cmdline-tools" -Force
Move-Item "C:\Android\Sdk\cmdline-tools\cmdline-tools" "C:\Android\Sdk\cmdline-tools\latest"

# 3. Accept SDK licenses and install required packages
$env:ANDROID_HOME = "C:\Android\Sdk"
"y`ny`ny`ny`ny`ny`ny`ny`n" | & "C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" `
  --sdk_root="C:\Android\Sdk" --licenses
& "C:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="C:\Android\Sdk" `
  "build-tools;36.0.0" "platforms;android-36" "platform-tools"
```

---

## Setup (per project clone)

The project ships **without** a Gradle wrapper or `local.properties` — both must be generated.

```powershell
# Download Gradle 9.3.1 (required by AGP 9.1.1 — 8.x fails)
Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-9.3.1-bin.zip" `
  -OutFile "$env:TEMP\gradle-9.3.1-bin.zip" -UseBasicParsing
Expand-Archive "$env:TEMP\gradle-9.3.1-bin.zip" "C:\gradle" -Force
$env:Path += ";C:\gradle\gradle-9.3.1\bin"

# Generate the Gradle wrapper inside the project
cd "path\to\wave-analyzer"
gradle wrapper --gradle-version=9.3.1

# Create local.properties
Set-Content local.properties "sdk.dir=C:\\Android\\Sdk"

# Create .env (Gemini API key — replace with real key for AI features)
Set-Content .env "GEMINI_API_KEY=YOUR_KEY_HERE"

# Remove broken signing config (debug.keystore doesn't exist in repo)
# In app/build.gradle.kts, change the debug block from:
#   debug { signingConfig = signingConfigs.getByName("debugConfig") }
# to:
#   debug { }
```

---

## Run (agent path) — build and screenshot

Use `smoke.ps1` to drive everything from the project root:

```powershell
# Build APK (outputs to app/build/outputs/apk/debug/app-debug.apk)
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task assembleDebug

# Take a screenshot of the waveform canvas (no device needed)
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task recordRoborazziDebug
# Screenshot lands at: app/src/test/screenshots/greeting.png

# Verify screenshot hasn't changed
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task verifyRoborazziDebug

# Run all unit + Robolectric tests
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task test
```

To override JDK or SDK paths:
```powershell
.\.claude\skills\run-wave-analyzer\smoke.ps1 -JavaHome "C:\jdk21" -AndroidHome "D:\Android\Sdk" -Task assembleDebug
```

### Screenshot test details

`GreetingScreenshotTest` in `app/src/test/java/com/example/` renders `WaveformCanvas` with a synthetic sine wave via Roborazzi. Robolectric + NATIVE graphics mode draws Compose without a device. The output is `app/src/test/screenshots/greeting.png`.

---

## Run (human path) — install on device

Once the APK is built:

```powershell
# List connected devices/emulators
adb devices

# Install
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Launch the app
adb shell am start -n "com.aistudio.waveanalyzer.abcdz/com.example.MainActivity"

# Take a screenshot from the device
adb exec-out screencap -p > device_screenshot.png
```

The app immediately requests microphone permission on first launch. Grant it from the device UI or:
```powershell
adb shell pm grant com.aistudio.waveanalyzer.abcdz android.permission.RECORD_AUDIO
```

---

## Gotchas

- **No Gradle wrapper in repo.** The project is designed to be opened in Android Studio, which generates the wrapper automatically. Running from the command line requires `gradle wrapper --gradle-version=9.3.1` first.

- **AGP 9.1.1 requires Gradle ≥ 9.3.1.** Running with Gradle 8.x fails with `Minimum supported Gradle version is 9.3.1`. The wrapper URL in `gradle-wrapper.properties` must point to `gradle-9.3.1-bin.zip`.

- **`debug.keystore` does not exist.** The `app/build.gradle.kts` `debug` block references `signingConfigs.getByName("debugConfig")` which expects a `debug.keystore` file at the project root. Remove that line (change `debug { signingConfig = ... }` to `debug { }`) or the build fails at `validateSigningDebug`.

- **`Greeting` composable doesn't exist.** The original `GreetingScreenshotTest.kt` from the Android Studio template referenced `Greeting("Robolectric")`, but that composable was removed when the app was rewritten. The test has been updated to render `WaveformCanvas`.

- **Hardware virtualization required for emulator.** The Android emulator needs Hyper-V (Windows) or KVM (Linux). Without it (`VMMonitorModeExtensions=False`), the emulator will refuse to start or be too slow to be usable. Use a physical device instead.

- **Gemini API key placeholder.** Without a real `GEMINI_API_KEY` in `.env`, the app builds fine but the "Run Gemini Intelligence Analysis" button on the Capture tab returns an error at runtime. Use `YOUR_KEY_HERE` as a placeholder for build-only workflows.

- **`ExampleRobolectricTest` fails.** The test asserts `appName == "My Application"` but the app name in `strings.xml` may differ. This test failure does not affect the build or screenshot capture — run `recordRoborazziDebug` directly instead of `test` if you only need screenshots.

---

## Troubleshooting

| Error | Fix |
|-------|-----|
| `Minimum supported Gradle version is 9.3.1` | Update `gradle-wrapper.properties` distributionUrl to `gradle-9.3.1-bin.zip`, or rerun `gradle wrapper --gradle-version=9.3.1` |
| `Keystore file 'debug.keystore' not found` | Remove `signingConfig = signingConfigs.getByName("debugConfig")` from the `debug {}` block in `app/build.gradle.kts` |
| `Unresolved reference 'Greeting'` | The test was fixed — make sure `GreetingScreenshotTest.kt` references `WaveformCanvas`, not `Greeting` |
| `sdk.dir not set` | Run `Set-Content local.properties "sdk.dir=C:\\Android\\Sdk"` from the project root |
| `GEMINI_API_KEY not found` | Create `.env` with `GEMINI_API_KEY=PLACEHOLDER` — the Secrets plugin requires this file to exist even for a debug build |
