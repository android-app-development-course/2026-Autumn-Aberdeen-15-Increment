package com.increment.app

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.text.BreakIterator
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

data class Cue(val start: Int, val end: Int, val text: String, val translation: String = "") {
    fun json() = JSONObject().put("start", start).put("end", end).put("text", text)
        .put("translation", translation)
}

data class Lesson(
    val id: String, val title: String, val english: String, val topic: String,
    val start: Int, val end: Int, val cues: List<Cue>, val target: String = "",
    val notes: String = "", val uri: String = "", val video: Boolean = false,
    val previousClipStart: Int = 0, val sourceUrl: String = "", val difficulty: String = "",
    val expressions: List<String> = emptyList(), val keyPoints: List<String> = emptyList(),
    val mediaId: String = id
) {
    val duration get() = end - start
    val isPlayable get() = duration > 0 && cues.isNotEmpty()
    val expressionOptions get() = (listOf(target) + expressions).filter(String::isNotBlank).distinct()

    fun json() = JSONObject().put("id", id).put("title", title).put("english", english)
        .put("topic", topic).put("start", start).put("end", end).put("target", target)
        .put("notes", notes).put("uri", uri).put("video", video)
        .put("previousClipStart", previousClipStart)
        .put("sourceUrl", sourceUrl).put("difficulty", difficulty)
        .put("mediaId", mediaId)
        .put("expressions", JSONArray(expressions)).put("keyPoints", JSONArray(keyPoints))
        .put("cues", JSONArray(cues.map(Cue::json)))

    fun mediaUri(context: Context): Uri = if (uri.isNotEmpty()) Uri.parse(uri) else
        Uri.parse("android.resource://${context.packageName}/raw/$mediaId")

    companion object {
        fun read(json: JSONObject): Lesson {
            val list = json.getJSONArray("cues")
            return Lesson(
                json.getString("id"), json.getString("title"), json.optString("english"),
                json.getString("topic"), json.optInt("start"), json.getInt("end"),
                List(list.length()) { i ->
                    list.getJSONObject(i).let {
                        Cue(it.getInt("start"), it.getInt("end"), it.getString("text"), it.optString("translation"))
                    }
                },
                json.optString("target"), json.optString("notes"), json.optString("uri"), json.optBoolean("video"),
                json.optInt("previousClipStart"), json.optString("sourceUrl"), json.optString("difficulty"),
                json.strings("expressions"), json.strings("keyPoints"), json.optString("mediaId", json.getString("id"))
            )
        }
    }
}

internal fun JSONObject.strings(key: String): List<String> {
    val values = optJSONArray(key) ?: return emptyList()
    return List(values.length(), values::getString)
}

enum class Step(val label: String, val completedCoreSteps: Int) {
    Listen("首听", 0), Read("精读", 1), Relisten("复听", 2), Speak("口语复述", 3),
    Write("独立写作", 3), Review("写作自查", 4), Revise("修订", 5);

    val usesLessonPlayer get() = this == Listen || this == Read || this == Relisten
    val coreProgress get() = completedCoreSteps.toFloat() / CORE_STEP_COUNT

    companion object {
        const val CORE_STEP_COUNT = 6
    }
}

enum class SpeakingSupport(val label: String) {
    Structure("结构提示"), Keywords("仅关键词"), None("无提示")
}

data class Practice(
    val lesson: Lesson,
    val id: String = UUID.randomUUID().toString(),
    val step: Step = Step.Listen,
    val target: String = "",
    val keywords: String = "",
    val first: String = "",
    val draft: String = "",
    val marks: List<Int> = emptyList(),
    val position: Int = 0,
    val listenNote: String = "",
    val speakingSupport: SpeakingSupport = SpeakingSupport.Structure,
    val writingCuesOpened: Boolean = false,
    val reviewChecks: Set<String> = emptySet(),
    val speakingChecks: Set<String> = emptySet(),
    val revisionSpeakingChecks: Set<String> = emptySet(),
    val writingFeedback: String = "",
    val revisionFeedback: String = "",
    val revisionFeedbackStale: Boolean = false
) {
    val keywordList get() = keywords.split(',', '，', '、', ';', '；', '\n')
        .map(String::trim).filter(String::isNotEmpty).distinct().take(5)

    fun json() = JSONObject().put("lesson", lesson.json()).put("id", id).put("step", step.name)
        .put("target", target).put("keywords", keywords).put("first", first).put("draft", draft)
        .put("marks", JSONArray(marks)).put("position", position)
        .put("listenNote", listenNote).put("speakingSupport", speakingSupport.name)
        .put("writingCuesOpened", writingCuesOpened)
        .put("reviewChecks", JSONArray(reviewChecks.toList()))
        .put("speakingChecks", JSONArray(speakingChecks.toList()))
        .put("revisionSpeakingChecks", JSONArray(revisionSpeakingChecks.toList()))
        .put("writingFeedback", writingFeedback).put("revisionFeedback", revisionFeedback)
        .put("revisionFeedbackStale", revisionFeedbackStale)

    companion object {
        fun read(json: JSONObject): Practice {
            val marks = json.optJSONArray("marks") ?: JSONArray()
            return Practice(
                Lesson.read(json.getJSONObject("lesson")), json.getString("id"),
                Step.valueOf(json.getString("step")), json.optString("target"),
                json.optString("keywords"), json.optString("first"), json.optString("draft"),
                List(marks.length()) { marks.getInt(it) }, json.optInt("position"),
                json.optString("listenNote"),
                SpeakingSupport.entries.find { it.name == json.optString("speakingSupport") } ?: SpeakingSupport.Structure,
                json.optBoolean("writingCuesOpened"), json.strings("reviewChecks").toSet(),
                json.strings("speakingChecks").toSet(), json.strings("revisionSpeakingChecks").toSet(),
                json.optString("writingFeedback"), json.optString("revisionFeedback"), json.optBoolean("revisionFeedbackStale")
            )
        }
    }
}

data class Completion(
    val id: String, val date: String, val title: String, val first: String, val draft: String,
    val writingCuesOpened: Boolean = false, val completedAt: OffsetDateTime? = null,
    val target: String = "", val retold: Boolean = false, val retoldAfterRevision: Boolean = false
) {
    val hasDrafts get() = first.isNotBlank() && draft.isNotBlank()
}

class PracticeStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("increment", Context.MODE_PRIVATE)

    fun load(builtInLessons: List<Lesson>): Practice? = runCatching {
        val json = prefs.getString("practice", null) ?: return@runCatching null
        val saved = Practice.read(JSONObject(json))
        if (saved.lesson.uri.isNotEmpty()) return@runCatching saved
        val current = builtInLessons.find { it.id == saved.lesson.id } ?: return@runCatching saved
        val offset = if (saved.lesson.duration in 60_000..90_000 && current.duration > 90_000) {
            current.previousClipStart
        } else {
            0
        }
        saved.copy(
            lesson = current,
            position = (saved.position + offset).coerceIn(0, current.duration),
            marks = saved.marks.map { (it + offset).coerceIn(0, current.duration) }
        )
    }.getOrNull()

    fun save(practice: Practice) {
        prefs.edit().putString("practice", practice.json().toString()).apply()
    }

    private fun recordsJson() = JSONArray(prefs.getString("records", "[]"))

    fun history(): List<Completion> {
        val records = recordsJson()
        return List(records.length()) { i ->
            val item = records.getJSONObject(records.length() - 1 - i)
            val completedAt = item.optString("completedAt").takeIf(String::isNotBlank)?.let {
                runCatching { OffsetDateTime.parse(it) }.getOrNull()
            }
            Completion(item.getString("id"), item.getString("date"), item.getString("title"),
                item.optString("first"), item.optString("draft"), item.optBoolean("writingCuesOpened"), completedAt,
                item.optString("target"), item.optBoolean("retold"), item.optBoolean("retoldAfterRevision"))
        }
    }

    fun complete(practice: Practice, retold: Boolean, retoldAfterRevision: Boolean): Boolean {
        if (practice.step != Step.Revise || practice.draft.isBlank() || practice.first.isBlank()) return false
        val records = recordsJson()
        val saved = prefs.getString("practice", null)?.let(::JSONObject)
        val alreadyRecorded = (0 until records.length()).any { records.getJSONObject(it).getString("id") == practice.id }
        if (saved == null) {
            if (!alreadyRecorded) return false
        } else if (saved.optString("id") != practice.id || saved.optString("step") != Step.Revise.name) {
            return false
        }
        if (!alreadyRecorded) {
            val now = OffsetDateTime.now()
            records.put(JSONObject().put("id", practice.id).put("date", now.toLocalDate().toString())
                .put("completedAt", now.toString()).put("title", practice.lesson.title)
                .put("first", practice.first).put("draft", practice.draft).put("writingCuesOpened", practice.writingCuesOpened)
                .put("target", practice.target).put("retold", retold).put("retoldAfterRevision", retoldAfterRevision))
        }
        return prefs.edit().putString("records", records.toString()).remove("practice").commit()
    }

    fun theme() = prefs.getString("theme", "系统") ?: "系统"
    fun theme(value: String) = prefs.edit().putString("theme", value).apply()
}

private val wordPattern = Regex("[A-Za-z]+(?:[’'-][A-Za-z]+)*")
fun wordCount(text: String) = wordPattern.findAll(text).count()
fun timeText(ms: Int): String {
    val seconds = ms.coerceAtLeast(0) / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private val clockPattern = Regex("""(?:(\d+):)?([0-5]?\d):([0-5]\d)(?:[.,](\d{1,3}))?""")
private fun parseClock(value: String): Int {
    val match = clockPattern.matchEntire(value) ?: error("无法识别时间戳 $value")
    val hourField = match.groupValues[1]
    val hours = if (hourField.isEmpty()) 0L else hourField.toLongOrNull() ?: error("时间戳超出范围")
    require(hours < 596) { "时间戳超出范围" }
    val millis = ((hours * 60 + match.groupValues[2].toInt()) * 60 +
        match.groupValues[3].toInt()) * 1000 + match.groupValues[4].padEnd(3, '0').toInt()
    require(millis <= Int.MAX_VALUE) { "时间戳超出范围" }
    return millis.toInt()
}

fun parseSubtitles(input: String, mediaDuration: Int, offset: Int = 0): List<Cue> {
    val normalized = input.removePrefix("\uFEFF").replace("\r\n", "\n").replace('\r', '\n').trim()
    require(normalized.isNotBlank()) { "字幕文件为空" }
    val cues = if (normalized.startsWith("[")) {
        val entries = normalized.lines().filter(String::isNotBlank).map { line ->
            val match = Regex("""^\[([^]]+)]\s*(.+)$""").matchEntire(line.trim())
                ?: error("TXT 每行须使用 [00:12.500] Sentence text 格式")
            parseClock(match.groupValues[1]) to match.groupValues[2].trim()
        }
        val lastEnd = (mediaDuration.toLong() - offset.coerceAtMost(0)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        entries.mapIndexed { index, (start, text) ->
            Cue(start, entries.getOrNull(index + 1)?.first ?: lastEnd, text)
        }
    } else {
        normalized.split(Regex("\\n[ \\t]*\\n+")).mapNotNull { block ->
            val lines = block.lines()
            val header = lines.first().trim()
            if (header.startsWith("WEBVTT") || header.startsWith("NOTE") ||
                header == "STYLE" || header == "REGION") return@mapNotNull null
            val index = lines.indexOfFirst { "-->" in it }
            require(index in 0..1) { "缺少 SRT 或 WebVTT 起止时间" }
            val parts = lines[index].split("-->", limit = 2)
            Cue(
                parseClock(parts[0].trim()),
                parseClock(parts[1].trim().takeWhile { !it.isWhitespace() }),
                lines.drop(index + 1).joinToString("\n").replace(Regex("<[^>]*>"), "").trim()
            )
        }
    }
    require(cues.isNotEmpty() && cues.all { it.end > it.start && it.text.isNotBlank() }) {
        "每条字幕需要有效时间范围和正文"
    }
    require(cues.zipWithNext().all { (a, b) -> a.start <= b.start }) { "字幕开始时间必须按顺序排列" }
    return cues
}

fun clipCues(cues: List<Cue>, start: Int, end: Int, offset: Int): List<Cue> =
    cues.mapNotNull { cue ->
        val clippedStart = (cue.start.toLong() + offset).coerceAtLeast(start.toLong())
        val clippedEnd = (cue.end.toLong() + offset).coerceAtMost(end.toLong())
        if (clippedStart < clippedEnd) cue.copy(start = clippedStart.toInt(), end = clippedEnd.toInt()) else null
    }

internal data class FocusedSentence(val text: String, val firstCue: Int, val lastCue: Int)

internal fun sentencesTouchingCue(cues: List<Cue>, index: Int): List<FocusedSentence> {
    val windowStart = (index - 4).coerceAtLeast(0)
    val windowEnd = (index + 5).coerceAtMost(cues.size)
    val transcript = StringBuilder()
    val ranges = (windowStart until windowEnd).map { i ->
        if (transcript.isNotEmpty()) transcript.append(' ')
        val start = transcript.length
        transcript.append(normalizeCaption(cues[i].text))
        start until transcript.length
    }
    val selected = ranges[index - windowStart]
    val text = transcript.toString()
    val iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH).apply { setText(text) }
    return buildList {
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE && start <= selected.last) {
            val sentence = text.substring(start, end).trimEnd()
            val contentEnd = start + sentence.length
            if (contentEnd > selected.first) {
                val first = ranges.indexOfFirst { it.last >= start }
                val last = ranges.indexOfLast { it.first < contentEnd }
                add(FocusedSentence(sentence.trimStart(), first + windowStart, last + windowStart))
            }
            start = end
            end = iterator.next()
        }
    }
}

internal fun normalizeCaption(text: String): String = text.trim().replace(Regex("\\s+"), " ")
