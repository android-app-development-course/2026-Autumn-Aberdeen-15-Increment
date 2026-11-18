package com.increment.app

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal data class ImportDraft(
    val mediaUri: String = "",
    val textUri: String = "",
    val title: String = "",
    val customRange: Boolean = false,
    val start: String = "0",
    val end: String = "",
    val offset: String = "0"
) {
    fun json() = JSONObject()
        .put("mediaUri", mediaUri).put("textUri", textUri).put("title", title)
        .put("customRange", customRange).put("start", start).put("end", end).put("offset", offset)

    companion object {
        fun read(json: JSONObject) = ImportDraft(
            mediaUri = json.optString("mediaUri"),
            textUri = json.optString("textUri"),
            title = json.optString("title"),
            customRange = json.optBoolean("customRange"),
            start = json.optString("start", "0"),
            end = json.optString("end"),
            offset = json.optString("offset", "0")
        )
    }
}

internal class LocalLibrary(context: Context) {
    private val prefs = context.getSharedPreferences("increment", Context.MODE_PRIVATE)
    private val file = AtomicFile(File(context.filesDir, "materials.json"))

    fun read(): List<Lesson> {
        val content = try {
            file.readFully()
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        val entries = JSONArray(content.toString(Charsets.UTF_8))
        return List(entries.length()) { Lesson.read(entries.getJSONObject(it)) }
    }

    fun write(lessons: List<Lesson>) {
        val content = JSONArray(lessons.map(Lesson::json)).toString().toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(content)
            file.finishWrite(stream)
        } catch (error: IOException) {
            file.failWrite(stream)
            throw error
        }
    }

    fun draft(): ImportDraft = prefs.getString("importDraft", null)?.let { ImportDraft.read(JSONObject(it)) }
        ?: ImportDraft()

    fun saveDraft(draft: ImportDraft) {
        prefs.edit().putString("importDraft", draft.json().toString()).apply()
    }
}
