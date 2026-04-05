package com.dailycurator

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
import org.junit.Assume.assumeFalse
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

/**
 * Optional live check against Cerebras (same stack as [com.dailycurator.ui.screens.chat.ChatViewModel]).
 *
 * Run on your machine (no phone install):
 *
 * ```
 * export CEREBRAS_API_KEY="your-key"
 * export CEREBRAS_MODEL="qwen-3-235b-a22b-instruct-2507"   # optional (default = same as ChatViewModel)
 * ./gradlew :app:testDebugUnitTest --tests "com.dailycurator.KoogCerebrasLiveTest"
 * ```
 */
class KoogCerebrasLiveTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun cerebrasChatCompletionsRoundTrip() = runBlocking {
        val apiKey = System.getenv("CEREBRAS_API_KEY")?.trim().orEmpty()
        assumeFalse("Set CEREBRAS_API_KEY to run this test", apiKey.isBlank())

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
            maxOutputTokens = null,
        )
        val agent = AIAgent(promptExecutor = executor, llmModel = model)

        val reply = withContext(Dispatchers.IO) {
            agent.run("Reply with exactly one word: pong")
        }
        assertTrue(reply.isNotBlank(), "empty model reply")
    }
}
