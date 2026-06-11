package com.example.webhooksender.data.repository

import com.example.webhooksender.data.local.MessageDao
import com.example.webhooksender.data.local.MessageEntity
import com.example.webhooksender.data.remote.WebhookApiService
import kotlinx.coroutines.flow.Flow

class MessageRepository(
    private val messageDao: MessageDao,
    private val webhookApiService: WebhookApiService
) {
    val allMessages: Flow<List<MessageEntity>> = messageDao.getAllMessages()

    suspend fun sendMessage(
        webhookUrl: String,
        keyword: String,
        content: String,
        priority: String = "一般",
        deadline: String = ""
    ): Boolean {
        val sendTime = System.currentTimeMillis()
        val result = webhookApiService.sendMessage(webhookUrl, keyword, content, priority, deadline)

        val success = result is WebhookApiService.SendResult.Success
        val errorSummary = when (result) {
            is WebhookApiService.SendResult.Success -> null
            is WebhookApiService.SendResult.Error -> result.message
        }

        messageDao.insertMessage(
            MessageEntity(
                keyword = keyword,
                content = content,
                priority = priority,
                deadline = deadline,
                webhookUrl = webhookUrl,
                sendTime = sendTime,
                success = success,
                errorSummary = errorSummary
            )
        )
        messageDao.pruneOldMessages()

        return success
    }

    suspend fun deleteMessage(id: Int) {
        messageDao.deleteMessage(id)
    }

    suspend fun clearAllMessages() {
        messageDao.deleteAllMessages()
    }
}
