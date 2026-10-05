package com.msgvault.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.msgvault.app.data.db.dao.ConversationDao
import com.msgvault.app.data.db.dao.MessageDao
import com.msgvault.app.data.db.entity.ConversationEntity
import com.msgvault.app.data.db.entity.MessageEntity

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}
