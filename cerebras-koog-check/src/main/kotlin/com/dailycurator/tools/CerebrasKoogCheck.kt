package com.dailycurator.tools

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

/**
 * Local JVM check: same Koog + Cerebras wiring as [com.dailycurator.ui.screens.chat.ChatViewModel].
 *
 * ```
 * export CEREBRAS_API_KEY="..."
 * export CEREBRAS_MODEL="qwen-3-235b-a22b-instruct-2507"   # optional
 * export CEREBRAS_PROMPT="Your question"                   # optional
 * ./gradlew :cerebras-koog-check:run
 * ```
 */
@OptIn(ExperimentalTime::class)
fun main(): Unit = runBlocking {
    val apiKey = System.getenv("CEREBRAS_API_KEY")?.trim().orEmpty()
    if (apiKey.isEmpty()) {
        System.err.println("Missing CEREBRAS_API_KEY")
        exitProcess(1)
    }

    val modelId = System.getenv("CEREBRAS_MODEL")?.trim()?.takeIf { it.isNotEmpty() }
        ?: "qwen-3-235b-a22b-instruct-2507"

    val client = OpenAILLMClient(
        apiKey = apiKey,
        settings = OpenAIClientSettings(baseUrl = "https://api.cerebras.ai")
    )
    val executor = SingleLLMPromptExecutor(client)
    val model = LLModel(
        provider = LLMProvider.OpenAI,
        id = modelId,
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.OpenAIEndpoint.Completions,
        ),
        contextLength = 8096L,
        maxOutputTokens = null
    )
    val agent = AIAgent(promptExecutor = executor, llmModel = model)

    val prompt = System.getenv("CEREBRAS_PROMPT")?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Reply with exactly one word: pong"

    try {
        val reply = withContext(Dispatchers.IO) {
            agent.run(prompt)
        }
        println(reply)
    } catch (e: Exception) {
        System.err.println("Request failed: ${e.message}")
        e.printStackTrace(System.err)
        exitProcess(2)
    }
}
