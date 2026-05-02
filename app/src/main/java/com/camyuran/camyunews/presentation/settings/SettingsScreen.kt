package com.camyuran.camyunews.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.camyuran.camyunews.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            apiKeyInput = ""
            viewModel.dismissSaveSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("設定", style = MaterialTheme.typography.headlineSmall)

        // ── Gemini APIキー ──
        SettingsSection(title = "AI設定") {
            Text(
                "Gemini APIキーはGoogle AI Studioで無料取得できます",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.hasApiKey) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APIキー設定済み", color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = viewModel::clearApiKey) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = viewModel::testGeminiConnection,
                        enabled = !uiState.isTestingConnection,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isTestingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("確認中...")
                        } else {
                            Text("接続テスト")
                        }
                    }
                }
                uiState.connectionTestResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    val isSuccess = result.startsWith("接続OK")
                    Surface(
                        color = if (isSuccess) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSuccess) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = { clipboardManager.setText(AnnotatedString(result)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "コピー",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSuccess) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            IconButton(
                                onClick = viewModel::dismissConnectionTestResult,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "閉じる",
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSuccess) MaterialTheme.colorScheme.onSecondaryContainer
                                           else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini APIキー") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showApiKey) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "表示切り替え"
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.saveApiKey(apiKeyInput) },
                    enabled = apiKeyInput.isNotBlank() && !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState.isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("保存")
                }
            }
        }

        // ── ニュース更新 ──
        SettingsSection(title = "ニュース更新") {
            ListItem(
                headlineContent = { Text("手動で今すぐ更新") },
                supportingContent = { Text("最新のニュースをすぐに取得します") },
                trailingContent = {
                    FilledTonalButton(onClick = viewModel::manualFetch) {
                        Text("更新")
                    }
                }
            )
            ListItem(
                headlineContent = { Text("通知") },
                supportingContent = { Text("毎朝6時に通知を受け取る") },
                trailingContent = {
                    Switch(
                        checked = uiState.notificationEnabled,
                        onCheckedChange = viewModel::setNotificationEnabled
                    )
                }
            )
        }

        // ── 表示設定 ──
        SettingsSection(title = "表示設定") {
            Text("テキストサイズ", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "小", 1 to "中", 2 to "大").forEach { (scale, label) ->
                    FilterChip(
                        selected = uiState.textSizeScale == scale,
                        onClick = { viewModel.setTextSizeScale(scale) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── アプリ情報 ──
        SettingsSection(title = "アプリ情報") {
            ListItem(
                headlineContent = { Text("バージョン") },
                trailingContent = { Text(BuildConfig.VERSION_NAME) }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}
