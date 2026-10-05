package com.msgvault.app.data.repository

import com.msgvault.app.data.db.dao.ConversationDao
import com.msgvault.app.data.db.dao.MessageDao
import com.msgvault.app.data.db.entity.ConversationEntity
import com.msgvault.app.data.db.entity.MessageEntity
import com.msgvault.app.data.sms.ContactResolver
import com.msgvault.app.data.sms.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SyncResult(
    val newMessages: Int,
    val newConversations: Int,
    val totalMessages: Int
)

@Singleton
class SmsRepository @Inject constructor(
    private val smsReader: SmsReader,
    private val contactResolver: ContactResolver,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {

    suspend fun syncAllMessages(): SyncResult = withContext(Dispatchers.IO) {
        val rawMessages = smsReader.readAll()
        return@withContext processAndSave(rawMessages)
    }

    suspend fun syncNewMessages(): SyncResult = withContext(Dispatchers.IO) {
        val lastId = messageDao.getLastSmsId() ?: 0L
        val rawMessages = smsReader.readSince(lastId)
        return@withContext processAndSave(rawMessages)
    }

    private suspend fun processAndSave(rawMessages: List<com.msgvault.app.data.sms.RawSms>): SyncResult {
        if (rawMessages.isEmpty()) {
            return SyncResult(0, 0, messageDao.getMessageCount())
        }

        val entities = rawMessages.map { smsReader.toEntity(it) }
        messageDao.insertAll(entities)

        val grouped = rawMessages.groupBy { it.address }
        var newConversations = 0

        for ((address, messages) in grouped) {
            val sorted = messages.sortedByDescending { it.date }
            val latest = sorted.first()
            val contactName = contactResolver.resolve(address)
            val existing = conversationDao.getByAddress(address)

            val conversation = ConversationEntity(
                id = existing?.id ?: 0,
                address = address,
                contactName = contactName,
                lastMessage = latest.body,
                lastMessageDate = latest.date,
                messageCount = (existing?.messageCount ?: 0) + messages.size,
                unreadCount = messages.count { !it.read } + (existing?.unreadCount ?: 0)
            )

            conversationDao.upsert(conversation)
            if (existing == null) newConversations++
        }

        return SyncResult(
            newMessages = rawMessages.size,
            newConversations = newConversations,
            totalMessages = messageDao.getMessageCount()
        )
    }

    fun getConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }

    fun searchConversations(query: String): Flow<List<ConversationEntity>> {
        return conversationDao.searchConversations(query)
    }

    fun getMessages(threadId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByThread(threadId)
    }

    fun getMessagesByAddress(address: String): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByAddress(address)
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return messageDao.searchMessages(query)
    }

    suspend fun getStats(): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val messages = messageDao.getMessageCount()
        val conversations = conversationDao.getConversationCount()
        Pair(messages, conversations)
    }

    suspend fun deleteConversation(address: String) = withContext(Dispatchers.IO) {
        messageDao.deleteByAddress(address)
        conversationDao.deleteByAddress(address)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        messageDao.deleteAll()
        conversationDao.deleteAll()
    }
}
