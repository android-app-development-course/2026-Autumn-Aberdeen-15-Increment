package com.increment.app

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        val splashExited = mutableStateOf(Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener {
                it.remove()
                splashExited.value = true
            }
        }
        val store = PracticeStore(applicationContext)
        val catalog = LessonCatalog(this)
        setContent {
            var showBrand by remember { mutableStateOf(savedInstanceState == null) }
            var theme by remember { mutableStateOf(store.theme()) }
            val dark = when (theme) {
                "浅色" -> false
                "深色" -> true
                else -> isSystemInDarkTheme()
            }
            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(0, 0) { dark },
                    navigationBarStyle = SystemBarStyle.auto(0, 0) { dark }
                )
            }
            IncrementTheme(dark) {
                Box(Modifier.fillMaxSize()) {
                    IncrementApp(store, catalog, filesDir, theme) { theme = it; store.theme(it) }
                    if (showBrand) {
                        LaunchBrand(splashExited = { splashExited.value }) { showBrand = false }
                    }
                }
            }
        }
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    Today("今日", Icons.Default.Home), Library("素材", Icons.AutoMirrored.Filled.List),
    History("记录", Icons.Default.DateRange), Profile("我的", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncrementApp(store: PracticeStore, catalog: LessonCatalog, files: File, theme: String, setTheme: (String) -> Unit) {
    val builtIns = catalog.originals
    val context = LocalContext.current
    val localLibrary = remember(context) { LocalLibrary(context.applicationContext) }
    val assessmentStore = remember(context) { AssessmentStore(context.applicationContext) }
    val initialLibrary = remember(localLibrary) { runCatching(localLibrary::read) }
    var imported by remember { mutableStateOf(initialLibrary.getOrDefault(emptyList())) }
    var importDraft by remember { mutableStateOf(localLibrary.draft()) }
    var tab by rememberSaveable { mutableStateOf(Tab.Today) }
    var screen by rememberSaveable { mutableStateOf("tabs") }
    var detailId by rememberSaveable { mutableStateOf(builtIns.first().id) }
    var practice by remember { mutableStateOf(store.load(builtIns)) }
    var records by remember { mutableStateOf(store.history()) }
    var recordId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingLesson by remember { mutableStateOf<Lesson?>(null) }
    var pendingRemoval by remember { mutableStateOf<Lesson?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deepSeek = remember(context) { DeepSeek(context.applicationContext) }
    fun message(text: String) { scope.launch { snackbar.showSnackbar(text) } }
    fun update(next: Practice) { practice = next; store.save(next) }
    fun saveDraft(next: ImportDraft) { localLibrary.saveDraft(next); importDraft = next }
    fun writeLibrary(next: List<Lesson>): Boolean {
        if (initialLibrary.isFailure || runCatching { localLibrary.write(next) }.isFailure) {
            message("无法更新我的素材，请检查本机存储后重试。")
            return false
        }
        imported = next
        return true
    }
    fun discardAudio(previous: Practice?) {
        previous?.let {
            discardSpeech(files, it.id)
            discardSpeech(files, it.id, revised = true)
        }
    }
    fun start(lesson: Lesson) {
        discardAudio(practice)
        update(Practice(lesson))
        screen = "practice"
    }
    fun request(lesson: Lesson) {
        if (practice == null) start(lesson) else pendingLesson = lesson
    }
    fun showDetail(lesson: Lesson) { detailId = lesson.id; screen = "detail" }
    fun saveImport(lesson: Lesson, startNow: Boolean) {
        val existing = imported.firstOrNull { it == lesson.copy(id = it.id) }
        if (existing == null && !writeLibrary(listOf(lesson) + imported)) return
        val saved = existing ?: lesson
        saveDraft(ImportDraft())
        tab = Tab.Library
        showDetail(saved)
        if (startNow) request(saved) else message("已存入我的素材。")
    }
    fun goBack() { screen = "tabs" }
    val selectedRecord = records.find { it.id == recordId }
    val detail = (builtIns + imported).find { it.id == detailId }
    val resumablePractice = practice?.takeIf { current ->
        current.lesson.uri.isNotEmpty() || builtIns.any { it.id == current.lesson.mediaId }
    }
    val activeScreen = when {
        screen == "practice" && resumablePractice == null -> "tabs"
        screen == "finish" && selectedRecord == null -> "tabs"
        screen == "detail" && detail == null -> "tabs"
        else -> screen
    }
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val showTabs = activeScreen == "tabs"
    val resolver = context.contentResolver
    val activeMedia = practice?.lesson?.uri
    LaunchedEffect(activeScreen, activeMedia, imported, importDraft) {
        if (activeScreen != "import" && initialLibrary.isSuccess) {
            val needed = imported.mapTo(mutableSetOf()) { it.uri }
            needed.addAll(listOfNotNull(activeMedia, importDraft.mediaUri, importDraft.textUri))
            resolver.persistedUriPermissions.filter { it.isReadPermission && it.uri.toString() !in needed }
                .forEach { permission ->
                    runCatching { resolver.releasePersistableUriPermission(permission.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                }
        }
    }
    BackHandler(enabled = !showTabs || tab != Tab.Today) {
        if (showTabs) tab = Tab.Today else goBack()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    if (showTabs && tab == Tab.Today) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IncrementLogo(Modifier.size(34.dp))
                            Text("Increment", fontWeight = FontWeight.SemiBold)
                        }
                    } else Text(when (activeScreen) {
                        "detail" -> "素材详情"
                        "import" -> "导入素材"
                        "practice" -> practice?.step?.label.orEmpty()
                        "finish" -> "练习完成"
                        "assessment" -> "阶段测评"
                        else -> tab.title
                    })
                },
                navigationIcon = {
                    if (!showTabs) IconButton(onClick = ::goBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, if (activeScreen == "practice") "暂停练习，返回" else "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (showTabs && !wide) NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item },
                        icon = { Icon(item.icon, null) }, label = { Text(item.title) })
                }
            }
        }
    ) { insets ->
        Row(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()) {
            if (showTabs && wide) {
                NavigationRail {
                    Tab.entries.forEach { item ->
                        NavigationRailItem(selected = tab == item, onClick = { tab = item },
                            icon = { Icon(item.icon, null) }, label = { Text(item.title) })
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                val body = Modifier.widthIn(max = 680.dp).fillMaxSize()
                when (activeScreen) {
                    "practice" -> resumablePractice?.let { current ->
                        key(current.id) {
                            PracticeScreen(current, files, deepSeek, { next ->
                                val active = practice
                                if (active?.id == current.id && active.step == current.step) update(next)
                            }, ::message, modifier = body) { completed ->
                                val active = practice
                                if (active?.id == completed.id && active.step == Step.Revise) {
                                    if (store.complete(completed, speechFile(files, completed.id).isFile,
                                            speechFile(files, completed.id, revised = true).isFile)) {
                                        discardAudio(completed)
                                        records = store.history()
                                        recordId = completed.id
                                        practice = null
                                        screen = "finish"
                                    } else message("保存记录失败，请重试。")
                                }
                            }
                        }
                    }
                    "import" -> PageEntrance(activeScreen, body) {
                        ImportScreen(Modifier.fillMaxSize(), importDraft, ::saveDraft, ::saveImport)
                    }
                    "assessment" -> PageEntrance(activeScreen, body) {
                        AssessmentScreen(Modifier.fillMaxSize(), catalog.assessments, files, assessmentStore, ::message)
                    }
                    "tabs" -> AnimatedContent(
                        targetState = tab,
                        modifier = body.clipToBounds(),
                        transitionSpec = {
                            val direction = if (targetState.ordinal > initialState.ordinal) {
                                SlideDirection.Start
                            } else {
                                SlideDirection.End
                            }
                            val slideAnimation = tween<IntOffset>(260, easing = FastOutSlowInEasing)
                            slideIntoContainer(direction, slideAnimation) togetherWith
                                slideOutOfContainer(direction, slideAnimation)
                        },
                        label = "主栏目切换"
                    ) { pageTab ->
                        Page {
                            when (pageTab) {
                                Tab.Today -> Today(resumablePractice, records, { screen = "practice" },
                                    { screen = "import" }, { showDetail(it) }, { tab = Tab.Library },
                                    builtIns.firstOrNull(Lesson::isPlayable))
                                Tab.Library -> Library(imported, builtIns,
                                    initialLibrary.isFailure, ::showDetail) { screen = "import" }
                                Tab.History -> History(records, { tab = Tab.Today }) { record ->
                                    recordId = record.id
                                    screen = "finish"
                                }
                                Tab.Profile -> Profile(theme, setTheme, deepSeek) { screen = "assessment" }
                            }
                        }
                    }
                    else -> PageEntrance(activeScreen, body) {
                        Page {
                            when (activeScreen) {
                                "detail" -> detail?.let { lesson ->
                                    Detail(lesson, { screen = "import" }, { request(lesson) },
                                        remove = if (lesson.uri.isNotBlank()) {{ pendingRemoval = lesson }} else null)
                                }
                                "finish" -> selectedRecord?.let { record ->
                                    Finish(record, records.size) {
                                        tab = Tab.History
                                        goBack()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    pendingLesson?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingLesson = null },
            title = { Text("替换当前练习？") },
            text = { Text("开始「${lesson.title}」会清除当前未完成的草稿、标记和口语录音。已完成记录将保留。") },
            confirmButton = { TextButton(onClick = { pendingLesson = null; start(lesson) }) { Text("开始新一轮") } },
            dismissButton = { TextButton(onClick = { pendingLesson = null }) { Text("取消") } }
        )
    }
    pendingRemoval?.let { lesson ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("移除这个素材？") },
            text = { Text("「${lesson.title}」将从我的素材移除。源文件和练习记录会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    if (writeLibrary(imported.filterNot { it.id == lesson.id })) {
                        pendingRemoval = null
                        goBack()
                    }
                }) { Text("移除素材") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("取消") } }
        )
    }
}

@Composable
fun Page(modifier: Modifier = Modifier.fillMaxSize(), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

@Composable
fun Title(text: String) = Text(text, style = MaterialTheme.typography.headlineMedium)

@Composable
fun Supporting(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
fun Primary(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
        Text(text)
    }
}

@Composable
private fun Today(
    current: Practice?, records: List<Completion>, resume: () -> Unit, start: () -> Unit,
    openLesson: (Lesson) -> Unit, library: () -> Unit, sample: Lesson?
) {
    val today = LocalDate.now()
    Text("${today.monthValue}月${today.dayOfMonth}日 · 今日练习",
        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text("把听懂的英语\n用起来", style = MaterialTheme.typography.displaySmall)
    Supporting("听懂一段材料，主动用出一个表达。")
    if (current != null) {
        Panel(color = MaterialTheme.colorScheme.secondaryContainer) {
            Text("继续练习", style = MaterialTheme.typography.titleMedium)
            Text("${current.lesson.title} · ${current.step.label}", style = MaterialTheme.typography.bodyLarge)
            LinearProgressIndicator(progress = { current.step.coreProgress }, modifier = Modifier.fillMaxWidth())
            Primary("继续${current.step.label}", onClick = resume)
        }
    } else {
        val palette = MaterialTheme.colorScheme
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
            Panel(color = palette.primaryContainer) {
                Text("开启一轮练习", color = palette.onPrimaryContainer, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(sample?.title ?: "用自己的素材", color = palette.onPrimaryContainer,
                    style = MaterialTheme.typography.headlineSmall)
                Text(if (sample != null) "内置${if (sample.video) "视频" else "音频"} · ${timeText(sample.duration)}"
                    else "音视频 + 带时间轴字幕",
                    color = palette.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { if (sample != null) openLesson(sample) else start() },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                        Text(if (sample != null) "开始今日练习" else "导入素材")
                }
            }
            if (LocalConfiguration.current.screenWidthDp >= 370 && LocalDensity.current.fontScale <= 1.2f) {
                Canvas(Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(115.dp, 114.dp)) {
                    val ring = palette.onPrimaryContainer.copy(alpha = .13f)
                    drawCircle(ring, size.height * .47f, style = Stroke(width = 1.dp.toPx()))
                    drawCircle(ring, size.height * .36f, style = Stroke(width = 1.dp.toPx()))
                    val bars = listOf(.18f, .37f, .55f, .31f, .49f, .68f, .34f, .19f)
                    bars.forEachIndexed { i, bar ->
                        val height = size.height * bar
                        drawRoundRect(palette.primary.copy(alpha = .77f), Offset(size.width * (.16f + i * .094f), (size.height - height) / 2),
                            Size(5.dp.toPx(), height), CornerRadius(8.dp.toPx()))
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiaryContainer) {
            Text("${records.size}", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer)
            Text("累计完成轮次", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Panel(Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceContainer) {
            Text("${records.count { it.date == today.toString() }}", style = MaterialTheme.typography.headlineMedium)
            Text("今日完成轮次", style = MaterialTheme.typography.labelMedium)
        }
    }
    if (sample != null || current != null) OutlinedButton(start, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("导入素材") }
    OutlinedButton(library, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("浏览素材库") }
}

@Composable
private fun Library(
    imported: List<Lesson>, originals: List<Lesson>, libraryError: Boolean,
    show: (Lesson) -> Unit, import: () -> Unit
) {
    Title("选择练习素材")
    Supporting("选一段有挑战但能听懂主旨的素材。")
    Button(import, Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
        Icon(Icons.Default.Add, null)
        Spacer(Modifier.width(8.dp))
        Text("导入自己的素材")
    }
    Text("我的素材", style = MaterialTheme.typography.titleMedium)
    if (libraryError) Supporting("暂时无法读取本机素材库。")
    else if (imported.isEmpty()) Supporting("配对并核对音视频与字幕后，可保存到这里反复练习。")
    else LessonCards(imported, show)
    Text("TED 原片", style = MaterialTheme.typography.titleMedium)
    LessonCards(originals, show)
}

@Composable
private fun LessonCards(lessons: List<Lesson>, show: (Lesson) -> Unit) {
    lessons.forEachIndexed { index, lesson ->
        val scheme = MaterialTheme.colorScheme
        val colors = listOf(scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer, scheme.secondaryContainer to scheme.onSecondaryContainer)
        val (tile, ink) = colors[index % colors.size]
        Card(onClick = { show(lesson) }, shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow)) {
            ListItem(
                headlineContent = { Text(lesson.title, style = MaterialTheme.typography.titleMedium) },
                supportingContent = {
                    Text(listOf(lesson.english, "${lesson.topic} · ${timeText(lesson.duration)}")
                        .filter(String::isNotBlank).joinToString("\n"))
                },
                leadingContent = {
                    Surface(color = tile, contentColor = ink, shape = RoundedCornerShape(14.dp)) {
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PlayArrow, null)
                        }
                    }
                },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun Detail(lesson: Lesson, import: () -> Unit, start: () -> Unit, remove: (() -> Unit)?) {
    val uriHandler = LocalUriHandler.current
    Panel(color = MaterialTheme.colorScheme.primaryContainer) {
        Text(lesson.topic, style = MaterialTheme.typography.labelLarge)
        Title(lesson.title)
        if (lesson.english.isNotBlank()) Text(lesson.english, style = MaterialTheme.typography.titleMedium)
        if (lesson.notes.isNotBlank()) Supporting(lesson.notes)
        if (lesson.difficulty.isNotBlank()) Supporting(lesson.difficulty)
        val mediaType = if (lesson.video) "视频" else "音频"
        Text(if (lesson.isPlayable) "$mediaType ${timeText(lesson.duration)}" else "音频和英文正文待补充",
            style = MaterialTheme.typography.bodyMedium)
        if (lesson.start > 0 || lesson.mediaId != lesson.id) {
            Supporting("原片时间 ${timeText(lesson.start)}–${timeText(lesson.end)}")
        }
        if (lesson.sourceUrl.isNotBlank()) {
            TextButton(onClick = { uriHandler.openUri(lesson.sourceUrl) }) { Text("查看 TED 原片") }
        }
    }
    Text("练习路线", style = MaterialTheme.typography.titleLarge)
    Panel {
        Text("首听 → 精读 → 复听 → 独立写作 → 写作自查 → 修订")
        Supporting("复听后可加练 45–60 秒口语。")
    }
    if (lesson.isPlayable) Primary("开始这一轮", onClick = start)
    else Primary("导入自己的素材", onClick = import)
    if (remove != null) TextButton(onClick = remove) { Text("从我的素材移除") }
}

@Composable
private fun History(records: List<Completion>, start: () -> Unit, open: (Completion) -> Unit) {
    Title("完成记录")
    Panel(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Text("${records.size} 轮", style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer)
        Text("累计完成", color = MaterialTheme.colorScheme.onTertiaryContainer)
    }
    val dates = records.groupBy { it.date }
    Text("最近 7 天", style = MaterialTheme.typography.titleMedium)
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (6 downTo 0).forEach {
            val date = today.minusDays(it.toLong())
            val count = dates[date.toString()]?.size ?: 0
            val scheme = MaterialTheme.colorScheme
            Surface(Modifier.weight(1f), color = if (count > 0) scheme.primaryContainer else scheme.surfaceContainerLow,
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${date.dayOfMonth}日", style = MaterialTheme.typography.labelSmall)
                    Text("$count", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if (records.isEmpty()) {
        Supporting("完成首轮练习后，日期和素材会自动出现在这里。")
        Primary("去练习", onClick = start)
    } else dates.forEach { (date, items) ->
        Text(date, style = MaterialTheme.typography.labelLarge)
        items.forEach { item ->
            if (item.hasDrafts) {
                Card(onClick = { open(item) }, modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text("查看写作前后对照") },
                        leadingContent = { Icon(Icons.Default.Check, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) })
                }
            } else {
                ListItem(headlineContent = { Text(item.title) }, supportingContent = { Text("已完成核心练习") },
                    leadingContent = { Icon(Icons.Default.Check, null) })
            }
        }
    }
}

@Composable
private fun Profile(theme: String, setTheme: (String) -> Unit, deepSeek: DeepSeek, openAssessment: () -> Unit) {
    DeepSeekSettings(deepSeek)
    Panel {
        Text("外观", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("系统", "浅色", "深色").forEach {
                FilterChip(selected = theme == it, onClick = { setTheme(it) }, label = { Text(it) },
                    modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
    Panel {
        Text("阶段测评", style = MaterialTheme.typography.titleMedium)
        Supporting("用两段独立素材记录听力理解和英文摘要，回看前后两次表现。")
        OutlinedButton(onClick = openAssessment) { Text("打开阶段测评") }
    }
    Panel {
        Text("本机数据", style = MaterialTheme.typography.titleMedium)
        Supporting("未完成练习、素材配对、两稿和测评答卷保存在此设备。录音仅供本轮回放，完成或替换练习后清除。")
    }
}
