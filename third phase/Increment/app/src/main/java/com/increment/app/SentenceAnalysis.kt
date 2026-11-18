package com.increment.app

import org.json.JSONException
import org.json.JSONObject

internal data class SentenceAnalysis(val translation: String, val segments: List<SentenceSegment>)

internal data class SentenceSegment(
    val range: IntRange,
    val role: String,
    val translation: String,
    val explanation: String
)

internal fun parseSentenceAnalysis(sentence: String, response: String): SentenceAnalysis {
    val result = try {
        JSONObject(response)
    } catch (_: JSONException) {
        throw invalidAnalysis()
    }
    val translation = result.nonEmptyText("translation") ?: throw invalidAnalysis()
    val segments = result.optJSONArray("segments") ?: throw invalidAnalysis()
    val original = normalizeQuotes(sentence)
    var cursor = 0
    val matched = buildList {
        for (index in 0 until segments.length().coerceAtMost(6)) {
            val segment = segments.optJSONObject(index) ?: continue
            val text = segment.nonEmptyText("text") ?: continue
            val role = segment.nonEmptyText("role") ?: continue
            val meaning = segment.nonEmptyText("translation") ?: continue
            val explanation = segment.nonEmptyText("explanation") ?: continue
            val quoted = Regex.escape(normalizeQuotes(normalizeCaption(text)))
            val startBoundary = if (text.first().isLetterOrDigit()) "(?<![\\p{L}\\p{N}])" else ""
            val endBoundary = if (text.last().isLetterOrDigit()) "(?![\\p{L}\\p{N}])" else ""
            val match = Regex(startBoundary + quoted + endBoundary, RegexOption.IGNORE_CASE)
                .find(original, cursor) ?: continue
            add(SentenceSegment(match.range, role, meaning, explanation))
            cursor = match.range.last + 1
        }
    }
    if (matched.isEmpty()) throw invalidAnalysis()
    return SentenceAnalysis(translation, matched)
}

private fun normalizeQuotes(value: String): String =
    value.replace('’', '\'').replace('‘', '\'').replace('“', '"').replace('”', '"')

private fun JSONObject.nonEmptyText(field: String): String? =
    (opt(field) as? String)?.trim()?.takeIf(String::isNotEmpty)

private fun invalidAnalysis() =
    DeepSeekError("这次解析没有返回可对应原句的内容，请重新解析。")
