package com.example.webhooksender.ui.screen

import kotlinx.coroutines.flow.Flow

sealed class SendUiState {
    object Idle : SendUiState()
    object Loading : SendUiState()
    data class Success(val message: String) : SendUiState()
    data class Error(val message: String) : SendUiState()
}

data class HistoryUiState(
    val messages: List<MessageItem> = emptyList(),
    val isEmpty: Boolean = true
)

data class MessageItem(
    val id: Int,
    val keyword: String,
    val content: String,
    val timeStr: String,
    val success: Boolean,
    val errorInfo: String?
)

data class SettingsUiState(
    val webhookUrl: String = "",
    val isSaved: Boolean = false
)
