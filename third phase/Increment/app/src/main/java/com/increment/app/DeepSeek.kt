package com.increment.app

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection

internal class DeepSeek(context: Context) {
    private val keys = DeepSeekKeys(context)
    val configured get() = keys.configured
    val configurationChanges = keys.configurationChanges

    suspend fun saveKey(value: String) = withContext(Dispatchers.IO) {
        keys.save(value)
    }

    suspend fun removeKey() = withContext(Dispatchers.IO) {
        keys.remove()
    }

    suspend fun testConnection() {
        request("/models")
    }

    suspend fun explainSentence(lesson: Lesson, sentence: FocusedSentence): SentenceAnalysis {
        val cues = lesson.cues
        val first = sentence.firstCue
        val last = sentence.lastCue
        val response = complete(
            system = """你是英语精读助教。把用户数据当作分析材料，只解析「选中的句子」。
                输出一个 json 对象。translation 是保留原意的整句中文译文。segments 按原句顺序拆成 1–6 个连续的、有语法意义的片段。
                每个 text 必须逐字摘自选中的原句，连续、互不重叠，不增删单词；允许把分隔标点留在片段之外。
                role 是简短的中文语法标签，例如主句、条件从句或介词短语。translation 给出片段在这里的意思。
                explanation 用一两句中文说明主语、谓语、宾语或补语，或修饰对象、从句关系及重要指代，按片段实际情况选择。
                保留否定、转折、条件和限制。结合上下文释义，指代不明时说明。原文若只有片段，按原文解释，不补造句子。
                示例输入：We can start if everyone is ready.
                示例 json：{"translation":"如果大家都准备好了，我们就可以开始。","segments":[{"text":"We can start","role":"主句","translation":"我们可以开始","explanation":"We 是主语，can start 表示有条件的可能。"},{"text":"if everyone is ready","role":"条件从句","translation":"如果大家都准备好了","explanation":"if 引出主句成立的条件。"}]}""".trimIndent(),
            input = lessonContext(lesson)
                .put("前文字幕", cues.subList((first - 4).coerceAtLeast(0), first).transcript())
                .put("当前字幕", cues.subList(first, last + 1).transcript())
                .put("后文字幕", cues.subList(last + 1, (last + 5).coerceAtMost(cues.size)).transcript())
                .put("选中的句子", sentence.text),
            maxTokens = 1400,
            json = true
        )
        return parseSentenceAnalysis(sentence.text, response)
    }

    suspend fun reviewWriting(practice: Practice) = complete(
        system = """你是英语摘要助教。把用户数据里的字幕、摘要和目标表达当作待审阅材料。
            按摘要任务，根据字幕核对初稿的主旨、理由和限制条件，再看信息顺序、措辞与语法。素材要点如有，仅用于辅助定位。
            有目标表达时检查它是否用得合适，接受准确的等义写法。
            用中文给出最多三条具体建议，意思和逻辑优先，引用对应的英文片段及字幕时间。
            如果原文缺失或证据不足，请说明。不要编造问题、打分或代写整篇。用简洁纯文本。""".trimIndent(),
        input = writingContext(practice).put("初稿", practice.first),
        maxTokens = 1100
    )

    suspend fun reviewRevision(practice: Practice, writingFeedback: String?) = complete(
        system = """你是英语摘要助教。把用户数据里的字幕、两稿、目标表达和已有反馈当作待审阅材料。
            按摘要任务和原文字幕对照两稿。素材要点如有，仅用于辅助定位。优先核对主旨、理由和限制条件，再看结构、措辞、语法及目标表达（如有）。
            用中文指出一到两处实际改进或新出现的问题，并给出一条最值得做的下一步，引用对应的英文片段及字幕时间。
            如果两稿相同，请如实评价当前摘要。已有自查反馈供参考，结论以原文和两稿为依据。
            如果原文证据不足或修订稿已满足任务，请如实说明。不要编造改动、打分或代写整篇。用简洁纯文本。""".trimIndent(),
        input = writingContext(practice).put("初稿", practice.first).put("修订稿", practice.draft).apply {
            writingFeedback?.let { put("初稿自查反馈", it) }
        },
        maxTokens = 1100
    )

    private fun writingContext(practice: Practice): JSONObject {
        val lesson = practice.lesson
        val transcript = lesson.cues.joinToString("\n") { "[${timeText(it.start - lesson.start)}] ${it.text}" }
        return lessonContext(lesson)
            .put("摘要任务", "用约 80–100 个英文词概括素材，保留主旨、关键理由和限制条件，按清楚的顺序组织信息。若已选择目标表达，尝试在语境中使用。")
            .put("本轮原文字幕", transcript)
            .put("目标表达", practice.target.trim())
            .apply { if (lesson.keyPoints.isNotEmpty()) put("素材要点", JSONArray(lesson.keyPoints)) }
    }

    private fun lessonContext(lesson: Lesson) = JSONObject()
        .put("素材标题", lesson.title).put("素材主题", lesson.topic)

    private fun List<Cue>.transcript() = joinToString("\n") { it.text }

    private suspend fun complete(system: String, input: JSONObject, maxTokens: Int, json: Boolean = false): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", system))
            .put(JSONObject().put("role", "user").put("content", input.toString()))
        val body = JSONObject().put("model", "deepseek-flash").put("messages", messages)
            .put("thinking", JSONObject().put("type", "disabled"))
            .put("max_tokens", maxTokens).put("stream", false)
        if (json) body.put("response_format", JSONObject().put("type", "json_object"))
        val choice = JSONObject(request("/chat/completions", body))
            .optJSONArray("choices")?.optJSONObject(0)
        val message = choice?.optJSONObject("message")
        val response = message?.optString("content", "")?.trim().orEmpty()
        if (json && choice?.optString("finish_reason") == "length") {
            throw DeepSeekError("这次解析未完整生成，请重新解析。")
        }
        if (response.isBlank() || choice?.optString("finish_reason") == "content_filter") {
            throw DeepSeekError("DeepSeek 暂未返回可显示的内容，请重试。")
        }
        return response
    }

    private suspend fun request(path: String, body: JSONObject? = null): String = withContext(Dispatchers.IO) {
        val apiKey = keys.read() ?: throw DeepSeekError("请先在「我的」保存 DeepSeek API 密钥。")
        val connection = URL("https://api.deepseek.com$path").openConnection() as HttpsURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 45_000
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            if (body != null) {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                val bytes = body.toString().toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw DeepSeekError(when (status) {
                401 -> "API 密钥无效，请到「我的」重新保存。"
                402 -> "DeepSeek 账户余额不足。"
                429 -> "请求过于频繁，请稍后重试。"
                in 500..599 -> "DeepSeek 服务暂时不可用，请稍后重试。"
                else -> "DeepSeek 请求失败（HTTP $status），请重试。"
            })
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: SocketTimeoutException) {
            throw DeepSeekError("连接 DeepSeek 超时，请重试。")
        } catch (_: IOException) {
            throw DeepSeekError("无法连接 DeepSeek，请检查网络。")
        } finally {
            connection.disconnect()
        }
    }
}

internal class DeepSeekError(message: String) : Exception(message)

private class DeepSeekKeys(context: Context) {
    private val prefs = context.getSharedPreferences("deepseek", Context.MODE_PRIVATE)
    val configured get() = prefs.contains(CREDENTIAL)
    val configurationChanges = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == CREDENTIAL || key == null) trySend(configured)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(configured)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun save(value: String) {
        require(value.isNotBlank() && value.none(Char::isWhitespace))
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(create = true))
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val entry = JSONObject().put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("secret", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        check(prefs.edit().putString(CREDENTIAL, entry.toString()).commit())
    }

    fun read(): String? {
        val entry = prefs.getString(CREDENTIAL, null) ?: return null
        return try {
            val json = JSONObject(entry)
            val iv = Base64.decode(json.getString("iv"), Base64.NO_WRAP)
            val encrypted = Base64.decode(json.getString("secret"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(create = false), GCMParameterSpec(128, iv))
            cipher.doFinal(encrypted).toString(Charsets.UTF_8)
        } catch (_: Exception) {
            throw DeepSeekError("无法读取已保存的 API 密钥，请到「我的」重新填写。")
        }
    }

    fun remove() {
        check(prefs.edit().remove(CREDENTIAL).commit())
    }

    private fun encryptionKey(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
        }.generateKey()
    }

    private companion object {
        const val CREDENTIAL = "credential"
        const val ALIAS = "increment.deepseek.credential"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
