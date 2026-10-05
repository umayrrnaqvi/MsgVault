package com.msgvault.app.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.msgvault.app.data.db.dao.ConversationDao
import com.msgvault.app.data.db.dao.MessageDao
import com.msgvault.app.data.db.entity.ConversationEntity
import com.msgvault.app.data.db.entity.MessageEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var messageDao: MessageDao
    @Inject lateinit var conversationDao: ConversationDao
    @Inject lateinit var contactResolver: ContactResolver

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (sms in messages) {
                    val address = sms.originatingAddress ?: continue
                    val body = sms.messageBody ?: ""
                    val date = sms.timestampMillis

                    val entity = MessageEntity(
                        smsId = date,
                        threadId = 0,
                        address = address,
                        body = body,
                        date = date,
                        type = MessageEntity.TYPE_INBOX,
                        read = false,
                        seen = false
                    )
                    messageDao.insert(entity)

                    val contactName = contactResolver.resolve(address)
                    val existing = conversationDao.getByAddress(address)
                    conversationDao.upsert(
                        ConversationEntity(
                            id = existing?.id ?: 0,
                            address = address,
                            contactName = contactName,
                            lastMessage = body,
                            lastMessageDate = date,
                            messageCount = (existing?.messageCount ?: 0) + 1,
                            unreadCount = (existing?.unreadCount ?: 0) + 1
                        )
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}
