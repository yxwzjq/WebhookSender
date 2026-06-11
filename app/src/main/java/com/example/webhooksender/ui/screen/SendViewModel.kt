package com.example.webhooksender.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.webhooksender.data.local.MessageEntity
import com.example.webhooksender.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.webhooksender.data.local.SettingsDataStore
import kotlinx.coroutines.launch

class SendViewModel(
    private val messageRepository: MessageRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    val historyMessages: Flow<List<MessageItem>> = messageRepository.allMessages
        .map { entities -> entities.map { it.toMessageItem() } }

    private val _uiState = MutableStateFlow<SendUiState>(SendUiState.Idle)
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    fun sendMessage(webhookUrl: String, keyword: String, content: String, deadline: String = "") {
        if (webhookUrl.isBlank()) {
            _uiState.value = SendUiState.Error("请先配置 Webhook 地址")
            return
        }
        if (keyword.isBlank() && content.isBlank()) {
            _uiState.value = SendUiState.Error("关键字和正文不能同时为空")
            return
        }

        _uiState.value = SendUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = messageRepository.sendMessage(webhookUrl, keyword, content, deadline)
                if (success) {
                    _uiState.value = SendUiState.Success("发送成功！")
                    // 记忆关键字
                    settingsDataStore.saveLastKeyword(keyword)
                } else {
                    _uiState.value = SendUiState.Error("发送失败，已保存到历史记录")
                }
            } catch (e: Exception) {
                _uiState.value = SendUiState.Error("发送异常: ${e.message}")
            }
        }
    }

    fun clearUiState() {
        _uiState.value = SendUiState.Idle
    }

    private fun MessageEntity.toMessageItem(): MessageItem {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return MessageItem(
            id = id,
            keyword = keyword,
            content = content,
            deadline = deadline,
            timeStr = sdf.format(Date(sendTime)),
            success = success,
            errorInfo = errorSummary
        )
    }

    companion object {
        fun provideFactory(
            repository: MessageRepository,
            settingsDataStore: SettingsDataStore
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SendViewModel(repository, settingsDataStore) as T
                }
            }
    }
}
