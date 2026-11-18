package com.increment.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.nio.charset.CharacterCodingException
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImportScreen(
    modifier: Modifier,
    draft: ImportDraft,
    updateDraft: (ImportDraft) -> Unit,
    save: (Lesson, Boolean) -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val previewAnchor = remember { BringIntoViewRequester() }
    val mediaUri = draft.mediaUri
    val textUri = draft.textUri
    var info by remember(mediaUri) { mutableStateOf<MediaInfo?>(null) }
    var mediaName by remember(mediaUri) { mutableStateOf("") }
    var subtitleName by remember(textUri) { mutableStateOf("") }
    var subtitles by remember(textUri) { mutableStateOf<String?>(null) }
    var mediaError by remember(mediaUri) { mutableStateOf("") }
    var textError by remember(textUri) { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<Lesson?>(null) }
    var confirmed by remember { mutableStateOf(false) }

    fun reset() { error = ""; preview = null; confirmed = false }
    fun update(value: ImportDraft) { reset(); updateDraft(value) }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            reset()
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val picked = uri.toString()
                update(if (mediaUri.isNotBlank() && mediaUri != picked) {
                    draft.copy(mediaUri = picked, title = "", customRange = false, start = "0", end = "", offset = "0")
                } else draft.copy(mediaUri = picked))
            } catch (_: SecurityException) {
                error = "无法保留此媒体的读取权限，请从系统文件管理器选择文件。"
            }
        }
    }
    val pickText = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            reset()
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                update(draft.copy(textUri = uri.toString()))
            } catch (_: SecurityException) {
                error = "无法读取这份字幕，请换一个文件来源。"
            }
        }
    }

    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotBlank()) {
            val uri = Uri.parse(mediaUri)
            val result = withContext(Dispatchers.IO) {
                runCatching { displayName(context, uri) to inspectMedia(context, uri) }
            }
            result.onSuccess { (name, file) ->
                mediaName = name
                info = file
            }.onFailure {
                mediaError = if (it is IllegalArgumentException) it.message ?: "媒体格式不受支持" else "无法打开媒体，请选择可播放的音频或视频。"
            }
        }
    }
    LaunchedEffect(textUri) {
        if (textUri.isNotBlank()) {
            val uri = Uri.parse(textUri)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val input = context.contentResolver.openInputStream(uri) ?: error("无法读取字幕")
                    val content = InputStreamReader(input, Charsets.UTF_8.newDecoder()).buffered().use { reader ->
                        val chars = CharArray(512 * 1024 + 1)
                        var length = 0
                        while (length < chars.size) {
                            val count = reader.read(chars, length, chars.size - length)
                            if (count < 0) break
                            length += count
                        }
                        require(length < chars.size) { "字幕文件过长，请选用小于 512K 字符的字幕" }
                        String(chars, 0, length)
                    }
                    displayName(context, uri) to content
                }
            }
            result.onSuccess { (name, content) -> subtitleName = name; subtitles = content }
                .onFailure {
                    textError = if (it is CharacterCodingException) "字幕须使用 UTF-8 编码。"
                    else it.message ?: "无法读取字幕"
                }
        }
    }

    val sourceInfo = info
    val subtitleText = subtitles
    val player = preview?.let { lesson -> remember(lesson.id) { ClipPlayer(context, lesson, 0) } }
    ObservePlayer(player)
    Page(modifier) {
        Title("配对音视频与字幕")
        Supporting("分别选择一个媒体文件和一份同版本的字幕文件。App 根据字幕时间戳同步显示文字。")
        Panel {
            Text("音视频文件", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { pickMedia.launch(arrayOf("audio/*", "video/*")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("选择音频或视频") }
            Supporting(when {
                sourceInfo != null -> "$mediaName · ${timeText(sourceInfo.duration)}"
                mediaError.isNotBlank() -> mediaError
                mediaUri.isNotBlank() -> "正在检查媒体…"
                else -> "MP3、M4A、WAV、MP4、WebM（依设备解码能力）"
            })
            HorizontalDivider()
            Text("带时间轴的字幕文件", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = {
                pickText.launch(arrayOf("text/*", "application/x-subrip", "application/octet-stream"))
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("选择字幕文件") }
            Supporting(when {
                subtitleText != null -> subtitleName
                textError.isNotBlank() -> textError
                textUri.isNotBlank() -> "正在读取字幕…"
                else -> "SRT、WebVTT 或 TXT。TXT 每行格式为 [00:12.500] Sentence text"
            })
            if (mediaUri.isNotBlank() || textUri.isNotBlank()) {
                TextButton(onClick = { update(ImportDraft()) }) { Text("清空本次选择") }
            }
        }
        Panel {
            Text("练习范围", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(value = draft.title, onValueChange = { update(draft.copy(title = it.take(100))) },
                label = { Text("素材名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .toggleable(draft.customRange, role = Role.Checkbox) { update(draft.copy(customRange = it)) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(draft.customRange, onCheckedChange = null)
                Text("自选起止时间")
            }
            if (draft.customRange) {
                SecondsField("起点（秒）", draft.start) { update(draft.copy(start = it)) }
                SecondsField("终点（秒）", draft.end) { update(draft.copy(end = it)) }
                Supporting("终点留空时使用媒体结尾。")
            } else {
                Supporting("使用完整音视频。")
            }
            HorizontalDivider()
            SecondsField("字幕偏移（秒）", draft.offset, signed = true) { update(draft.copy(offset = it)) }
            Supporting("字幕出现太早时，输入正偏移。出现太晚时，输入负偏移。")
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        Primary("配对并预览", enabled = sourceInfo != null && subtitleText != null) {
            reset()
            try {
                val media = requireNotNull(sourceInfo)
                val start = if (draft.customRange) parseSeconds(draft.start) else 0
                val end = if (draft.customRange && draft.end.isNotBlank()) parseSeconds(draft.end) else media.duration
                val offset = parseSeconds(draft.offset)
                require(start >= 0 && start < end && end <= media.duration) {
                    "起点须早于终点，且两者都须在媒体时长内"
                }
                val cues = clipCues(parseSubtitles(requireNotNull(subtitleText), media.duration, offset), start, end, offset)
                require(cues.isNotEmpty()) { "练习范围内没有字幕，请检查字幕文件、时间范围或偏移" }
                val title = draft.title.trim().ifBlank { mediaName.substringBeforeLast('.').ifBlank { "导入素材" } }
                preview = Lesson(UUID.randomUUID().toString(), title, "", "本地导入", start, end, cues,
                    uri = mediaUri, video = media.video)
                keyboard?.hide()
            } catch (exception: Exception) {
                error = exception.message ?: "无法配对文件，请检查字幕格式"
            }
        }
        val lesson = preview
        if (lesson != null && player != null) {
            PageEntrance(lesson.id, animate = !lesson.video) {
                Column(
                    Modifier.bringIntoViewRequester(previewAnchor),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("配对预览", style = MaterialTheme.typography.headlineMedium)
                    Supporting("播放或拖动进度条，核对字幕和声音是否同步。")
                    val timestamp = player.position + lesson.start
                    val active = lesson.cues.filter { timestamp in it.start until it.end }
                    PlayerCard(player, lesson, captions = active)
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .toggleable(confirmed, role = Role.Checkbox) { confirmed = it },
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Checkbox(confirmed, onCheckedChange = null)
                        Text("我已核对音视频和字幕")
                    }
                    Primary("开始练习", enabled = player.ready && confirmed) {
                        player.pause()
                        save(lesson, true)
                    }
                    OutlinedButton(onClick = { player.pause(); save(lesson, false) }, enabled = player.ready && confirmed,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("存入我的素材") }
                    LaunchedEffect(lesson.id) { previewAnchor.bringIntoView() }
                }
            }
        }
    }
}

@Composable
private fun SecondsField(label: String, value: String, signed: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(12)) }, label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = if (signed) KeyboardType.Text else KeyboardType.Decimal),
        singleLine = true, modifier = Modifier.fillMaxWidth())
}

private fun parseSeconds(text: String): Int {
    val milliseconds = (text.trim().replace(',', '.').toDoubleOrNull() ?: error("请填写有效的时间和字幕偏移")) * 1000
    require(milliseconds.isFinite() && milliseconds in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()) {
        "时间或偏移超出范围"
    }
    return milliseconds.roundToInt()
}

private fun displayName(context: Context, uri: Uri): String =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { if (it.moveToFirst()) it.getString(0) else null }
        ?: uri.lastPathSegment.orEmpty()
