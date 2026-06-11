package com.example.webhooksender.data.remote

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class WebhookApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()

    sealed class SendResult {
        data class Success(val responseCode: Int) : SendResult()
        data class Error(val message: String) : SendResult()
    }

    suspend fun sendMessage(
        webhookUrl: String,
        keyword: String,
        content: String,
        priority: String,
        deadline: String = ""
    ): SendResult {
        return try {
            val textBuilder = StringBuilder()
            textBuilder.append(keyword)
            if (priority.isNotBlank()) {
                textBuilder.append(" 【").append(priority).append("】")
            }
            if (deadline.isNotBlank()) {
                textBuilder.append("\n截止时间：").append(deadline)
            }
            textBuilder.append("\n").append(content)

            val fullText = textBuilder.toString()

            val jsonBody = JSONObject().apply {
                put("keyword", keyword)
                put("content", content)
                put("priority", priority)
                put("deadline", deadline)
                put("text", fullText)
                put("timestamp", System.currentTimeMillis())
            }

            val body = jsonBody.toString().toRequestBody(JSON)
            val request = Request.Builder()
                .url(webhookUrl)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    SendResult.Success(response.code)
                } else {
                    val errorBody = response.body?.string()?.take(200) ?: "Unknown error"
                    SendResult.Error("HTTP ${response.code}: $errorBody")
                }
            }
        } catch (e: IOException) {
            Log.e("WebhookApi", "Network error", e)
            SendResult.Error("网络错误: ${e.message}")
        } catch (e: Exception) {
            Log.e("WebhookApi", "Unexpected error", e)
            SendResult.Error("发送出错: ${e.message}")
        }
    }
}
