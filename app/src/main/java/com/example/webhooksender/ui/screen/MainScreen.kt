package com.example.webhooksender.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.webhooksender.WebhookApp
import com.example.webhooksender.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

enum class Screen { Send, History, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebhookAppScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as WebhookApp
    var currentScreen by remember { mutableStateOf(Screen.Send) }

    val sendViewModel: SendViewModel = viewModel(
        factory = SendViewModel.provideFactory(app.container.messageRepository)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsDataStore)
    )

    var showSettings by remember { mutableStateOf(false) }

    // Observe send result
    LaunchedEffect(sendViewModel) {
        sendViewModel.uiState.collectLatest { state ->
            when (state) {
                is SendUiState.Success ->
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                is SendUiState.Error ->
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentScreen) {
                            Screen.Send -> "WebhookSender"
                            Screen.History -> "发送历史"
                            Screen.Settings -> "设置"
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Send, contentDescription = "发送") },
                    label = { Text("发送") },
                    selected = currentScreen == Screen.Send,
                    onClick = { currentScreen = Screen.Send }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "历史") },
                    label = { Text("历史") },
                    selected = currentScreen == Screen.History,
                    onClick = { currentScreen = Screen.History }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Send -> SendScreen(sendViewModel, settingsViewModel)
                Screen.History -> HistoryScreen(sendViewModel)
                else -> SendScreen(sendViewModel, settingsViewModel)
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            viewModel = settingsViewModel,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun SendScreen(
    sendViewModel: SendViewModel,
    settingsViewModel: SettingsViewModel
) {
    val webhookUrl by settingsViewModel.webhookUrl.collectAsState()
    var keyword by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val sendState by sendViewModel.uiState.collectAsState()
    val history by sendViewModel.historyMessages.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Webhook URL indicator
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (webhookUrl.isNotBlank())
                    Color(0xFFE3F2FD) else Color(0xFFFFF3E0)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (webhookUrl.isNotBlank())
                        Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (webhookUrl.isNotBlank()) PrimaryBlue else Color(0xFFFF9800),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (webhookUrl.isNotBlank())
                        "Webhook 已配置" else "请先配置 Webhook 地址",
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = if (webhookUrl.isNotBlank()) PrimaryBlue else Color(0xFFFF9800)
                )
            }
        }

        // Keyword input
        OutlinedTextField(
            value = keyword,
            onValueChange = { keyword = it },
            label = { Text("关键字") },
            placeholder = { Text("输入关键字...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Content input
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("正文") },
            placeholder = { Text("输入要发送的正文内容...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            minLines = 4
        )

        // Send button
        Button(
            onClick = {
                sendViewModel.sendMessage(webhookUrl, keyword, content)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sendState != SendUiState.Loading && webhookUrl.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (sendState == SendUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (sendState == SendUiState.Loading) "发送中..." else "发送")
        }

        // Status indicator
        when (sendState) {
            is SendUiState.Success -> StatusChip(
                text = (sendState as SendUiState.Success).message,
                color = SuccessGreen
            )
            is SendUiState.Error -> StatusChip(
                text = (sendState as SendUiState.Error).message,
                color = ErrorRed
            )
            else -> {}
        }

        // Recent history preview
        if (history.isNotEmpty()) {
            Text(
                "最近发送",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            history.take(3).forEach { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
private fun HistoryScreen(sendViewModel: SendViewModel) {
    val messages by sendViewModel.historyMessages.collectAsState(initial = emptyList())

    if (messages.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("暂无发送记录", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
private fun HistoryCard(item: MessageItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        if (item.success) SuccessGreen else ErrorRed,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.keyword.ifBlank { "（无关键字）" },
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize
                    )
                    Text(
                        text = item.timeStr,
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.content,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (item.success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (item.success) "✓ 成功" else "✗ ${item.errorInfo ?: "失败"}",
                        color = if (item.success) SuccessGreen else ErrorRed,
                        fontSize = MaterialTheme.typography.labelMedium.fontSize,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = color,
                fontWeight = FontWeight.Medium,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit) {
    val webhookUrl by viewModel.webhookUrl.collectAsState()
    var urlText by remember { mutableStateOf(webhookUrl) }
    val saved by viewModel.saveResult.collectAsState()

    LaunchedEffect(Unit) {
        urlText = webhookUrl
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("设置") },
        text = {
            Column {
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Webhook 地址") },
                    placeholder = { Text("https://example.com/webhook") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "请填写要接收消息的目标 Webhook URL",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.saveWebhookUrl(urlText)
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text("取消")
            }
        }
    )

    LaunchedEffect(saved) {
        if (saved) {
            viewModel.resetSaveResult()
            onDismiss()
        }
    }
}
