package com.increment.app

import android.content.Context
import org.json.JSONArray

internal class LessonCatalog(context: Context) {
    val originals = context.readEntries("lessons.json").map(Lesson::read)
    val assessments = context.readEntries("assessments.json").map(Lesson::read)
}

private fun Context.readEntries(asset: String) =
    JSONArray(assets.open(asset).bufferedReader().use { it.readText() }).let { entries ->
        List(entries.length(), entries::getJSONObject)
    }
