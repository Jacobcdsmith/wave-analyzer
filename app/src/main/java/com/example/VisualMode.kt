package com.example

enum class VisualMode(val title: String) {
    WAVEFORM("Waveform"),
    SPECTRUM("RF Spectrum"),
    WATERFALL_3D("3D Waterfall"),
    SDR_WATERFALL("SDR Waterfall"),
    IQ_PLOT("I/Q Plot"),
    RADAR_SPECTRUM("Radar Plot"),
    PHASE_SPACE("Phase Space")
}
