package com.increment.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.util.UUID

internal enum class AssessmentStep {
    Listen, Understand, Read, Write, Speak, Done
}

internal data class AssessmentAttempt(
    val materialId: String,
    val id: String = UUID.randomUUID().toString(),
    val step: AssessmentStep = AssessmentStep.Listen,
    val familiar: Boolean = false,
    val listens: Int = 0,
    val comprehension: String = "",
    val readUntil: Long = 0,
    val summary: String = "",
    val speaking: Boolean = false,
    val recorded: Boolean = false,
    val speakingChecks: Set<String> = emptySet(),
    val completedAt: OffsetDateTime? = null
) {
    fun json() = JSONObject().put("materialId", materialId).put("id", id).put("step", step.name)
        .put("familiar", familiar).put("listens", listens).put("comprehension", comprehension)
        .put("readUntil", readUntil).put("summary", summary).put("speaking", speaking)
        .put("recorded", recorded).put("speakingChecks", JSONArray(speakingChecks.toList()))
        .put("completedAt", completedAt?.toString().orEmpty())

    companion object {
        const val REQUIRED_LISTENS = 2
        const val READING_SECONDS = 90
        const val MAX_ANSWER_LENGTH = 6000

        fun read(json: JSONObject) = AssessmentAttempt(
            materialId = json.getString("materialId"),
            id = json.getString("id"),
            step = AssessmentStep.valueOf(json.getString("step")),
            familiar = json.getBoolean("familiar"),
            listens = json.optInt("listens"),
            comprehension = json.optString("comprehension"),
            readUntil = json.optLong("readUntil"),
            summary = json.optString("summary"),
            speaking = json.optBoolean("speaking"),
            recorded = json.optBoolean("recorded"),
            speakingChecks = json.strings("speakingChecks").toSet(),
            completedAt = json.optString("completedAt").takeIf(String::isNotBlank)?.let(OffsetDateTime::parse)
        )
    }
}

internal class AssessmentStore(context: Context) {
    private val prefs = context.getSharedPreferences("assessment", Context.MODE_PRIVATE)

    fun load(): List<AssessmentAttempt> {
        val json = JSONArray(prefs.getString("attempts", "[]"))
        return List(json.length()) { AssessmentAttempt.read(json.getJSONObject(it)) }
    }

    fun save(attempts: List<AssessmentAttempt>, submit: Boolean): Boolean {
        val editor = prefs.edit().putString("attempts", JSONArray(attempts.map(AssessmentAttempt::json)).toString())
        if (submit) return editor.commit()
        editor.apply()
        return true
    }
}
