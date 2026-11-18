# Increment 1.0.3 · 第三阶段 Android 应用

Increment 面向需要参加英文授课讨论与汇报的大学生。它继承第一阶段的单一目标表达和完整练习循环，以及第二阶段的「今日 / 素材 / 记录 / 我的」导航与口语分支。

## 页面实现

第三阶段 Android 界面使用 **Kotlin、Jetpack Compose 和 Material 3**。各页面由 `@Composable` 函数组成，应用从 `MainActivity` 的 `setContent` 进入 Compose 界面。

- [MainActivity.kt](app/src/main/java/com/increment/app/MainActivity.kt) 实现「今日 / 素材 / 记录 / 我的」、素材详情、练习完成页及页面导航。
- [ImportScreen.kt](app/src/main/java/com/increment/app/ImportScreen.kt) 实现素材导入和字幕核对。[PracticeScreen.kt](app/src/main/java/com/increment/app/PracticeScreen.kt) 实现听读、口语、写作及自查修订。练习中的视频画面通过 `AndroidView` 嵌入 Android `SurfaceView`。
- [LessonCatalog.kt](app/src/main/java/com/increment/app/LessonCatalog.kt) 加载三部 TED 原片和独立测评素材。[LocalLibrary.kt](app/src/main/java/com/increment/app/LocalLibrary.kt) 保存导入表单及配对素材。[AssessmentScreen.kt](app/src/main/java/com/increment/app/AssessmentScreen.kt) 实现两次独立测评和答卷导出。
- [Theme.kt](app/src/main/java/com/increment/app/Theme.kt) 定义 Material 3 浅色和深色配色。[Motion.kt](app/src/main/java/com/increment/app/Motion.kt) 提供页面入场过渡。

## 安装与运行

- 开发时用 Android Studio 打开本目录，使用随 IDE 提供的 JDK 并安装 Android SDK API 37。选择 `app` 运行配置，或在项目目录执行 `./gradlew assembleDebug lintDebug`。
- 将 [Increment-1.0.3.apk](Increment-1.0.3.apk) 安装到 Android 8.0（API 26）及以上设备，或拖入 Android 模拟器。课程安装包沿用此前 APK 的签名，可覆盖安装并保留本机数据。

## 音视频与字幕配对

两份输入分别是一段有声媒体（MP3、M4A、WAV、MP4 或 WebM）和一份同版本的带时间轴字幕（SRT、WebVTT 或 UTF-8 时间戳 TXT）。TXT 每行使用 `[00:12.500] Sentence text` 形式。

在「素材 → 导入自己的素材」可先选任意一份文件，退出后回来继续配对。选齐后点击「配对并预览」。默认使用完整音视频，也可以开启「自选起止时间」练习其中一段。支持播放或拖动进度条核对同步，必要时调整字幕偏移。确认后可立即练习或「存入我的素材」。保存的素材可直接复练，移除素材时保留源文件和已完成记录。

导入演示使用 61 秒的原创英文短视频《How to Remember a New Phrase》，配有离线合成旁白和 11 条对应字幕。把下列两份文件放进 Android 设备的「下载」，分别选择 MP4 和 SRT，保持字幕偏移为 0，点击「配对并预览」。播放核对后勾选确认，即可开始练习或保存到「我的素材」。

- [演示视频（MP4）](示例素材/remember_a_phrase.mp4)
- [对应英文字幕（SRT）](示例素材/remember_a_phrase.srt)

## 体验流程

从「今日 → 开始今日练习」进入 TED 原片，也可在「素材」选择其他 TED 原片或已保存的配对素材。

1. **首听**。播放素材，听完进入精读。内置 TED 原片可切换「仅听音频」。
2. **精读**。播放器下方的独立字幕框显示当前字幕和时间，提供「重听」「句子聚焦」和可用的参考译文。点「展开全部字幕」可查看时间轴、定位当前字幕或选任意一条重听。跨字幕的句子可结合相邻字幕阅读。可以记录 3–5 个关键词，再选取或填写一个本轮目标表达。
3. **复听**。从字幕重放困难片段，再用「从头复听」合上字幕重听完整练习范围。之后可直接写作，也可先录制 45–60 秒口语复述。口语可切换结构提示、仅关键词或无提示，回听后可对照清单自评并重录。
4. **独立写作**。用自己的话写英文摘要，建议 80–100 词。关键词和结构提示默认收起，主动查看会随本轮记录。
5. **写作自查**。回看初稿，对照原文和自查清单。内置素材提供参考要点。启用 AI 辅助后可获取 AI 写作反馈。
6. **修订**。修改摘要或确认保留初稿，可对照两稿或自选口语加练。写作前和修订后的录音可分别回听。启用 AI 辅助后可获取 AI 修订评价。完成后自动记入「记录」，可回看两稿和完成时间。

练习中返回后，可从「今日」续练。写作草稿、自查勾选和当前稿对应的 AI 反馈随本轮自动保存。修改稿件后可重新评价。

## 阶段测评

从「我的 → 打开阶段测评」进入。可选择 R1 或 R2 作为首次素材，第二次使用另一段，建议间隔 3–7 天。两次先以原速完整听两遍，再记录理解、限时阅读原文 90 秒、独立写英文摘要。可自选口语录制和自评，第二次沿用首次的口语选择，可按需调整。完成后可回看两次答卷，也可从系统文件选择器导出文本。两次测评使用项目原创英文和同一离线合成语音，答卷单独保存在本机。

## DeepSeek AI

未保存 API 密钥时，应用默认进入离线模式，可使用内置和本地导入的素材完成整轮练习。保存密钥后启用 AI 辅助，移除密钥后恢复离线模式。AI 请求在点击相应按钮时发送。

1. 在 [DeepSeek 开放平台](https://platform.deepseek.com/api_keys) 创建 API 密钥，打开「我的 → DeepSeek API」，粘贴并点「保存密钥」。保存后可选「测试连接」。密钥加密保存在本机，调用使用你的 DeepSeek 账户额度。
2. 第 2 步「精读」中，点当前字幕框或时间轴条目的「句子聚焦」。在弹层点「AI 解析本句」后，应用发送素材标题、主题、选中的句子、所在字幕及前后各最多四条字幕。弹层提供原声重听、整句译文、原文分段高亮及每段的句法和语境解释。同一条字幕有多句时，可在弹层内切换。
3. 第 5 步「写作自查」中，点「获取 AI 写作反馈」。应用会发送素材标题、主题、本轮带时间的字幕、摘要任务、初稿和已选的目标表达。内置素材附上参考要点。反馈优先检查内容和逻辑，并引用相关原文和字幕时间。
4. 第 6 步「修订」中，点「评价当前修订稿」。应用会发送同一份任务和原文、初稿、当前稿及已有的 AI 自查反馈，评价实际改动和下一步。继续编辑后可重新评价当前版本。

精读解析使用 JSON 模式返回整句译文和分段讲解。片段会与原句匹配后再高亮。[DeepSeek JSON 输出说明](https://api-docs.deepseek.com/zh-cn/guides/json_mode/)

## 设计依据

| 设计决定 | Google 官方资料 |
| --- | --- |
| 使用 Compose Material 3 的 TopAppBar、NavigationBar、NavigationRail、Button、FilterChip、表单和底部弹层。窄屏使用底部导航，宽屏使用侧边导航。 | [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) |
| 延续 Increment 的暖白与橙色，用 primary、primaryContainer、surface 等语义色阶承载不同层级，支持系统、浅色和深色模式。 | [Material 3 Color Roles](https://m3.material.io/styles/color/the-color-system/) |
| 标题、说明、计时和控件标签分别使用 Material 3 对应字阶。 | [Material 3 Typography](https://m3.material.io/styles/typography/applying-type) |
| 处理状态栏、系统导航栏和键盘安全区。主要交互目标使用至少 48 dp，图标按钮和播放进度带无障碍标签。 | [Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e) · [Compose Accessibility Defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) |

## 动效

- 启动时，橙色实底的圆形「i++」展开为「increment」字标，随后随开屏一起淡出，露出首页。
- 主栏目按排列顺序横向滑动切换。详情及练习步骤切换采用 200 毫秒淡入和轻微上移，位移动画使用无回弹弹簧。离开媒体页面后立即释放播放器或录音资源，嵌入的视频画面直接显示。[Material 3 Motion](https://m3.material.io/styles/motion/overview/how-it-works)
- 练习进度和当前字幕高亮随状态变化平滑更新。播放器状态和句子聚焦文字淡变，录音状态和前后对照可展开、收起。[Compose 动效 API 指南](https://developer.android.com/develop/ui/compose/animation/choose-api)
- 页面及组件动画使用 Compose 的系统时长缩放。Android 动画时长设为 0 时，状态直接过渡至目标值。[Compose MotionDurationScale](https://developer.android.com/reference/kotlin/androidx/compose/ui/MotionDurationScale)

## 内置素材

内置练习素材仅收录三部 TED 原片。全片可离线练习，保留片头、正片和片尾。素材详情提供难度说明和 TED 原片链接。

| 素材 | 视频时长（约） |
| --- | --- |
| 学习一门外语的秘密 | 10 分 45 秒 |
| 学外语真的可以让你更聪明 | 5 分 4 秒 |
| 为什么游戏是商业的基础 | 11 分 10 秒 |

视频保留原有画面和全片时间轴，内置音轨统一做了响度均衡和峰值限制。英文讲稿以原片画面字幕为依据对齐到全片时间轴，练习字幕和目标表达在 `app/src/main/assets/lessons.json`，媒体文件在 `app/src/main/res/raw/`。

补充内置素材时，在 `app/src/main/res/raw/` 放入与条目 `id` 同名的媒体，视频须设置 `video: true`，并填写毫秒计的 `end` 和 `cues`（每条包含 `start`、`end`、`text`）。

录音需用户主动授权，保存在本轮的应用私有目录。导入的媒体通过系统文件选择器读取。商业方案中的订阅、院校看板和合作素材库仍属于产品路线图。

## 本机验证

- `assembleRelease`、`lintRelease`、APK 对齐与签名校验通过。Lint 报告为 0 错误。用旧版签名和本机测试数据验证覆盖安装与草稿、历史迁移。
- 在 Pixel 8 模拟器验证三支 TED 视频的全片时长、后半段的字幕定位和重听，以及旧版短片练习的播放进度和标记迁移。
- 覆盖安装 1.0.1 后，验证内置练习库仅显示三部原片，旧版已开始的短片练习仍可续练。通过系统文件选择器配对当前演示 MP4 和 SRT，验证零偏移预览和视频精读字幕。
- 1.0.2 用模拟响应验证精读取句、跨字幕原声重听、同一条字幕内切句、分段高亮和无效响应重试。[S2 句子解析截图](screenshots/s2-sentence-analysis.png)
- 1.0.3 用导入演示视频验证独立字幕框、同步切换、重听和句子聚焦。展开时间轴后可滚到最后一条，定位当前字幕，再收起。[S2 字幕框截图](screenshots/s2-subtitle-card.png)
- 在 Pixel 8 模拟器（API 37）完成示例课，验证播放、跳转、标记、单句重听、写作草稿恢复和历史中的前后稿对照。
- 将系统动画时长设为 0 后，验证练习步骤切换及口语分支。另验证录音计时、回放、重录和继续写作。
- 完成一轮完整 TED 练习，验证首听笔记和标记重听、译文开关、目标表达、口语提示档位、修订后重录及离线完成记录。使用模拟响应核对两次 AI 写作请求的带时间戳字幕、参考要点和稿件内容。
- 在 Pixel 8 模拟器上用无联网权限的测试包验证首次启动时可离线完成整轮练习。保存测试密钥后，三个 AI 入口可用，重启后配置保留。移除密钥后恢复离线模式，可续练并完成同一轮。
- 在模拟器中验证字幕先选、重启恢复、偏移校验、保存复练和移除素材后的原文件及历史保留。另验证 SRT、WebVTT、时间戳 TXT、无效输入和跨字幕句子拼接。
- 实际播放两段测评各两遍，等待两次 90 秒限时阅读，验证两份答卷在重启后可恢复并导出为本地文本。用模拟响应验证 AI 结果随草稿保留及修改稿件后旧评价失效。
- 验证拒绝麦克风权限后的提示，自评重启恢复，写作前及修订后两段录音的分开存储和完成后清理。
- [素材库界面](screenshots/library.png)

课程第三阶段还要求云真机测试、超过 10 名同学试用、根据反馈改进及最终汇报。这些活动需要另行执行。
