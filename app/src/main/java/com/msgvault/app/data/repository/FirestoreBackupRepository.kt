package com.msgvault.app.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.msgvault.app.data.db.dao.ConversationDao
import com.msgvault.app.data.db.dao.MessageDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val prefs = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)

    suspend fun syncToCloud() {
        val uid = auth.currentUser?.uid ?: return

        val lastSyncedId = prefs.getLong("last_cloud_synced_id", 0L)
        val newMessages = messageDao.getMessagesSince(lastSyncedId)

        if (newMessages.isNotEmpty()) {
            newMessages.chunked(400).forEach { batch ->
                val writeBatch = firestore.batch()
                batch.forEach { msg ->
                    val ref = firestore.collection("users").document(uid)
                        .collection("messages").document(msg.smsId.toString())
                    writeBatch.set(ref, mapOf(
                        "smsId" to msg.smsId,
                        "address" to msg.address,
                        "body" to msg.body,
                        "date" to msg.date,
                        "type" to msg.type
                    ))
                }
                writeBatch.commit().await()
            }

            val maxSyncedId = newMessages.maxOf { it.smsId }
            prefs.edit().putLong("last_cloud_synced_id", maxSyncedId).apply()
        }

        val conversations = conversationDao.getAllConversationsList()
        conversations.forEach { conv ->
            val ref = firestore.collection("users").document(uid)
                .collection("conversations").document(conv.address.replace("/", "_"))
            ref.set(mapOf(
                "address" to conv.address,
                "contactName" to (conv.contactName ?: conv.address),
                "lastMessage" to conv.lastMessage,
                "lastMessageDate" to conv.lastMessageDate,
                "messageCount" to conv.messageCount
            )).await()
        }

        firestore.collection("users").document(uid).update(
            mapOf(
                "messageCount" to (messageDao.getMessageCount()),
                "lastSyncAt" to System.currentTimeMillis()
            )
        ).await()
    }
}
