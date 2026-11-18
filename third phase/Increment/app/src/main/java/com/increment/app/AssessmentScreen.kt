package com.increment.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Composable
internal fun AssessmentScreen(
    modifier: Modifier,
    lessons: List<Lesson>,
    files: File,
    store: AssessmentStore,
    message: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaded = remember(store) { runCatching(store::load) }
    var attempts by remember(store) { mutableStateOf(loaded.getOrDefault(emptyList())) }
    val current = attempts.lastOrNull()?.takeUnless { it.step == AssessmentStep.Done }
    val finished = attempts.filter { it.step == AssessmentStep.Done }
    fun update(next: List<AssessmentAttempt>, submit: Boolean = false): Boolean {
        if (!store.save(next, submit)) {
            message("无法保存测评答卷，请重试。")
            return false
        }
        attempts = next
        return true
    }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val report = assessmentReport(finished, lessons)
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("无法打开文件")
                        stream.bufferedWriter().use { it.write(report) }
                    }.isSuccess
                }
                message(if (success) "测评答卷已导出。" else "无法导出答卷，请重新选择保存位置。")
            }
        }
    }

    if (loaded.isFailure) {
        Page(modifier) { Supporting("暂时无法读取本机测评答卷。") }
    } else if (current != null) {
        val lesson = lessons.first { it.id == current.materialId }
        key(current.id) {
            AssessmentAttemptScreen(
                modifier, lesson, current, attempts.size, files, message,
                change = { next ->
                    if (attempts.lastOrNull()?.id == next.id && attempts.last().step != AssessmentStep.Done) {
                        update(attempts.dropLast(1) + next)
                    }
                },
                submit = { submitted ->
                    if (attempts.lastOrNull()?.id == submitted.id && attempts.last().step != AssessmentStep.Done) {
                        val result = submitted.copy(step = AssessmentStep.Done, completedAt = OffsetDateTime.now())
                        if (update(attempts.dropLast(1) + result, submit = true)) {
                            discardSpeech(files, result.id)
                        }
                    }
                }
            )
        }
    } else {
        Page(modifier) {
            Title("阶段测评")
            if (finished.isEmpty()) {
                var firstId by rememberSaveable { mutableStateOf(lessons.random().id) }
                Supporting("做两次独立测评，建议间隔 3–7 天。每次完整听两遍，记录理解，阅读原文 90 秒，再独立写英文摘要。")
                Supporting("两次都用相同的播放速度、阅读时长和写作要求。测评原文只在限时阅读中显示。")
                Panel {
                    Text("首次测评素材", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        lessons.forEachIndexed { index, lesson ->
                            FilterChip(selected = firstId == lesson.id, onClick = { firstId = lesson.id },
                                label = { Text("R${index + 1}") })
                        }
                    }
                }
                AssessmentStart(lessons.first { it.id == firstId }, "开始首次测评") { familiar, speaking ->
                    update(listOf(AssessmentAttempt(firstId, familiar = familiar, speaking = speaking)))
                }
            } else if (finished.size == 1) {
                val first = finished.single()
                val day = requireNotNull(first.completedAt).toLocalDate()
                Panel {
                    Text("首次测评已提交", style = MaterialTheme.typography.titleMedium)
                    Supporting("完成日期 ${day}，建议在 ${day.plusDays(3)} 至 ${day.plusDays(7)} 再做第二次。")
                }
                val next = lessons.first { it.id != first.materialId }
                AssessmentStart(next, "开始第二次测评", initialSpeaking = first.speaking) { familiar, speaking ->
                    update(attempts + AssessmentAttempt(next.id, familiar = familiar, speaking = speaking))
                }
            } else {
                val firstDay = finished.first().completedAt?.toLocalDate()
                val lastDay = finished.last().completedAt?.toLocalDate()
                if (firstDay != null && lastDay != null) {
                    Supporting("两次测评均已完成，实际间隔 ${ChronoUnit.DAYS.between(firstDay, lastDay)} 天。")
                }
                finished.forEachIndexed { index, result ->
                    val lesson = lessons.first { it.id == result.materialId }
                    AssessmentResult(index, lesson, result)
                }
                OutlinedButton(onClick = { export.launch("Increment-阶段测评.txt") }) { Text("导出两次答卷") }
            }
        }
    }
}

@Composable
private fun AssessmentStart(
    lesson: Lesson,
    button: String,
    initialSpeaking: Boolean = false,
    start: (Boolean, Boolean) -> Unit
) {
    var familiar by rememberSaveable(lesson.id) { mutableStateOf<Boolean?>(null) }
    var speaking by rememberSaveable(lesson.id) { mutableStateOf(initialSpeaking) }
    Panel {
        Text(lesson.title, style = MaterialTheme.typography.titleMedium)
        Supporting("测评前听过这段材料吗？")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = familiar == false, onClick = { familiar = false }, label = { Text("没听过") })
            FilterChip(selected = familiar == true, onClick = { familiar = true }, label = { Text("听过") })
        }
        FilterChip(selected = speaking, onClick = { speaking = !speaking }, label = { Text("加练口语（可选）") })
        Primary(button, enabled = familiar != null) { start(familiar == true, speaking) }
    }
}

@Composable
private fun AssessmentAttemptScreen(
    modifier: Modifier,
    lesson: Lesson,
    attempt: AssessmentAttempt,
    round: Int,
    files: File,
    message: (String) -> Unit,
    change: (AssessmentAttempt) -> Unit,
    submit: (AssessmentAttempt) -> Unit
) {
    Page(modifier) {
        Text("第 ${round} 次测评", style = MaterialTheme.typography.labelLarge)
        Title(lesson.title)
        key(attempt.step) {
            when (attempt.step) {
                AssessmentStep.Listen -> AssessmentListening(lesson, attempt, change)
                AssessmentStep.Understand -> {
                    Supporting("根据两遍首听写下主旨、理由和限制条件，然后再阅读原文。")
                    AssessmentField("听力理解", attempt.comprehension) { change(attempt.copy(comprehension = it)) }
                    Primary("继续阅读", enabled = attempt.comprehension.isNotBlank()) {
                        change(attempt.copy(step = AssessmentStep.Read))
                    }
                }
                AssessmentStep.Read -> AssessmentReading(lesson, attempt, change)
                AssessmentStep.Write -> {
                    Supporting("独立写 80–100 词英文摘要，保留材料主旨、关键理由和限制条件。")
                    AssessmentField("英文摘要", attempt.summary, supporting = "${wordCount(attempt.summary)} 词") {
                        change(attempt.copy(summary = it))
                    }
                    Primary(if (attempt.speaking) "继续口语" else "提交本次测评",
                        enabled = wordCount(attempt.summary) > 0) {
                        if (attempt.speaking) change(attempt.copy(step = AssessmentStep.Speak)) else submit(attempt)
                    }
                }
                AssessmentStep.Speak -> {
                    val practice = remember(lesson, attempt.id) {
                        Practice(lesson, id = attempt.id, speakingSupport = SpeakingSupport.None)
                    }
                    Speaking(practice, files, message, changeSupport = {}, task = SpeakingTask.Assessment,
                        checks = attempt.speakingChecks, onChecks = { change(attempt.copy(speakingChecks = it)) }) {
                        val hasRecording = speechFile(files, attempt.id).isFile
                        submit(attempt.copy(
                            recorded = hasRecording,
                            speakingChecks = if (hasRecording) attempt.speakingChecks else emptySet()
                        ))
                    }
                }
                AssessmentStep.Done -> Unit
            }
        }
    }
}

@Composable
private fun AssessmentListening(lesson: Lesson, attempt: AssessmentAttempt, change: (AssessmentAttempt) -> Unit) {
    val context = LocalContext.current
    val player = remember(lesson) { ClipPlayer(context, lesson, 0) }
    var pending by remember { mutableStateOf(false) }
    val required = AssessmentAttempt.REQUIRED_LISTENS
    ObservePlayer(player)
    LaunchedEffect(player.position, player.playing, pending) {
        if (pending && !player.playing && player.position >= lesson.duration && attempt.listens < required) {
            pending = false
            change(attempt.copy(listens = attempt.listens + 1))
        }
    }

    Panel {
        Text("无字幕听力", style = MaterialTheme.typography.titleMedium)
        Supporting("已完整播放 ${attempt.listens} / ${required} 遍 · 每遍 ${timeText(lesson.duration)}")
        if (player.playing) {
            LinearProgressIndicator(progress = { player.position.toFloat() / lesson.duration },
                modifier = Modifier.fillMaxWidth())
        }
        if (player.error.isNotBlank()) Text(player.error, color = MaterialTheme.colorScheme.error)
        if (attempt.listens < required) {
            Primary(if (player.playing) "正在播放" else "播放第 ${attempt.listens + 1} 遍",
                enabled = player.ready && !player.playing && (!pending || player.position < lesson.duration)) {
                pending = true
                player.play(0)
            }
        } else {
            Primary("记录听到的内容") { change(attempt.copy(step = AssessmentStep.Understand)) }
        }
    }
}

@Composable
private fun AssessmentReading(lesson: Lesson, attempt: AssessmentAttempt, change: (AssessmentAttempt) -> Unit) {
    var now by remember(attempt.readUntil) { mutableLongStateOf(System.currentTimeMillis()) }
    val remaining = ((attempt.readUntil - now + 999) / 1000).coerceAtLeast(0).toInt()
    LaunchedEffect(attempt.readUntil) {
        while (now < attempt.readUntil) {
            delay(250)
            now = System.currentTimeMillis()
        }
    }
    when {
        attempt.readUntil == 0L -> {
            Supporting("原文开放 90 秒。时间到后隐藏原文并进入写作。")
            Primary("开始限时阅读") {
                change(attempt.copy(readUntil = System.currentTimeMillis() + AssessmentAttempt.READING_SECONDS * 1000L))
            }
        }
        remaining > 0 -> {
            Text("剩余 ${remaining} 秒", style = MaterialTheme.typography.titleMedium)
            Text(lesson.cues.joinToString(" ") { it.text }, style = MaterialTheme.typography.bodyLarge)
        }
        else -> LaunchedEffect(attempt.id) { change(attempt.copy(step = AssessmentStep.Write)) }
    }
}

@Composable
private fun AssessmentField(label: String, value: String, supporting: String? = null, change: (String) -> Unit) {
    OutlinedTextField(
        value, { change(it.take(AssessmentAttempt.MAX_ANSWER_LENGTH)) },
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, minLines = 5, maxLines = 12,
        supportingText = if (supporting == null) null else {{ Text(supporting) }}
    )
}

@Composable
private fun AssessmentResult(index: Int, lesson: Lesson, result: AssessmentAttempt) {
    Panel {
        Text("第 ${index + 1} 次 · ${lesson.title}", style = MaterialTheme.typography.titleMedium)
        Supporting("提交时间 ${result.completedAt?.toLocalDate()} · ${if (result.familiar) "听过材料" else "首次听到"}")
        Text("听力理解", style = MaterialTheme.typography.labelLarge)
        Text(result.comprehension)
        Text("独立摘要", style = MaterialTheme.typography.labelLarge)
        Text(result.summary)
        Supporting(speakingResult(result))
    }
}

private fun speakingResult(result: AssessmentAttempt): String = when {
    !result.speaking -> "未选择口语"
    !result.recorded -> "已选择口语，本次无录音"
    result.speakingChecks.isEmpty() -> "已录音，未填写自评"
    else -> "口语自评 · " + result.speakingChecks.joinToString("、")
}

private fun assessmentReport(results: List<AssessmentAttempt>, lessons: List<Lesson>): String = buildString {
    appendLine("Increment 阶段测评")
    results.forEachIndexed { index, result ->
        val lesson = lessons.first { it.id == result.materialId }
        appendLine()
        appendLine("第 ${index + 1} 次 · ${lesson.title}")
        appendLine("提交时间 ${result.completedAt}")
        appendLine("材料熟悉度 ${if (result.familiar) "听过" else "没听过"}")
        appendLine("完整收听 ${result.listens} 遍 · 原文 ${AssessmentAttempt.READING_SECONDS} 秒")
        appendLine(speakingResult(result))
        appendLine("听力理解")
        appendLine(result.comprehension)
        appendLine("独立摘要")
        appendLine(result.summary)
    }
}
