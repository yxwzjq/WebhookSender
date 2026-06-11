package com.example.webhooksender.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val content: String,
    val priority: String = "一般",
    val deadline: String = "",
    val webhookUrl: String,
    val sendTime: Long,
    val success: Boolean,
    val errorSummary: String? = null
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY sendTime DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM messages WHERE content = :content LIMIT 1)")
    suspend fun hasContent(content: String): Boolean

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Int)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM messages WHERE id NOT IN (SELECT id FROM messages ORDER BY sendTime DESC LIMIT 10)")
    suspend fun pruneOldMessages()
}
