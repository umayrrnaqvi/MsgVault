package com.msgvault.app.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["address"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val contactName: String?,
    val lastMessage: String,
    val lastMessageDate: Long,
    val messageCount: Int,
    val unreadCount: Int = 0
)
