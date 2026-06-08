package com.example.webhooksender.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val keyword: String,
    val content: String,
    val webhookUrl: String,
    val sendTime: Long,
    val success: Boolean,
    val errorSummary: String? = null
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY sendTime DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Insert
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Int)

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()
}
