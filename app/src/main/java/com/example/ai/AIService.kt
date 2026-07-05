package com.example.ai

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object AIService {
    private val client = OkHttpClient()
    private val mediaType = "application/json".toMediaType()

    suspend fun analyzeSpectrum(peakBins: List<Float>): Result<String> = withContext(Dispatchers.IO) {
        val formatted = peakBins.take(20).joinToString { String.format("%.1f", it) }
        val prompt = "Act as a scientific RF/audio analyzer. I have captured the following FFT peak amplitudes (first 20 bins in dB): [$formatted]. Analyze this spectrum distribution. What kind of signal might this represent (e.g. ambient noise, vocal tone, 50/60Hz hum)? Provide a concise, highly technical 2-sentence summary of the anomaly."

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=${BuildConfig.GEMINI_API_KEY}")
            .post(jsonBody.toString().toRequestBody(mediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            if (json.has("candidates")) {
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                Result.success(text)
            } else {
                Result.failure(IOException("Unexpected response format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
