package com.msgvault.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.msgvault.app.data.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM conversations ORDER BY lastMessageDate DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE contactName LIKE '%' || :query || '%' OR address LIKE '%' || :query || '%' ORDER BY lastMessageDate DESC")
    fun searchConversations(query: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): ConversationEntity?

    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()

    @Query("DELETE FROM conversations WHERE address = :address")
    suspend fun deleteByAddress(address: String)

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsList(): List<ConversationEntity>
}
