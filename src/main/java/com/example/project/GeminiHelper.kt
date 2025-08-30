package com.example.project

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class GeminiHelper {
    private val client = OkHttpClient()
    private val apiKey = "AIzaSyBMt7cT5Gqxtwk2rgqj_wHQFLX2SnQFyEU"

    fun getHealthAdvice(steps: Int, callback: (String) -> Unit) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$apiKey"

        val json = JSONObject()
            .put("contents", listOf(
                mapOf("parts" to listOf(
                    mapOf("text" to "I walked $steps steps today. Give me a short health tip.")
                ))
            ))

        // ✅ Fix here (use toRequestBody)
        val body = json.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("❌ Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val res = JSONObject(response.body!!.string())
                val text = res.getJSONArray("candidates")
                    .getJSONObject(0).getJSONObject("content")
                    .getJSONArray("parts").getJSONObject(0).getString("text")

                callback("💡 Gemini Advice: $text")
            }
        })
    }
}