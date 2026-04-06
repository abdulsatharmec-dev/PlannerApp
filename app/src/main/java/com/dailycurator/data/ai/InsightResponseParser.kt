package com.dailycurator.data.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Parses assistant-style JSON (bold_headline, summary, recovery_or_strategy) from model output.
 * Never returns raw JSON blobs as the summary string for UI display.
 */
fun parseInsightJson(raw: String): Triple<String, String, String?> {
    val stripped = stripMarkdownFences(raw.trim())
    val jsonSlice = extractBalancedJsonObject(stripped)
        ?: run {
            val s = stripped.indexOf('{')
            val e = stripped.lastIndexOf('}')
            if (s >= 0 && e > s) stripped.substring(s, e + 1) else null
        }

    val candidates = listOfNotNull(jsonSlice, stripped).distinct()
    for (candidate in candidates) {
        val obj = runCatching { JsonParser.parseString(candidate).asJsonObject }.getOrNull() ?: continue
        val bold = obj.optInsightString("bold_headline")
        var summary = obj.optInsightString("summary")
        val recovery = obj.optInsightNullableString("recovery_or_strategy")

        if (summary.isEmpty()) {
            summary = extractSummaryFromLooseText(stripped)
        }
        summary = unwrapNestedJsonSummary(summary)

        if (summary.isNotBlank() && !looksLikeRawInsightJson(summary)) {
            return Triple(bold, summary, recovery)
        }
        if (summary.isNotBlank() && looksLikeRawInsightJson(summary)) {
            val inner = runCatching { JsonParser.parseString(summary.trim()).asJsonObject.optInsightString("summary") }.getOrNull()
            if (!inner.isNullOrBlank() && !looksLikeRawInsightJson(inner)) {
                return Triple(bold, inner, recovery)
            }
        }
    }

    return Triple(
        "",
        "The model reply could not be turned into a summary. Tap **Regenerate** to try again.",
        null,
    )
}

private fun extractSummaryFromLooseText(text: String): String {
    val keyIdx = text.indexOf("\"summary\"")
    if (keyIdx < 0) return ""
    val colon = text.indexOf(':', keyIdx).takeIf { it > 0 } ?: return ""
    var i = colon + 1
    while (i < text.length && text[i].isWhitespace()) i++
    if (i >= text.length || text[i] != '"') return ""
    i++
    val out = StringBuilder()
    var escape = false
    while (i < text.length) {
        val c = text[i]
        when {
            escape -> {
                out.append(c)
                escape = false
            }
            c == '\\' -> escape = true
            c == '"' -> break
            else -> out.append(c)
        }
        i++
    }
    return out.toString().replace("\\n", "\n").replace("\\t", "\t").replace("\\\"", "\"").trim()
}

private fun unwrapNestedJsonSummary(summary: String): String {
    val t = summary.trim()
    if (t.startsWith("{") && t.contains("\"summary\"")) {
        runCatching {
            val inner = JsonParser.parseString(t).asJsonObject.optInsightString("summary")
            if (inner.isNotBlank()) return inner
        }
    }
    return summary.trim()
}

private fun looksLikeRawInsightJson(s: String): Boolean {
    val t = s.trim()
    if (!t.startsWith("{")) return false
    return t.contains("\"bold_headline\"") || t.contains("\"summary\"") || t.contains("\"recovery_or_strategy\"")
}

private fun stripMarkdownFences(text: String): String {
    var t = text.trim()
    if (!t.startsWith("```")) return t
    t = t.removePrefix("```json").removePrefix("```").trim()
    val endFence = t.lastIndexOf("```")
    if (endFence >= 0) t = t.substring(0, endFence).trim()
    return t
}

/**
 * First complete `{ ... }` in [text], respecting strings so braces inside values do not break.
 */
private fun extractBalancedJsonObject(text: String): String? {
    val start = text.indexOf('{')
    if (start < 0) return null
    var depth = 0
    var inString = false
    var escape = false
    for (i in start until text.length) {
        val c = text[i]
        if (escape) {
            escape = false
            continue
        }
        when {
            c == '\\' && inString -> escape = true
            c == '"' -> inString = !inString
            !inString && c == '{' -> depth++
            !inString && c == '}' -> {
                depth--
                if (depth == 0) return text.substring(start, i + 1)
            }
        }
    }
    return null
}

private fun JsonObject.optInsightString(key: String): String {
    val el = get(key) ?: return ""
    if (el.isJsonNull || !el.isJsonPrimitive) return ""
    return el.asString.trim()
}

private fun JsonObject.optInsightNullableString(key: String): String? {
    val el = get(key) ?: return null
    if (el.isJsonNull || !el.isJsonPrimitive) return null
    return el.asString.trim().takeIf { it.isNotEmpty() }
}
