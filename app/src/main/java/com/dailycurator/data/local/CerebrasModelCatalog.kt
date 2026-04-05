package com.dailycurator.data.local

/** Labels for known Cerebras Inference models (ids per https://inference-docs.cerebras.ai/models). */
data class CerebrasModelOption(val displayName: String, val modelId: String)

const val DEFAULT_CEREBRAS_MODEL_ID = "qwen-3-235b-a22b-instruct-2507"

val CEREBRAS_MODEL_OPTIONS: List<CerebrasModelOption> = listOf(
    CerebrasModelOption("Qwen 3 235B Instruct", "qwen-3-235b-a22b-instruct-2507"),
    CerebrasModelOption("Llama 3.1 8B", "llama3.1-8b"),
    CerebrasModelOption("GPT OSS 120B", "gpt-oss-120b"),
    CerebrasModelOption("Z.ai GLM 4.7", "zai-glm-4.7"),
)
