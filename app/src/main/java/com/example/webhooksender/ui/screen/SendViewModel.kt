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

class SendViewModel(
    private val messageRepository: MessageRepository
) : ViewModel() {
    val historyMessages: Flow<List<MessageItem>> = messageRepository.allMessages
        .map { entities -> entities.map { it.toMessageItem() } }

    private val _uiState = MutableStateFlow<SendUiState>(SendUiState.Idle)
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()

    fun sendMessage(
        webhookUrl: String,
        keyword: String,
        content: String,
        priority: String = "一般",
        deadline: String = ""
    ) {
        if (webhookUrl.isBlank()) {
            _uiState.value = SendUiState.Error("请先配置 Webhook 地址")
            return
        }
        if (content.isBlank()) {
            _uiState.value = SendUiState.Error("待办项内容不能为空")
            return
        }

        _uiState.value = SendUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = messageRepository.sendMessage(webhookUrl, keyword, content, priority, deadline)
                if (success) {
                    _uiState.value = SendUiState.Success("发送成功！")
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
            priority = priority,
            deadline = deadline,
            timeStr = sdf.format(Date(sendTime)),
            success = success,
            errorInfo = errorSummary
        )
    }

    companion object {
        fun provideFactory(
            repository: MessageRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SendViewModel(repository) as T
                }
            }
    }
}
