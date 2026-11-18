package com.increment.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SentenceFocusSheet(
    lesson: Lesson,
    cueIndex: Int,
    player: ClipPlayer?,
    deepSeek: DeepSeek,
    aiEnabled: Boolean,
    analyses: MutableMap<FocusedSentence, SentenceAnalysis>,
    close: () -> Unit
) {
    val cue = lesson.cues[cueIndex]
    val sentences = remember(lesson, cueIndex) {
        sentencesTouchingCue(lesson.cues, cueIndex).ifEmpty {
            listOf(FocusedSentence(normalizeCaption(cue.text), cueIndex, cueIndex))
        }
    }
    var selected by rememberSaveable(cueIndex) { mutableIntStateOf(0) }
    val sentence = sentences.getOrElse(selected) { sentences.first() }
    val from = lesson.cues[sentence.firstCue].start - lesson.start
    val until = remember(lesson, sentence) {
        lesson.cues.subList(sentence.firstCue, sentence.lastCue + 1).maxOf(Cue::end) - lesson.start
    }
    val analysis = if (aiEnabled) analyses[sentence] else null
    val reference = cue.translation.takeIf { it.isNotBlank() && sentence.text == normalizeCaption(cue.text) }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val height = (LocalConfiguration.current.screenHeightDp * .74f).dp

    ModalBottomSheet(onDismissRequest = close, sheetState = sheet) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("句子聚焦", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = close) { Icon(Icons.Default.Close, contentDescription = "关闭句子聚焦") }
        }
        HorizontalDivider()
        key(sentence) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = height).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        enabled = player?.ready == true,
                        onClick = {
                            if (player?.playing == true) player.pause() else player?.play(from, until)
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text(if (player?.playing == true) "暂停原声" else "重听原声") }
                    Supporting("${timeText(from)}–${timeText(until)}")
                }
                if (sentences.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(enabled = selected > 0, onClick = { player?.pause(); selected-- }) { Text("上一句") }
                    TextButton(enabled = selected < sentences.lastIndex, onClick = { player?.pause(); selected++ }) { Text("下一句") }
                }
                FocusedEnglish(sentence.text, analysis)
                if (analysis != null) {
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("整句译文", style = MaterialTheme.typography.labelLarge)
                            Text(analysis.translation, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Text("分段讲解", style = MaterialTheme.typography.titleMedium)
                    analysis.segments.forEachIndexed { index, segment ->
                        SentenceSegmentCard(sentence.text, segment, index)
                    }
                } else if (reference != null) {
                    Text(reference, style = MaterialTheme.typography.bodyLarge)
                }
                if (aiEnabled) {
                    DeepSeekRequest(
                        label = "AI 解析本句",
                        disclosure = "点击后将选中句子、相邻字幕、素材标题和主题发送给 DeepSeek。",
                        hasResult = analysis != null,
                        onResult = { result -> analyses[sentence] = result },
                        retryLabel = "重新解析"
                    ) { deepSeek.explainSentence(lesson, sentence) }
                } else {
                    Supporting("找到主语和谓语，再核对原因、条件、转折或否定怎样影响句意。")
                }
            }
        }
    }
}

@Composable
private fun FocusedEnglish(sentence: String, analysis: SentenceAnalysis?) {
    val scheme = MaterialTheme.colorScheme
    val accents = listOf(
        scheme.primaryContainer to scheme.onPrimaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer
    )
    val highlighted = buildAnnotatedString {
        append(sentence)
        analysis?.segments?.forEachIndexed { index, segment ->
            val (background, ink) = accents[index % accents.size]
            addStyle(SpanStyle(color = ink, background = background, fontWeight = FontWeight.SemiBold),
                segment.range.first, segment.range.last + 1)
        }
    }
    SelectionContainer { Text(highlighted, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun SentenceSegmentCard(sentence: String, segment: SentenceSegment, index: Int) {
    val scheme = MaterialTheme.colorScheme
    val accent: Color = if (index % 2 == 0) scheme.primary else scheme.tertiary
    Panel(color = scheme.surfaceContainerHigh) {
        Text(segment.role, color = accent, style = MaterialTheme.typography.labelLarge)
        SelectionContainer {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sentence.substring(segment.range), style = MaterialTheme.typography.titleMedium)
                Text(segment.translation, style = MaterialTheme.typography.bodyLarge)
                Supporting(segment.explanation)
            }
        }
    }
}
