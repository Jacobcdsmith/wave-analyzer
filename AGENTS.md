# AGENTS.md

## Critical Setup

- **No Gradle wrapper in repo.** Run `gradle wrapper --gradle-version=9.3.1` before any build command.
- **AGP 9.1.1 requires Gradle ≥ 9.3.1.** Gradle 8.x fails with version error.
- **`debug.keystore` missing.** The `debug {}` block in `app/build.gradle.kts` must be empty — remove any `signingConfig` line or build fails at `validateSigningDebug`.
- **`.env` required.** Create with `GEMINI_API_KEY=<key>` — Secrets plugin needs this file even for debug builds.
- **`local.properties` required.** Set `sdk.dir=C:\\Android\\Sdk` (or your SDK path).
- **No `google-services.json`.** Works because `gradle.properties` sets `googleServices.missing.passthrough=true`.

## Commands

```powershell
# First-time setup
gradle wrapper --gradle-version=9.3.1
Set-Content local.properties "sdk.dir=C:\\Android\\Sdk"
Set-Content .env "GEMINI_API_KEY=YOUR_KEY_HERE"

# Build
.\gradlew assembleDebug

# Test
.\gradlew test                                                          # all tests
.\gradlew test --tests "com.example.GreetingScreenshotTest"             # screenshots only
.\gradlew recordRoborazziDebug                                          # capture baselines
.\gradlew verifyRoborazziDebug                                          # verify against baselines

# Agent automation (handles env setup automatically)
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task assembleDebug
.\.claude\skills\run-wave-analyzer\smoke.ps1 -Task recordRoborazziDebug

# Device install + launch
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n "com.aistudio.waveanalyzer.abcdz/com.example.MainActivity"
adb shell pm grant com.aistudio.waveanalyzer.abcdz android.permission.RECORD_AUDIO
```

## Architecture

Source is split across focused files under `app/src/main/java/com/example/`:

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Thin activity — `enableEdgeToEdge()` + `setContent { WaveAnalyzerApp() }` |
| `WaveAnalyzerApp.kt` | Root composable: permission gate + hero screen + 4-tab bottom nav |
| `AudioAnalyzerViewModel.kt` | Audio capture, DSP pipeline, state, AI analysis |
| `VisualMode.kt` | 7 visualization mode enum |
| `dsp/FFT.kt` | In-place Cooley-Tukey FFT, `transform(real, imag)` on `DoubleArray` |
| `ai/AIService.kt` | Gemini API — suspend function, OkHttp direct POST |
| `screens/HeroScreen.kt` | Splash screen, shown on every launch (no persistence) |
| `screens/MonitorScreen.kt` | Live Canvas viz, HUD overlays, preset strip, play/stop |
| `screens/CaptureScreen.kt` | Peak snapshot + AI analysis button/result |
| `screens/EngineScreen.kt` | Audio source, color theme, gain, sensitivity |
| `screens/RemoteScreen.kt` | RTL-SDR TCP connection (real socket) |
| `ui/visualizations/Visualizations.kt` | All 7 Canvas composables |
| `ui/theme/ThemeHelpers.kt` | `getThemeColor()` + `getThemeHueOffset()` |
| `ui/theme/Color.kt` | Color constants (dark scientific palette, cyan accent) |
| `ui/theme/Theme.kt` | Material 3 dark-only `darkColorScheme` + `MyApplicationTheme` |
| `ui/theme/Type.kt` | 3 custom text styles (displaySmall, bodyLarge, labelMedium) |

### Audio Pipeline

| Parameter | Value |
|-----------|-------|
| Sample rate | 44100 Hz |
| Buffer / FFT size | 1024 samples |
| Window function | Hann (0.54 − 0.46·cos) |
| Usable bins | 512 |
| Waterfall history | 60 frames |
| Peak decay | 0.99× per frame |
| dB range | −80 dB → 0 dB |

`AudioAnalyzerViewModel` StateFlows: `waveform`, `waterfall`, `peakSpectrum`, `isRecording`, `gain`, `sensitivity`, `colorTheme`, `aiAnalysis`, `isAnalyzing`.

### Screens (4 tabs, manual `when()` routing — no Navigation Compose)

| Tab | Composable | Contents |
|-----|-----------|----------|
| Monitor | `MonitorScreen` | Live Canvas viz, HUD overlays, preset strip, play/stop |
| Capture | `CaptureScreen` | Peak snapshot + AI analysis button/result |
| Engine | `EngineScreen` | Audio source, color theme, gain (0.1–5×), sensitivity (0.1–3×) |
| Remote | `RemoteScreen` | RTL-SDR TCP connection with real socket + error handling |

### VisualMode Enum → Canvas Composables (in `ui/visualizations/Visualizations.kt`)

| Enum | Canvas | Input |
|------|--------|-------|
| `WAVEFORM` | `WaveformCanvas` | waveform |
| `SPECTRUM` | `SpectrumCanvas` | spectrum + peakSpectrum |
| `WATERFALL_3D` | `Waterfall3DCanvas` | waterfall — **default mode** |
| `SDR_WATERFALL` | `SDRWaterfallCanvas` | waterfall |
| `IQ_PLOT` | `IQConstellationCanvas` | waveform (5-sample delay) |
| `RADAR_SPECTRUM` | `RadarSpectrumCanvas` | spectrum |
| `PHASE_SPACE` | `PhaseSpaceCanvas` | waveform (12-sample delay) |

### Color Themes

| Index | Name | Color | Hue Offset |
|-------|------|-------|------------|
| 0 | Default | Cyan `#00E5FF` | 240° |
| 1 | Ocean | Blue `#00B0FF` | 180° |
| 2 | Fire | Red `#FF3D00` | 60° |
| 3 | Cyberpunk | Magenta `#E040FB` | 300° |

`getThemeColor(theme, alpha)` in `ui/theme/ThemeHelpers.kt`. `getThemeHueOffset(theme)` → HSV base hue for waterfall canvases.

### AI Service

- **Model:** `gemini-3.1-pro-preview` via `generativelanguage.googleapis.com/v1beta`
- **Client:** OkHttp direct POST in `ai/AIService.kt` (suspend function, coroutine-based)
- **Key:** `BuildConfig.GEMINI_API_KEY` (injected from `.env` by Secrets plugin)
- **Prompt:** First 20 peak bins formatted as dB values, asks for RF/audio signal classification

## Testing

| Test | Type | Status |
|------|------|--------|
| `GreetingScreenshotTest` | Robolectric + Roborazzi | Works. Renders `WaveformCanvas` (sine wave) + `HeroScreen` |
| `ExampleUnitTest` | Plain JUnit | Works. Trivial `2+2=4` |
| `ExampleRobolectricTest` | Robolectric | Works. Asserts `app_name == "Wave Analyzer"` |
| `ExampleInstrumentedTest` | Instrumented | Works. Asserts `packageName == "com.aistudio.waveanalyzer.abcdz"` |

Screenshot baselines: `app/src/test/screenshots/greeting.png`, `hero.png`. No custom Roborazzi config — output paths hardcoded in test code.

## Gotchas

- **`applicationId` ≠ `namespace`.** Package is `com.aistudio.waveanalyzer.abcdz`, namespace is `com.example`.
- **No emulator without Hyper-V/KVM.** Use physical device or Roborazzi for screenshots.
- **Gemini placeholder key.** `YOUR_KEY_HERE` builds fine; AI features fail at runtime.
- **Dark-only theme.** No light mode exists.
- **No lint, format, or CI/CD config.** No ktlint, detekt, editorconfig, or GitHub Actions.
- **`gradle.properties` quirks:** `kotlin.compiler.execution.strategy=in-process`, JVM heap 4g, max 4 workers, config cache enabled.
