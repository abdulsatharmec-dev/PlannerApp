package com.dailycurator.data.local

/**
 * Model ids from the Groq console rate-limits documentation (chat / text models).
 * Source: [Rate limits](https://console.groq.com/docs/rate-limits)
 */
data class GroqModelOption(val displayName: String, val modelId: String)

val GROQ_MODEL_OPTIONS: List<GroqModelOption> = listOf(
    GroqModelOption("Llama 3.3 70B Versatile", "llama-3.3-70b-versatile"),
    GroqModelOption("Llama 3.1 8B Instant", "llama-3.1-8b-instant"),
    GroqModelOption("Llama 4 Scout 17B 16E Instruct", "meta-llama/llama-4-scout-17b-16e-instruct"),
    GroqModelOption("Qwen3 32B", "qwen/qwen3-32b"),
    GroqModelOption("Kimi K2 Instruct", "moonshotai/kimi-k2-instruct"),
    GroqModelOption("Kimi K2 Instruct 0905", "moonshotai/kimi-k2-instruct-0905"),
    GroqModelOption("GPT-OSS 120B", "openai/gpt-oss-120b"),
    GroqModelOption("GPT-OSS 20B", "openai/gpt-oss-20b"),
    GroqModelOption("GPT-OSS Safeguard 20B", "openai/gpt-oss-safeguard-20b"),
    GroqModelOption("Groq Compound", "groq/compound"),
    GroqModelOption("Groq Compound Mini", "groq/compound-mini"),
    GroqModelOption("Allam 2 7B", "allam-2-7b"),
    GroqModelOption("Llama Prompt Guard 2 22M", "meta-llama/llama-prompt-guard-2-22m"),
    GroqModelOption("Llama Prompt Guard 2 86M", "meta-llama/llama-prompt-guard-2-86m"),
)
