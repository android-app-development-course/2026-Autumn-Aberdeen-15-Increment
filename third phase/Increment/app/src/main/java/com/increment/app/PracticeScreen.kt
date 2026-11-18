package com.increment.app

import android.Manifest
import android.content.pm.PackageManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
private fun PracticeProgress(
    step: Step,
    canGoBack: Boolean,
    canGoNext: Boolean,
    goBack: () -> Unit,
    goNext: () -> Unit
) {
    val progress by animateFloatAsState(
        targetValue = step.coreProgress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "练习进度"
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (step == Step.Speak) "可选口语"
                else "第 ${step.completedCoreSteps + 1} 步 / 共 ${Step.CORE_STEP_COUNT} 步",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(onClick = goBack, enabled = canGoBack, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("上一步")
            }
            TextButton(onClick = goNext, enabled = canGoNext, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("下一步")
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = "核心练习进度" }
        )
    }
}

@Composable
internal fun PracticeScreen(
    practice: Practice, files: File, deepSeek: DeepSeek, update: (Practice) -> Unit, message: (String) -> Unit,
    modifier: Modifier = Modifier,
    complete: (Practice) -> Unit
) {
    val lesson = practice.lesson
    val step = practice.step
    var showWritingOptions by rememberSaveable(step) { mutableStateOf(false) }
    val context = LocalContext.current
    val player = if (step.usesLessonPlayer) {
        remember(lesson.id, step) { ClipPlayer(context, lesson, practice.position) }
    } else null

    val previousStep = when (step) {
        Step.Listen -> null
        Step.Read -> Step.Listen
        Step.Relisten -> Step.Read
        Step.Speak, Step.Write -> Step.Relisten
        Step.Review -> Step.Write
        Step.Revise -> Step.Review
    }
    val canGoNext = when (step) {
        Step.Listen, Step.Relisten -> player?.ready == true
        Step.Write -> wordCount(practice.draft) > 0
        Step.Read, Step.Speak, Step.Review -> true
        Step.Revise -> false
    }

    fun advance(to: Step) {
        player?.pause()
        val first = if (step == Step.Write && to == Step.Review) practice.draft else practice.first
        val next = practice.copy(step = to, position = 0, first = first)
        update(if (first == practice.first) next else next.copy(
            reviewChecks = emptySet(), writingFeedback = "", revisionFeedback = "", revisionFeedbackStale = false
        ))
    }
    fun goNext() {
        when (step) {
            Step.Listen -> advance(Step.Read)
            Step.Read -> advance(Step.Relisten)
            Step.Relisten -> {
                player?.pause()
                showWritingOptions = true
            }
            Step.Speak -> advance(Step.Write)
            Step.Write -> advance(Step.Review)
            Step.Review -> advance(Step.Revise)
            Step.Revise -> Unit
        }
    }

    Column(modifier) {
        PracticeProgress(step, previousStep != null, canGoNext,
            goBack = { previousStep?.let(::advance) }, goNext = ::goNext)
        PageEntrance(
            destination = step,
            modifier = Modifier.weight(1f),
            animate = !lesson.video || !step.usesLessonPlayer
        ) {
            PracticeStepContent(
                practice, files, player, deepSeek, update, message, canGoNext, ::goNext, complete
            )
        }
    }
    if (showWritingOptions) AlertDialog(
        onDismissRequest = { showWritingOptions = false },
        title = { Text("接下来做什么？") },
        confirmButton = { TextButton(onClick = { showWritingOptions = false; advance(Step.Write) }) { Text("直接写作") } },
        dismissButton = { TextButton(onClick = { showWritingOptions = false; advance(Step.Speak) }) { Text("加练口语") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PracticeStepContent(
    practice: Practice,
    files: File,
    player: ClipPlayer?,
    deepSeek: DeepSeek,
    update: (Practice) -> Unit,
    message: (String) -> Unit,
    canGoNext: Boolean,
    goNext: () -> Unit,
    complete: (Practice) -> Unit
) {
    val lesson = practice.lesson
    val step = practice.step
    val writingFeedback = practice.writingFeedback.takeIf(String::isNotBlank)
    val aiEnabled by deepSeek.configurationChanges.collectAsState(deepSeek.configured)
    var source by rememberSaveable { mutableStateOf(false) }
    var extraSpeaking by rememberSaveable { mutableStateOf(false) }
    var focus by rememberSaveable { mutableIntStateOf(-1) }
    val analyses = remember(lesson.id) { mutableStateMapOf<FocusedSentence, SentenceAnalysis>() }
    val latest by rememberUpdatedState(practice)
    val latestUpdate by rememberUpdatedState(update)
    ObservePlayer(player) { position -> latestUpdate(latest.copy(position = position)) }

    Page {
        when (step) {
            Step.Listen -> {
                Title(lesson.title)
                if (player != null) PlayerCard(player, lesson, allowAudioOnly = true)
                Primary("首听完成，进入精读", enabled = canGoNext, onClick = goNext)
            }
            Step.Relisten -> {
                if (practice.marks.isNotEmpty()) {
                    Text("重听标记片段", style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        practice.marks.forEach { mark ->
                            val from = (mark - 2_000).coerceAtLeast(0)
                            val until = mark + (lesson.duration - mark).coerceIn(0, 6_000)
                            AssistChip(onClick = { player?.play(from, until) }, label = { Text(timeText(mark)) },
                                enabled = player?.ready == true,
                                modifier = Modifier.heightIn(min = 48.dp))
                        }
                    }
                }
                if (player != null) PlayerCard(player, lesson, allowAudioOnly = true)
                Supporting("重听一两处困难片段，再合上字幕从头听一遍。可以跟读。")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { player?.pause(); source = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("按字幕重听")
                    }
                    TextButton(onClick = { player?.play(0) }, enabled = player?.ready == true,
                        modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("从头复听")
                    }
                }
                Primary("复听完成", enabled = canGoNext, onClick = goNext)
            }
            Step.Read -> {
                if (player != null) PlayerCard(player, lesson)
                ReadingSubtitles(lesson, player) { index ->
                    player?.pause()
                    focus = index
                }
                if (practice.listenNote.isNotBlank()) {
                    Panel {
                        Text("首听笔记", style = MaterialTheme.typography.titleMedium)
                        Text(practice.listenNote)
                    }
                }
                OutlinedTextField(value = practice.keywords, onValueChange = { update(practice.copy(keywords = it.take(240))) },
                    label = { Text("关键词（可选）") },
                    supportingText = { Text("建议记下 3–5 个词或短语，用逗号或换行分隔。") },
                    modifier = Modifier.fillMaxWidth())
                TargetExpression(practice) { target ->
                    if (target != latest.target) latestUpdate(latest.copy(
                        target = target, writingFeedback = "", revisionFeedback = "", revisionFeedbackStale = false
                    ))
                }
                Primary("精读完成，进入复听", onClick = goNext)
            }
            Step.Speak -> {
                Speaking(practice, files, message, { latestUpdate(latest.copy(speakingSupport = it)) },
                    checks = practice.speakingChecks, onChecks = { latestUpdate(latest.copy(speakingChecks = it)) },
                    advance = goNext)
            }
            Step.Write -> {
                Supporting("用自己的话概括主旨、关键理由和限制条件。")
                if (practice.target.isNotBlank()) Supporting("本轮尝试使用 ${practice.target}")
                Draft(practice.draft) { update(practice.copy(draft = it)) }
                var showCues by rememberSaveable { mutableStateOf(false) }
                TextButton(onClick = {
                    showCues = !showCues
                    if (showCues && !practice.writingCuesOpened) update(practice.copy(writingCuesOpened = true))
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (showCues) "收起写作提示" else "查看写作提示") }
                if (showCues) {
                    Panel {
                        Supporting("按主旨、理由或例子、适用条件的顺序组织摘要。")
                        if (practice.keywordList.isNotEmpty()) Supporting(practice.keywordList.joinToString(" · "))
                    }
                }
                Primary("检查我的摘要", enabled = canGoNext, onClick = goNext)
            }
            Step.Review -> {
                InitialDraft(practice.first)
                if (aiEnabled) Panel {
                    Text("AI 写作反馈", style = MaterialTheme.typography.titleMedium)
                    DeepSeekAction(
                        label = "获取 AI 写作反馈",
                        disclosure = "点击后发送素材标题、主题、本轮字幕、初稿、已选的目标表达及素材要点（如有）给 DeepSeek。",
                        result = writingFeedback,
                        onResult = { result ->
                            if (latest.first == practice.first && latest.target == practice.target) {
                                latestUpdate(latest.copy(writingFeedback = result))
                            }
                        }
                    ) { deepSeek.reviewWriting(practice) }
                }
                val words = wordCount(practice.first)
                Panel(color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("离线规则检查", style = MaterialTheme.typography.titleMedium)
                    Text("$words 词 · ${if (words in 80..100) "符合建议篇幅" else "建议 80–100 词"}")
                    if (practice.target.isNotBlank()) {
                        val literal = practice.first.contains(practice.target.trim(), ignoreCase = true)
                        Supporting(if (literal) "找到了目标表达的原样文字。请核对它在语境中是否合适。"
                            else "可核对目标表达是否已用合适的形式写出。")
                        Supporting("本轮目标：${practice.target}")
                    }
                }
                Panel {
                    Text("对照清单", style = MaterialTheme.typography.titleMedium)
                    SelfCheck(
                        listOf("主旨和关键理由准确", "保留原文的条件和限制", "信息顺序清楚", "用词和语法适合语境"),
                        practice.reviewChecks
                    ) { latestUpdate(latest.copy(reviewChecks = it)) }
                }
                OutlinedButton(onClick = { source = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("对照原文")
                }
                Primary("进入修订", onClick = goNext)
            }
            Step.Revise -> {
                if (aiEnabled && writingFeedback != null) {
                    Panel {
                        Text("初稿的 AI 写作反馈", style = MaterialTheme.typography.titleMedium)
                        DeepSeekResult(writingFeedback)
                    }
                }
                Draft(practice.draft) { draft ->
                    latestUpdate(latest.copy(draft = draft, revisionFeedback = "",
                        revisionFeedbackStale = latest.revisionFeedbackStale || latest.revisionFeedback.isNotBlank()))
                }
                var showInitialDraft by rememberSaveable { mutableStateOf(false) }
                TextButton(onClick = { showInitialDraft = !showInitialDraft }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (showInitialDraft) "收起初稿" else "对照初稿")
                }
                if (showInitialDraft) InitialDraft(practice.first)
                TextButton(onClick = { source = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("对照素材原文") }
                val draft = practice.draft
                if (aiEnabled) {
                    Panel {
                        Text("AI 修订评价", style = MaterialTheme.typography.titleMedium)
                        DeepSeekAction(
                            label = "评价当前修订稿",
                            disclosure = if (writingFeedback == null) {
                                "点击后发送素材标题、主题、本轮字幕、两稿、已选的目标表达及素材要点（如有）给 DeepSeek。"
                            } else {
                                "点击后发送素材标题、主题、本轮字幕、两稿、已选的目标表达、素材要点（如有）和初稿反馈给 DeepSeek。"
                            },
                            result = practice.revisionFeedback.takeIf(String::isNotBlank),
                            onResult = { result ->
                                if (latest.draft == draft && latest.first == practice.first && latest.target == practice.target) {
                                    latestUpdate(latest.copy(revisionFeedback = result, revisionFeedbackStale = false))
                                } else {
                                    latestUpdate(latest.copy(revisionFeedbackStale = true))
                                }
                            },
                            enabled = wordCount(draft) > 0
                        ) { deepSeek.reviewRevision(practice, writingFeedback) }
                        if (practice.revisionFeedbackStale) {
                            Supporting("当前稿已修改，可重新评价。")
                        }
                    }
                }
                OutlinedButton(onClick = { extraSpeaking = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text("修订后加练口语")
                }
                Primary(if (practice.first.trim() == practice.draft.trim()) "确认保留并完成" else "完成这一轮",
                    enabled = wordCount(draft) > 0) { complete(practice) }
            }
        }
    }

    if (extraSpeaking) ModalBottomSheet(onDismissRequest = { extraSpeaking = false }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 550.dp).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Title("修订后加练口语")
            Speaking(practice, files, message, { latestUpdate(latest.copy(speakingSupport = it)) }, task = SpeakingTask.Revision,
                checks = practice.revisionSpeakingChecks, onChecks = { latestUpdate(latest.copy(revisionSpeakingChecks = it)) }) {
                extraSpeaking = false
            }
        }
    }

    if (source) ModalBottomSheet(onDismissRequest = { source = false }) {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 550.dp), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Title("素材原文")
                if (lesson.notes.isNotBlank()) Supporting(lesson.notes)
                if ((step == Step.Review || step == Step.Revise) && lesson.keyPoints.isNotEmpty()) {
                    Panel {
                        Text("参考要点", style = MaterialTheme.typography.titleMedium)
                        lesson.keyPoints.forEach { Supporting(it) }
                    }
                }
            }
            items(lesson.cues) { cue ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(timeText(cue.start - lesson.start), color = MaterialTheme.colorScheme.primary)
                    Text(cue.text, style = MaterialTheme.typography.bodyLarge)
                    if (step == Step.Relisten) {
                        TextButton(onClick = {
                            source = false
                            player?.play(cue.start - lesson.start, cue.end - lesson.start)
                        }, modifier = Modifier.heightIn(min = 48.dp)) { Text("播放这段") }
                    }
                }
            }
        }
    }
    if (step == Step.Read && focus in lesson.cues.indices) {
        SentenceFocusSheet(lesson, focus, player, deepSeek, aiEnabled, analyses) {
            player?.pause()
            focus = -1
        }
    }
}

@Composable
private fun TargetExpression(practice: Practice, change: (String) -> Unit) {
    Panel {
        Text("本轮目标表达", style = MaterialTheme.typography.titleMedium)
        Supporting("选一个看得懂、自己还不常用的表达，稍后尝试用出来。")
        practice.lesson.expressionOptions.forEach { option ->
            val selected = option == practice.target
            FilterChip(selected = selected, onClick = { change(if (selected) "" else option) },
                label = { Text(option) }, modifier = Modifier.heightIn(min = 48.dp))
        }
        OutlinedTextField(
            value = practice.target, onValueChange = { change(it.take(100)) },
            label = { Text("目标表达（可选）") }, placeholder = { Text("输入一个英文词组或句型") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SelfCheck(items: List<String>, checks: Set<String>, onChecks: (Set<String>) -> Unit) {
    items.forEach { text ->
        key(text) {
            val checked = text in checks
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(checked, role = Role.Checkbox) { onChecks(if (it) checks + text else checks - text) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Checkbox(checked, onCheckedChange = null)
                Text(text)
            }
        }
    }
}

@Composable
private fun InitialDraft(text: String) {
    Panel {
        Text("初稿", style = MaterialTheme.typography.titleMedium)
        Text(text)
    }
}

@Composable
private fun ReadingSubtitles(lesson: Lesson, player: ClipPlayer?, openFocus: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showTranslation by rememberSaveable { mutableStateOf(false) }
    val hasTranslation = remember(lesson) { lesson.cues.any { it.translation.isNotBlank() } }
    val activeIndices by remember(lesson, player) {
        derivedStateOf {
            val time = lesson.start + (player?.position ?: 0)
            lesson.cues.indices.filter { time in lesson.cues[it].start until lesson.cues[it].end }
        }
    }
    val latestIndex by remember(lesson, player) {
        derivedStateOf {
            val time = lesson.start + (player?.position ?: 0)
            lesson.cues.indexOfLast { it.start <= time }
        }
    }
    val currentIndex = activeIndices.firstOrNull() ?: latestIndex.coerceAtLeast(0)
    val scheme = MaterialTheme.colorScheme
    Panel(color = scheme.surfaceContainerHigh) {
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val heading = when {
                activeIndices.isNotEmpty() || lesson.cues.isEmpty() -> "当前字幕"
                latestIndex < 0 -> "下一条字幕"
                else -> "最近一条字幕"
            }
            Text(heading, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (hasTranslation) {
                FilterChip(selected = showTranslation, onClick = { showTranslation = !showTranslation },
                    label = { Text("参考译文") }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        if (lesson.cues.isEmpty()) {
            Supporting("这份素材没有字幕。")
        } else {
            activeIndices.ifEmpty { listOf(currentIndex) }.forEachIndexed { position, index ->
                if (position > 0) HorizontalDivider()
                ReadingCue(lesson, index, player, showTranslation, openFocus)
            }
            OutlinedButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(if (expanded) "收起全部字幕" else "展开全部字幕")
            }
            if (expanded) {
                HorizontalDivider()
                SubtitleTimeline(lesson, player, currentIndex, showTranslation, openFocus)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubtitleTimeline(
    lesson: Lesson, player: ClipPlayer?, currentIndex: Int, showTranslation: Boolean, openFocus: (Int) -> Unit
) {
    val timeline = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val header = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(header) { header.bringIntoView() }
    LaunchedEffect(timeline, currentIndex) {
        if (!timeline.isScrollInProgress) {
            if (abs(currentIndex - timeline.firstVisibleItemIndex) > 5) {
                timeline.scrollToItem(currentIndex)
            } else {
                timeline.animateScrollToItem(currentIndex)
            }
        }
    }
    Row(Modifier.fillMaxWidth().bringIntoViewRequester(header), verticalAlignment = Alignment.CenterVertically) {
        Text("字幕时间轴", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { scope.launch { timeline.scrollToItem(currentIndex) } }) { Text("定位当前字幕") }
    }
    LazyColumn(
        state = timeline,
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(lesson.cues) { index, _ ->
            val scheme = MaterialTheme.colorScheme
            val color by animateColorAsState(
                targetValue = if (index == currentIndex) scheme.primaryContainer else scheme.surfaceContainerLow,
                animationSpec = tween(180), label = "时间轴当前字幕"
            )
            Surface(Modifier.fillMaxWidth(), color = color, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(12.dp)) {
                    ReadingCue(lesson, index, player, showTranslation, openFocus)
                }
            }
        }
    }
}

@Composable
private fun ReadingCue(lesson: Lesson, index: Int, player: ClipPlayer?, showTranslation: Boolean, openFocus: (Int) -> Unit) {
    val cue = lesson.cues[index]
    val from = cue.start - lesson.start
    val until = cue.end - lesson.start
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(timeText(from), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        CaptionText(cue, showTranslation)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { player?.play(from, until) }, enabled = player?.ready == true,
                modifier = Modifier.heightIn(min = 48.dp)) { Text("重听") }
            TextButton(onClick = { openFocus(index) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("句子聚焦") }
        }
    }
}

@Composable
private fun CaptionText(cue: Cue, showTranslation: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(cue.text, style = MaterialTheme.typography.bodyLarge)
        if (showTranslation && cue.translation.isNotBlank()) {
            Text(cue.translation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Draft(draft: String, change: (String) -> Unit) {
    OutlinedTextField(
        value = draft, onValueChange = { change(it.take(4000)) },
        label = { Text("英文摘要") }, minLines = 6, maxLines = 14,
        supportingText = { Text("${wordCount(draft)} 词 · 建议 80–100 词") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun ObservePlayer(player: ClipPlayer?, save: ((Int) -> Unit)? = null) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onSave by rememberUpdatedState(save)
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && player != null) {
                player.tick()
                player.pause()
                onSave?.invoke(player.position)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (player != null) {
                player.tick()
                player.close()
                onSave?.invoke(player.position)
            }
        }
    }
    if (player?.playing == true) LaunchedEffect(player) {
        while (player.playing) {
            delay(120)
            player.tick()
        }
    }
}

@Composable
fun VideoSurface(player: ClipPlayer) {
    AndroidView(
        factory = { context ->
            SurfaceView(context).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) { player.display(holder) }
                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit
                    override fun surfaceDestroyed(holder: SurfaceHolder) { player.display(null) }
                })
            }
        },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
    )
}

@Composable
fun PlayerCard(
    player: ClipPlayer,
    lesson: Lesson,
    captions: List<Cue>? = null,
    allowAudioOnly: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    var audioOnly by rememberSaveable(lesson.id) { mutableStateOf(false) }
    Panel(color = scheme.primaryContainer) {
        if (lesson.video && !audioOnly) {
            VideoSurface(player)
        } else {
            val progress = player.position.toFloat() / lesson.duration
            Canvas(Modifier.fillMaxWidth().height(62.dp)) {
                val bars = listOf(13, 21, 42, 25, 52, 33, 18, 40, 56, 31, 45, 19, 33, 51, 25, 39, 18, 48, 59, 29, 38, 22, 51, 34, 15, 32)
                val spacing = size.width / bars.size
                bars.forEachIndexed { i, value ->
                    val height = size.height * value / 64f
                    val color = if (i.toFloat() / bars.size <= progress) scheme.onPrimaryContainer else scheme.onPrimaryContainer.copy(alpha = .28f)
                    drawRoundRect(color, Offset(spacing * i, (size.height - height) / 2),
                        Size(spacing * .48f, height), CornerRadius(10.dp.toPx()))
                }
            }
        }
        if (captions != null) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (captions.isEmpty()) {
                    Text("当前位置没有字幕", style = MaterialTheme.typography.bodyMedium)
                } else {
                    captions.forEach { CaptionText(it, showTranslation = false) }
                }
            }
        }
        if (player.error.isNotEmpty()) {
            Text(player.error, color = scheme.onPrimaryContainer)
        } else {
            if (lesson.video && allowAudioOnly) {
                TextButton(onClick = { audioOnly = !audioOnly }, modifier = Modifier.align(Alignment.End)) {
                    Text(if (audioOnly) "显示视频" else "仅听音频")
                }
            }
            Column(Modifier.fillMaxWidth()) {
                Slider(value = player.position.toFloat(), onValueChange = { player.seek(it.toInt()) },
                    valueRange = 0f..lesson.duration.toFloat(), enabled = player.ready,
                    modifier = Modifier.semantics { contentDescription = "播放进度" })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(timeText(player.position), style = MaterialTheme.typography.labelMedium)
                    Text(timeText(lesson.duration), style = MaterialTheme.typography.labelMedium)
                }
            }
            val compactPadding = PaddingValues(horizontal = 4.dp)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PlaybackSpeedButton(player, 0.75f, Modifier.weight(1.1f))
                TextButton(
                    onClick = { player.seek(player.position - 10_000) },
                    enabled = player.ready,
                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "后退 10 秒" },
                    contentPadding = compactPadding
                ) { Text("-10s", maxLines = 1) }
                Button(
                    onClick = { if (player.playing) player.pause() else player.play() },
                    enabled = player.ready,
                    modifier = Modifier.weight(1.2f).heightIn(min = 48.dp),
                    contentPadding = compactPadding
                ) {
                    Crossfade(player.playing, animationSpec = tween(160), label = "播放状态") { playing ->
                        Text(if (playing) "暂停" else "播放", maxLines = 1)
                    }
                }
                TextButton(
                    onClick = { player.seek(player.position + 10_000) },
                    enabled = player.ready,
                    modifier = Modifier.weight(0.9f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "前进 10 秒" },
                    contentPadding = compactPadding
                ) { Text("+10", maxLines = 1) }
                PlaybackSpeedButton(player, 1.25f, Modifier.weight(1.1f))
            }
        }
    }
}

@Composable
private fun PlaybackSpeedButton(player: ClipPlayer, speed: Float, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val isSelected = speed == player.playbackRate
    OutlinedButton(
        onClick = { player.changeSpeed(if (isSelected) 1f else speed) },
        modifier = modifier.heightIn(min = 48.dp).semantics {
            selected = isSelected
            contentDescription = if (isSelected) "$speed 倍速，再按恢复原速" else "$speed 倍速"
        },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) scheme.secondaryContainer else Color.Transparent,
            contentColor = if (isSelected) scheme.onSecondaryContainer else scheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) { Text("$speed", maxLines = 1) }
}

internal enum class SpeakingTask { Practice, Revision, Assessment }

internal fun speechFile(files: File, id: String, revised: Boolean = false): File =
    File(files, "speech-$id${if (revised) "-revision" else ""}.m4a")

internal fun discardSpeech(files: File, id: String, revised: Boolean = false) {
    val file = speechFile(files, id, revised)
    file.delete()
    File(files, "${file.name}.tmp").delete()
}

@Composable
internal fun Speaking(
    practice: Practice, files: File, message: (String) -> Unit,
    changeSupport: (SpeakingSupport) -> Unit,
    task: SpeakingTask = SpeakingTask.Practice,
    checks: Set<String>, onChecks: (Set<String>) -> Unit,
    advance: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(practice.id, task) { speechFile(files, practice.id, task == SpeakingTask.Revision) }
    val recorder = remember(file) { LocalRecorder(context, file) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var seconds by remember { mutableIntStateOf(0) }
    var recording by remember { mutableStateOf(false) }
    var version by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(if (file.exists()) recordingLength(file) else 0) }
    var playerVisible by remember { mutableStateOf(file.exists()) }
    val player = if (playerVisible && duration > 0) remember(file, version) {
        ClipPlayer(context, practice.lesson.copy(uri = file.toURI().toString(), video = false, start = 0, end = duration), 0)
    } else null
    ObservePlayer(player)
    var compareRecording by rememberSaveable { mutableStateOf(false) }
    val earlierRecording = remember(practice.id) { speechFile(files, practice.id) }
    val earlierLength = if (task == SpeakingTask.Revision && earlierRecording.isFile) {
        remember(earlierRecording) { recordingLength(earlierRecording) }
    } else 0
    val earlierLesson = remember(earlierRecording, earlierLength) {
        practice.lesson.copy(uri = earlierRecording.toURI().toString(), video = false, start = 0, end = earlierLength)
    }
    val earlierPlayer = if (compareRecording && !recording && earlierLength > 0) {
        remember(earlierLesson) { ClipPlayer(context, earlierLesson, 0) }
    } else null
    ObservePlayer(earlierPlayer)
    fun stop() {
        if (!recording) return
        recording = false
        if (recorder.stop()) {
            duration = recordingLength(file)
            version++
            playerVisible = true
        } else {
            playerVisible = file.exists()
            message("未能保存这段录音，可以重试。")
        }
    }
    fun start() {
        player?.pause()
        earlierPlayer?.pause()
        runCatching { recorder.start() }.onSuccess {
            seconds = 0
            playerVisible = false
            recording = true
            onChecks(emptySet())
        }.onFailure { message("麦克风暂时不可用，可以重试或跳过。") }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) start() else message("麦克风权限未开启，可以跳过口语。")
    }
    LaunchedEffect(recording) {
        if (recording) {
            repeat(60) {
                delay(1000)
                seconds++
            }
            stop()
        }
    }
    DisposableEffect(lifecycle, recorder) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_PAUSE) stop() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            recorder.stop()
        }
    }
    Supporting("用 45–60 秒复述主旨，再说一个看法和理由。")
    if (task != SpeakingTask.Assessment && practice.target.isNotBlank()) Supporting("本轮尝试使用 ${practice.target}")
    if (task != SpeakingTask.Assessment) Panel {
        Text("提示程度", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeakingSupport.entries.forEach { support ->
                FilterChip(selected = practice.speakingSupport == support, onClick = { changeSupport(support) },
                    label = { Text(support.label) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        val keywords = practice.keywordList
        when (practice.speakingSupport) {
            SpeakingSupport.Structure -> {
                Supporting("主旨 → 理由或例子 → 适用条件 → 自己的看法和理由")
                Supporting("可用 because、for example 或 however 衔接。")
                if (keywords.isNotEmpty()) Supporting(keywords.joinToString(" · "))
            }
            SpeakingSupport.Keywords -> Supporting(keywords.take(3).joinToString(" · ").ifEmpty { "还没有记录关键词。" })
            SpeakingSupport.None -> Unit
        }
    }
    AnimatedVisibility(
        visible = recording,
        enter = contentFadeIn() + expandVertically(),
        exit = contentFadeOut() + shrinkVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("正在录音", Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            Text("${timeText(seconds * 1000)} / 1:00")
            val progress by animateFloatAsState(seconds / 60f, tween(200), label = "录音进度")
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
    Primary(if (recording) "停止录音" else if (file.exists()) "重新录音" else "开始录音") {
        when {
            recording -> stop()
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> start()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    if (player != null) {
        PlayerCard(player, practice.lesson.copy(video = false, start = 0, end = duration))
        Panel {
            Text("回听自查", style = MaterialTheme.typography.titleMedium)
            SelfCheck(buildList {
                add("说清了素材的主旨和理由")
                add("保留了关键条件或限制")
                add("说出了自己的看法和理由")
                if (practice.target.isNotBlank()) add("目标表达用得合适")
            }, checks, onChecks)
        }
    }
    if (earlierLength > 0 && !recording) {
        TextButton(onClick = { compareRecording = !compareRecording }) {
            Text(if (compareRecording) "收起写作前录音" else "对照写作前录音")
        }
        if (earlierPlayer != null) PlayerCard(earlierPlayer, earlierLesson)
    }
    OutlinedButton(onClick = { stop(); advance() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(when {
            task == SpeakingTask.Assessment -> "提交本次测评"
            task == SpeakingTask.Revision -> "返回修订"
            file.exists() -> "继续独立写作"
            else -> "跳过口语，继续写作"
        })
    }
}

private fun recordingLength(file: File): Int {
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0
    } catch (_: Exception) {
        0
    } finally {
        retriever.release()
    }
}

@Composable
fun Finish(record: Completion, count: Int, openHistory: () -> Unit) {
    var compare by rememberSaveable { mutableStateOf(false) }
    Panel(color = MaterialTheme.colorScheme.primaryContainer) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null) }
        }
        Text("完成一轮", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Medium)
        val completed = record.completedAt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) ?: record.date
        Text("${record.title} · $completed")
        Text("累计完成 $count 轮")
        if (record.target.isNotBlank()) Supporting("本轮目标表达 ${record.target}")
        if (record.writingCuesOpened) Supporting("本轮查看过写作提示。")
        if (record.retold || record.retoldAfterRevision) {
            val attempts = listOfNotNull("写作前".takeIf { record.retold }, "修订后".takeIf { record.retoldAfterRevision })
            Supporting("口语复述 · ${attempts.joinToString("、")}")
        }
    }
    Primary("查看完成记录", onClick = openHistory)
    OutlinedButton(onClick = { compare = !compare }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(if (compare) "收起前后对照" else "查看前后对照")
    }
    AnimatedVisibility(
        visible = compare,
        enter = contentFadeIn() + expandVertically(),
        exit = contentFadeOut() + shrinkVertically()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Panel { Text("初稿", style = MaterialTheme.typography.titleMedium); Text(record.first) }
            Panel { Text("完成稿", style = MaterialTheme.typography.titleMedium); Text(record.draft) }
        }
    }
}
