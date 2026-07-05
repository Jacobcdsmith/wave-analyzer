# CLAUDE.md

This file provides guidance to Claude Code / OpenCode when working with code in this repository.

## Project Overview

**OmniWave / Wave Analyzer** — an Android audio/RF spectrum analyzer app built with Kotlin and Jetpack Compose. It captures microphone audio in real time, runs FFT analysis, renders multiple visualization modes, and sends spectrum data to Gemini AI for interpretation.

## Build & Run

**Prerequisites:** Android Studio with Android SDK (minSdk 24, compileSdk 36).

```bash
# First-time setup (no wrapper in repo)
gradle wrapper --gradle-version=9.3.1

# Build debug APK
./gradlew assembleDebug

# Run all unit + Robolectric tests
./gradlew test

# Capture screenshot baselines (Roborazzi)
./gradlew recordRoborazziDebug

# Verify screenshots against baselines
./gradlew verifyRoborazziDebug
```

**API key setup:** Create `.env` in the project root and set `GEMINI_API_KEY=<your_key>`. The Secrets Gradle plugin injects it into `BuildConfig.GEMINI_API_KEY` at build time.

**Setup notes:**
- `debug.keystore` is missing, so the `debug {}` block in `app/build.gradle.kts` must be empty (no `signingConfig` line).
- `local.properties` must set `sdk.dir` to your Android SDK path.
- No `google-services.json` is required because `gradle.properties` sets `googleServices.missing.passthrough=true`.

## Architecture

Source is split across focused files under `app/src/main/java/com/example/`:

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Thin activity — `enableEdgeToEdge()` + `setContent { WaveAnalyzerApp() }` |
| `WaveAnalyzerApp.kt` | Root composable: permission banner + hero screen + 4-tab bottom nav |
| `AudioAnalyzerViewModel.kt` | Audio capture, DSP pipeline orchestration, state, AI analysis |
| `VisualMode.kt` | 7 visualization mode enum |
| `dsp/FFT.kt` | In-place Cooley-Tukey FFT |
| `dsp/SpectrumProcessor.kt` | Windowing, FFT, dB conversion, peak-hold, waterfall buffering |
| `dsp/ToneGenerator.kt` | Sine/square/saw/noise generator for self-test |
| `ai/AIService.kt` | Gemini API — suspend function, OkHttp direct POST |
| `screens/*.kt` | Hero, Monitor, Capture, Engine, Remote screens |
| `ui/visualizations/Visualizations.kt` | All 7 Canvas composables |
| `ui/theme/*` | Dark-only Material 3 theme and color helpers |

### Audio Pipeline

- Sample rate: 44 100 Hz
- Buffer / FFT size: 1024 samples
- Window: Hann (0.54 − 0.46·cos)
- Usable bins: 512
- Waterfall history: 60 frames
- Peak decay: 0.99× per frame
- dB range: −80 dB → 0 dB

`SpectrumProcessor` encapsulates the DSP math. `AudioAnalyzerViewModel` owns `AudioRecord`, runtime state, and zoom/pan logic.

### State Flows

`AudioAnalyzerViewModel` exposes `StateFlow`s for:
`waveform`, `waterfall`, `peakSpectrum`, `isRecording`, `gain`, `sensitivity`, `colorTheme`, `aiAnalysis`, `isAnalyzing`, `zoomStartBin`, `zoomBinCount`.

### Screens

Four tabs routed with manual `when()` — no Navigation Compose:

| Tab | Composable | Contents |
|-----|-----------|----------|
| Monitor | `MonitorScreen` | Live Canvas viz, HUD overlays, preset strip, play/stop |
| Capture | `CaptureScreen` | Peak snapshot + AI analysis button/result |
| Engine | `EngineScreen` | Audio source, color theme, gain, sensitivity |
| Remote | `RemoteScreen` | RTL-SDR TCP connection |

## Key Dependencies

- **Jetpack Compose** (BOM 2024.09.00) + Material 3
- **OkHttp 4.10** — Gemini API calls
- **Robolectric 4.16 + Roborazzi 1.59** — unit and screenshot testing
- **Secrets Gradle Plugin 2.0.1** — loads `.env` → `BuildConfig`
