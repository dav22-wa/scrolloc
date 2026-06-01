package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getAdditionInsights(
        limits: List<AppLimit>,
        streak: DisciplineStreak?,
        overrides: List<EmergencyOverrideLog>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "🔑 API Key is not set in AI Studio Secrets. To enable personalized AI Addiction Insights, enter a valid GEMINI_API_KEY in the Secrets panel."
        }

        val usageSummary = limits.joinToString("\n") { 
            "- ${it.appName}: ${String.format("%.1f", it.usedMinutesToday)} / ${it.limitMinutes} min (Blocked: ${it.isBlocked})"
        }
        val streakInfo = streak?.let { 
            "Streak: ${it.currentStreak} days under limits, ${it.totalHoursSaved} total hours saved."
        } ?: "No active streak info."
        val overrideSummary = overrides.joinToString("\n") {
            "- Override for ${it.appName} on ${java.util.Date(it.timestamp)}: \"${it.reason}\""
        }

        val prompt = """
            You are ScrollLock AI, an elite digital wellbeing expert, behavioral psychologist, and accountability coach.
            Analyze the following social media usage data for today:
            
            $usageSummary
            
            $streakInfo
            
            Emergency Overrides Performed Today:
            ${if (overrides.isEmpty()) "None. Excellent discipline!" else overrideSummary}
            
            Deliver a highly motivating, professional, and slightly witty Digital Wellbeing Report.
            Include:
            1. 📊 Scroll Addiction Triage: Diagnose their level of scrolling (Safe, Wandering, or Critical Bingeing) and explain why based on their usage relative to their limits.
            2. 💡 Tailored Behavioral Hacks: Give them 2 bespoke practical recommendations to defeat their most addictive apps today/tomorrow.
            3. 🔥 Mindful Streak Fuel: Keep their streak going or encourage them if they fell short.
            
            Format nicely with clear headers (no markdown markdown block formatting, just bold titles and bullet points). Be concise, direct, helpful, and direct-to-the-point! Avoid generic greetings or corporate speak.
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val jsonRequest = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API error: status code ${response.code}, message: $errorBody")
                    return@withContext "Unable to connection to ScrollLock AI. (HTTP code: ${response.code}). Please verify your API Key or try again later."
                }

                val responseBody = response.body?.string()
                if (responseBody == null) {
                    return@withContext "Empty response from ScrollLock AI."
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No insights generated.")
                    }
                }
                return@withContext "No response text found from AI model. Please verify structure."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            return@withContext "Connection Error: Unable to reach ScrollLock AI. Detail: ${e.localizedMessage}. Verify your internet connection."
        }
    }
}
