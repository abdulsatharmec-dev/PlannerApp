package com.dailycurator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime

class KoogTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun testKoogCerebrasAgentSetup() {
        val apiKey = "csk-yj5wyd5hcttwk3vdkwhvd43kd24n2mh44yemcv5ppfkeejpx"
        val client = OpenAILLMClient(
            apiKey = apiKey,
            settings = OpenAIClientSettings(baseUrl = "https://api.cerebras.ai")
        )
        val executor = SingleLLMPromptExecutor(client)
        val model = LLModel(
            id = "llama3.1-8b",
            provider = LLMProvider.OpenAI,
            contextLength = 8096
        )
        val agent = AIAgent(promptExecutor = executor, llmModel = model)

        assertNotNull(agent)
        assertTrue(model.id.isNotBlank())
    }
}
