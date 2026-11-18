package com.increment.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun DeepSeekSettings(deepSeek: DeepSeek) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var input by remember { mutableStateOf("") }
    val configured by deepSeek.configurationChanges.collectAsState(deepSeek.configured)
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }

    fun run(action: suspend () -> String) {
        busy = true
        status = null
        error = false
        scope.launch {
            try {
                status = action()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = true
                status = failure.deepSeekMessage("操作失败，请重试。")
            } finally {
                busy = false
            }
        }
    }

    Panel {
        Text("DeepSeek API", style = MaterialTheme.typography.titleMedium)
        Text(
            if (configured) "AI 辅助已启用" else "当前为离线模式",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        if (!configured) Supporting("可以离线完成练习。保存密钥即可启用 AI 辅助。")
        Supporting("调用 AI 使用你的 DeepSeek 账户额度。API 密钥加密保存在本机。")
        TextButton(onClick = { uriHandler.openUri("https://platform.deepseek.com/api_keys") }) {
            Text("去 DeepSeek 创建 API 密钥")
        }
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text(if (configured) "替换 API 密钥" else "API 密钥") },
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        val candidate = input.trim()
        Primary("保存密钥", enabled = !busy && candidate.isNotEmpty() && candidate.none(Char::isWhitespace)) {
            run {
                deepSeek.saveKey(candidate)
                input = ""
                "密钥已保存。可选测试连接。"
            }
        }
        if (configured) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = !busy, onClick = {
                    run {
                        deepSeek.testConnection()
                        "DeepSeek 连接成功。"
                    }
                }) { Text("测试连接") }
                TextButton(enabled = !busy, onClick = {
                    run {
                        deepSeek.removeKey()
                        input = ""
                        "密钥已移除。"
                    }
                }) { Text("移除密钥") }
            }
        }
        val feedback = if (busy) "处理中…" else status
        if (feedback != null) {
            Text(
                feedback,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
    if (configured) Panel {
        Text("在哪里用 AI", style = MaterialTheme.typography.titleMedium)
        Supporting("第 2 步精读：点字幕框中的「句子聚焦」，再点「AI 解析本句」。")
        Supporting("第 5 步写作自查：提交摘要后，点「获取 AI 写作反馈」。")
        Supporting("第 6 步修订：在完成练习前，点「评价当前修订稿」。")
    }
}

@Composable
internal fun DeepSeekAction(
    label: String,
    disclosure: String,
    result: String?,
    onResult: (String) -> Unit,
    enabled: Boolean = true,
    request: suspend () -> String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeepSeekRequest(label, disclosure, result != null, onResult, enabled = enabled, request = request)
        result?.let { DeepSeekResult(it) }
    }
}

@Composable
internal fun <T> DeepSeekRequest(
    label: String,
    disclosure: String,
    hasResult: Boolean,
    onResult: (T) -> Unit,
    enabled: Boolean = true,
    retryLabel: String = "重新生成",
    request: suspend () -> T
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Supporting(disclosure)
        OutlinedButton(
            enabled = enabled && !loading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            onClick = {
                loading = true
                error = null
                scope.launch {
                    try {
                        onResult(request())
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.deepSeekMessage("无法获取 AI 结果，请重试。")
                    } finally {
                        loading = false
                    }
                }
            }
        ) { Text(if (loading) "生成中…" else if (hasResult) retryLabel else label) }
        error?.let { text ->
            Text(text, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun DeepSeekResult(result: String) {
    SelectionContainer {
        Text(result, Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Exception.deepSeekMessage(fallback: String): String =
    (this as? DeepSeekError)?.message ?: fallback
