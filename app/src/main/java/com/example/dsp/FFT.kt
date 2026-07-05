package com.example.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FFT {
    fun transform(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        if (n <= 1) return

        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempReal = real[i]
                val tempImag = imag[i]
                real[i] = real[j]
                imag[i] = imag[j]
                real[j] = tempReal
                imag[j] = tempImag
            }
            var m = n / 2
            while (m <= j) {
                j -= m
                m /= 2
            }
            j += m
        }

        var l1 = 1
        var step = 2
        while (l1 < n) {
            val theta = -PI / l1
            val wReal = cos(theta)
            val wImag = sin(theta)
            for (i in 0 until n step step) {
                var wr = 1.0
                var wi = 0.0
                for (k in 0 until l1) {
                    val i1 = i + k
                    val i2 = i1 + l1
                    val tReal = wr * real[i2] - wi * imag[i2]
                    val tImag = wr * imag[i2] + wi * real[i2]
                    real[i2] = real[i1] - tReal
                    imag[i2] = imag[i1] - tImag
                    real[i1] += tReal
                    imag[i1] += tImag

                    val newWr = wr * wReal - wi * wImag
                    val newWi = wr * wImag + wi * wReal
                    wr = newWr
                    wi = newWi
                }
            }
            l1 = step
            step *= 2
        }
    }
}
