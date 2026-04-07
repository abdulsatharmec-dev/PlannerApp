package com.dailycurator.data.ai

import com.google.gson.JsonObject
import com.google.gson.JsonParser

data class ParsedInsightBundle(
    val boldHeadline: String,
    val summary: String,
    val recoveryOrStrategy: String?,
    val summarySegmentsJson: String?,
    val spiritualSource: String?,
    val spiritualArabic: String?,
    val spiritualEnglish: String?,
)

/**
 * Parses assistant-style JSON (bold_headline, summary, recovery_or_strategy) from model output.
 * Never returns raw JSON blobs as the summary string for UI display.
 */
fun parseInsightJson(raw: String): Triple<String, String, String?> {
    val b = parseInsightBundle(raw)
    return Triple(b.boldHeadline, b.summary, b.recoveryOrStrategy)
}

/**
 * Full parse including optional [summary_segments] and [spiritual_note] for assistant insight UI.
 */
fun parseInsightBundle(raw: String): ParsedInsightBundle {
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
        val segmentsJson = obj.optInsightSegmentsJson("summary_segments")
        val (spSrc, spAr, spEn) = obj.optSpiritualNote()

        if (summary.isEmpty()) {
            summary = extractSummaryFromLooseText(stripped)
        }
        summary = unwrapNestedJsonSummary(summary)

        if (summary.isNotBlank() && !looksLikeRawInsightJson(summary)) {
            return ParsedInsightBundle(bold, summary, recovery, segmentsJson, spSrc, spAr, spEn)
        }
        if (summary.isNotBlank() && looksLikeRawInsightJson(summary)) {
            val inner = runCatching { JsonParser.parseString(summary.trim()).asJsonObject.optInsightString("summary") }.getOrNull()
            if (!inner.isNullOrBlank() && !looksLikeRawInsightJson(inner)) {
                return ParsedInsightBundle(bold, inner, recovery, segmentsJson, spSrc, spAr, spEn)
            }
        }
    }

    return ParsedInsightBundle(
        boldHeadline = "",
        summary = "The model reply could not be turned into a summary. Tap **Regenerate** to try again.",
        recoveryOrStrategy = null,
        summarySegmentsJson = null,
        spiritualSource = null,
        spiritualArabic = null,
        spiritualEnglish = null,
    )
}

private fun JsonObject.optInsightSegmentsJson(key: String): String? {
    val el = get(key) ?: return null
    if (el.isJsonNull || !el.isJsonArray) return null
    val arr = el.asJsonArray
    if (arr.size() == 0) return null
    return arr.toString()
}

private fun JsonObject.optSpiritualNote(): Triple<String?, String?, String?> {
    val el = get("spiritual_note") ?: return Triple(null, null, null)
    if (el.isJsonNull || !el.isJsonObject) return Triple(null, null, null)
    val o = el.asJsonObject
    val src = o.optInsightNullableString("source")
    val ar = o.optInsightNullableString("arabic")
    val en = o.optInsightNullableString("english")
    if (en.isNullOrBlank() && ar.isNullOrBlank()) return Triple(null, null, null)
    return Triple(src, ar, en)
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
