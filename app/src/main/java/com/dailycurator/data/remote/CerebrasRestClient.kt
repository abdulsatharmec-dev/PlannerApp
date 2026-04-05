package com.dailycurator.data.remote

import com.dailycurator.data.local.AppPreferences
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

data class CerebrasToolDefinition(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject,
)

data class CerebrasChatMessage(
    val role: String,
    val content: String? = null,
    val toolCalls: List<CerebrasToolCall>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
)

data class CerebrasToolCall(
    val id: String,
    val functionName: String,
    val argumentsJson: String,
)

data class CerebrasChoiceMessage(
    val role: String,
    val content: String?,
    val toolCalls: List<CerebrasToolCall>?,
)

data class CerebrasCompletionResult(
    val message: CerebrasChoiceMessage?,
    val finishReason: String?,
)

class CerebrasApiException(message: String, val httpCode: Int? = null) : Exception(message)

@Singleton
class CerebrasRestClient @Inject constructor(
    private val prefs: AppPreferences,
    private val gson: Gson,
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun chatCompletion(
        messages: List<CerebrasChatMessage>,
        tools: List<CerebrasToolDefinition>? = null,
        toolChoice: String = "auto",
        temperature: Double = 0.4,
        maxTokens: Int = 2048,
    ): CerebrasCompletionResult = withContext(Dispatchers.IO) {
        val apiKey = prefs.getCerebrasKey().trim()
        if (apiKey.isEmpty()) throw CerebrasApiException("Cerebras API key is not set.")

        val model = prefs.getCerebrasModelId()
        val body = JsonObject().apply {
            addProperty("model", model)
            addProperty("temperature", temperature)
            addProperty("max_tokens", maxTokens)
            val arr = JsonArray()
            messages.forEach { m -> arr.add(m.toJson()) }
            add("messages", arr)
            if (!tools.isNullOrEmpty()) {
                val toolsArr = JsonArray()
                tools.forEach { t ->
                    toolsArr.add(
                        JsonObject().apply {
                            addProperty("type", "function")
                            add(
                                "function",
                                JsonObject().apply {
                                    addProperty("name", t.name)
                                    addProperty("description", t.description)
                                    add("parameters", t.parametersSchema)
                                },
                            )
                        },
                    )
                }
                add("tools", toolsArr)
                addProperty("tool_choice", toolChoice)
            }
        }

        val req = Request.Builder()
            .url("https://api.cerebras.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody(JSON_MEDIA))
            .build()

        http.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw CerebrasApiException("HTTP ${resp.code}: $respBody", resp.code)
            }
            val root = gson.fromJson(respBody, JsonObject::class.java)
            val choices = root.getAsJsonArray("choices") ?: throw CerebrasApiException("No choices in response")
            if (choices.size() == 0) throw CerebrasApiException("Empty choices")
            val choice = choices[0].asJsonObject
            val finishReason = choice.get("finish_reason")?.asString
            val msg = choice.getAsJsonObject("message")
            val role = msg.get("role")?.asString ?: "assistant"
            val content = msg.get("content")?.takeUnless { it.isJsonNull }?.asString
            val toolCalls = msg.get("tool_calls")?.asJsonArray?.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val o = el.asJsonObject
                val id = o.get("id")?.asString ?: return@mapNotNull null
                val fn = o.getAsJsonObject("function")
                val name = fn.get("name")?.asString ?: return@mapNotNull null
                val args = fn.get("arguments")?.asString ?: "{}"
                CerebrasToolCall(id = id, functionName = name, argumentsJson = args)
            }
            CerebrasCompletionResult(
                message = CerebrasChoiceMessage(
                    role = role,
                    content = content,
                    toolCalls = toolCalls?.takeIf { it.isNotEmpty() },
                ),
                finishReason = finishReason,
            )
        }
    }

    private fun CerebrasChatMessage.toJson(): JsonObject = JsonObject().apply {
        addProperty("role", role)
        if (role == "tool") {
            addProperty("tool_call_id", toolCallId ?: "")
            addProperty("content", content ?: "")
            return@apply
        }
        if (content != null) addProperty("content", content)
        toolCallId?.let { addProperty("tool_call_id", it) }
        name?.let { addProperty("name", it) }
        toolCalls?.let { calls ->
            val arr = JsonArray()
            calls.forEach { tc ->
                arr.add(
                    JsonObject().apply {
                        addProperty("id", tc.id)
                        addProperty("type", "function")
                        add(
                            "function",
                            JsonObject().apply {
                                addProperty("name", tc.functionName)
                                addProperty("arguments", tc.argumentsJson)
                            },
                        )
                    },
                )
            }
            add("tool_calls", arr)
        }
    }
}
