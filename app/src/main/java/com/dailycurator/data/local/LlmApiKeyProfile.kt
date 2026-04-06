package com.dailycurator.data.local

import java.util.UUID

/** Stored LLM credential row (multiple keys, user-named, per-provider model). */
data class LlmApiKeyProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String,
    /** [LlmProviderIds] */
    val providerId: String,
    val modelId: String,
    val apiKey: String,
    val enabled: Boolean = true,
)

object LlmProviderIds {
    const val CEREBRAS = "cerebras"
    const val GROQ = "groq"
}

/** Resolved URL + credentials for one OpenAI-compatible chat request. */
data class LlmEndpointConfig(
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
)

fun LlmApiKeyProfile.toEndpointConfig(): LlmEndpointConfig? {
    val base = when (providerId) {
        LlmProviderIds.CEREBRAS -> "https://api.cerebras.ai/v1"
        LlmProviderIds.GROQ -> "https://api.groq.com/openai/v1"
        else -> return null
    }
    val k = apiKey.trim()
    if (k.isEmpty()) return null
    val m = modelId.trim()
    if (m.isEmpty()) return null
    return LlmEndpointConfig(baseUrl = base, apiKey = k, modelId = m)
}
