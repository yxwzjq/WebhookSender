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
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.annotation.StringRes
import com.example.webhooksender.R

import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.collectLatest

enum class Screen { Send, History, Feedback, Help }

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
                title = { Text("TodoRec") },
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
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Edit, contentDescription = "反馈") },
                    label = { Text("反馈") },
                    selected = currentScreen == Screen.Feedback,
                    onClick = { currentScreen = Screen.Feedback }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Help, contentDescription = "帮助") },
                    label = { Text("帮助") },
                    selected = currentScreen == Screen.Help,
                    onClick = { currentScreen = Screen.Help }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.Send -> SendScreen(sendViewModel, settingsViewModel) { showSettings = true }
                Screen.History -> HistoryScreen(sendViewModel)
                Screen.Feedback -> FeedbackScreen(sendViewModel)
                Screen.Help -> HelpScreen()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackScreen(
    sendViewModel: SendViewModel
) {
    var content by remember { mutableStateOf("") }
    val sendState by sendViewModel.uiState.collectAsState()
    val history by sendViewModel.historyMessages.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 功能说明
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "欢迎提交软件修改意见，帮助我们做得更好",
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // 反馈意见输入框
        OutlinedTextField(
            value = content,
            onValueChange = { if (it.length <= 100) content = it },
            label = { Text("反馈意见") },
            placeholder = { Text("请输入您的意见或建议...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            minLines = 4,
            maxLines = 8,
            supportingText = {
                Text(
                    text = "${content.length}/100",
                    color = if (content.length >= 100) Color.Red else Color.Gray
                )
            },
            isError = content.length >= 100
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Send button
        Button(
            onClick = {
                sendViewModel.sendFeedback(content)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sendState != SendUiState.Loading && content.isNotBlank(),
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

        // Recent feedback history preview
        val feedbackHistory = history.filter { it.keyword == "意见反馈" }
        if (feedbackHistory.isNotEmpty()) {
            Text(
                "最近反馈",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            feedbackHistory.take(3).forEach { item ->
                HistoryCard(item)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendScreen(
    sendViewModel: SendViewModel,
    settingsViewModel: SettingsViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val webhookUrl by settingsViewModel.webhookUrl.collectAsState()
    var content by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("P1一般") }
    var deadline by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val sendState by sendViewModel.uiState.collectAsState()
    val history by sendViewModel.historyMessages.collectAsState(initial = emptyList())

    // Priority dropdown
    var priorityExpanded by remember { mutableStateOf(false) }
    val priorities = listOf("P0高优", "P1一般", "P2低优")
    val priorityColors = mapOf(
        "P0高优" to Color(0xFFCC0000),
        "P1一般" to Color(0xFF777777),
        "P2低优" to Color(0xFF008800)
    )

    val isConfigured = webhookUrl.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 未配置 webhook 提示
        if (!isConfigured) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF3E0)
                ),
                onClick = onOpenSettings,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "请首先配置 Webhook 地址",
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        color = Color(0xFFF57C00),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Todo item input
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("待办项") },
            placeholder = { Text("输入待办事项...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            minLines = 3,
            maxLines = 6
        )

        // Priority selector
        ExposedDropdownMenuBox(
            expanded = priorityExpanded,
            onExpandedChange = { priorityExpanded = it }
        ) {
            OutlinedTextField(
                value = priority,
                onValueChange = { },
                readOnly = true,
                label = { Text("优先级") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                priorityColors[priority] ?: Color.Gray,
                                RoundedCornerShape(5.dp)
                            )
                    )
                }
            )
            ExposedDropdownMenu(
                expanded = priorityExpanded,
                onDismissRequest = { priorityExpanded = false }
            ) {
                priorities.forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            priorityColors[p] ?: Color.Gray,
                                            RoundedCornerShape(5.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(p)
                            }
                        },
                        onClick = {
                            priority = p
                            priorityExpanded = false
                        }
                    )
                }
            }
        }

        // Deadline date picker
        OutlinedTextField(
            value = deadline,
            onValueChange = { },
            label = { Text("截止时间（可选）") },
            placeholder = { Text("选择截止日期...") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                Row {
                    if (deadline.isNotBlank()) {
                        IconButton(onClick = { deadline = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "选择日期")
                    }
                }
            }
        )

        // Preview card
        if (content.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "预览",
                        fontWeight = FontWeight.Bold,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = buildString {
                            append("todos")
                            if (priority.isNotBlank()) {
                                append(" 【").append(priority).append("】")
                            }
                            if (deadline.isNotBlank()) {
                                append("\n截止时间：").append(deadline)
                            }
                            append("\n").append(content)
                        },
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Send button
        Button(
            onClick = {
                sendViewModel.sendMessage(webhookUrl, "todos", content, priority, deadline)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = sendState != SendUiState.Loading && isConfigured && content.isNotBlank(),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            history.take(3).forEach { item ->
                HistoryCard(item)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        deadline = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages) { item ->
                HistoryCard(item)
            }
        }
    }
}

@Composable
private fun HistoryCard(item: MessageItem) {
    val isFeedback = item.keyword == "意见反馈"
    val priorityColors = mapOf(
        "P0高优" to Color(0xFFCC0000),
        "P1一般" to Color(0xFF777777),
        "P2低优" to Color(0xFF008800)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 反馈消息：显示彩色圆点图标；待办消息：显示优先级圆点
            if (isFeedback) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            Color(0xFF0097A7),
                            RoundedCornerShape(5.dp)
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            priorityColors[item.priority] ?: Color.Gray,
                            RoundedCornerShape(5.dp)
                        )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isFeedback) "【意见反馈】${item.content}" else item.content,
                        fontWeight = FontWeight.Medium,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = item.timeStr,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = Color.Gray
                    )
                }
                if (!isFeedback && item.deadline.isNotBlank()) {
                    Text(
                        text = "截止 ${item.deadline}",
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                        color = Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 状态标签
            val statusText = if (item.success) "✓" else "✗"
            Text(
                text = statusText,
                color = if (item.success) SuccessGreen else ErrorRed,
                fontSize = MaterialTheme.typography.labelMedium.fontSize,
                fontWeight = FontWeight.Bold
            )
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
    val isConfigured = webhookUrl.isNotBlank()

    LaunchedEffect(Unit) {
        urlText = webhookUrl
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 功能说明
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "本APP用于工作待办项快捷记录，支持优先级标记和截止时间设置。",
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                // 配置状态
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfigured)
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
                            imageVector = if (isConfigured)
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isConfigured) PrimaryBlue else Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isConfigured)
                                "Webhook 已配置" else "请先配置 Webhook 地址",
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            color = if (isConfigured) PrimaryBlue else Color(0xFFFF9800)
                        )
                    }
                }

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Webhook 地址") },
                    placeholder = { Text("https://example.com/webhook") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "请填写要接收消息的目标 Webhook URL",
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = Color.Gray
                )
                Text(
                    "版本：1.0.4",
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

@Composable
private fun HelpScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.help_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item { HelpCard(title = R.string.help_app_desc_title, body = R.string.help_app_desc) }

        item {
            Text(
                text = stringResource(id = R.string.help_steps_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item { HelpStepCard(stepTitle = R.string.help_step_1) }
        item { HelpStepCard(stepTitle = R.string.help_step_2) }
        item {
            HelpStepCard(
                stepTitle = R.string.help_step_3_1,
                subSteps = listOf(
                    R.string.help_step_3_1_1,
                    R.string.help_step_3_1_2,
                    R.string.help_step_3_1_3
                )
            )
        }
        item {
            HelpStepCard(
                stepTitle = R.string.help_step_3_2,
                subSteps = listOf(
                    R.string.help_step_3_2_1,
                    R.string.help_step_3_2_2,
                    R.string.help_step_3_2_3
                )
            )
        }
        item {
            HelpStepCard(
                stepTitle = R.string.help_step_3_3,
                subSteps = listOf(
                    R.string.help_step_3_3_1,
                    R.string.help_step_3_3_2
                )
            )
        }
        item { HelpStepCard(stepTitle = R.string.help_step_4) }
        item { HelpStepCard(stepTitle = R.string.help_step_5) }
    }
}

@Composable
private fun HelpCard(@StringRes title: Int, @StringRes body: Int) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = body),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HelpStepCard(
    @StringRes stepTitle: Int,
    subSteps: List<Int> = emptyList()
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = stepTitle),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            if (subSteps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                subSteps.forEach { subStep ->
                    Text(
                        text = "• " + stringResource(id = subStep),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
    }
}
