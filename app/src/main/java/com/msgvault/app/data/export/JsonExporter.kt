package com.msgvault.app.data.export

import com.msgvault.app.data.db.entity.ConversationEntity
import com.msgvault.app.data.db.entity.MessageEntity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JsonExporter {

    fun exportAll(
        conversations: List<ConversationEntity>,
        messagesByAddress: Map<String, List<MessageEntity>>
    ): String {
        val root = JSONObject()
        root.put("app", "MsgVault")
        root.put("exportedAt", isoFormat(System.currentTimeMillis()))
        root.put("totalConversations", conversations.size)
        root.put("totalMessages", messagesByAddress.values.sumOf { it.size })

        val convArray = JSONArray()
        for (conversation in conversations) {
            val convObj = JSONObject()
            convObj.put("address", conversation.address)
            convObj.put("contactName", conversation.contactName ?: JSONObject.NULL)
            convObj.put("messageCount", conversation.messageCount)

            val messages = messagesByAddress[conversation.address] ?: emptyList()
            val msgArray = JSONArray()
            for (msg in messages) {
                val msgObj = JSONObject()
                msgObj.put("body", msg.body)
                msgObj.put("date", isoFormat(msg.date))
                msgObj.put("timestamp", msg.date)
                msgObj.put("type", typeLabel(msg.type))
                msgObj.put("read", msg.read)
                msgArray.put(msgObj)
            }
            convObj.put("messages", msgArray)
            convArray.put(convObj)
        }
        root.put("conversations", convArray)

        return root.toString(2)
    }

    fun exportConversation(
        conversation: ConversationEntity,
        messages: List<MessageEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "MsgVault")
        root.put("exportedAt", isoFormat(System.currentTimeMillis()))
        root.put("address", conversation.address)
        root.put("contactName", conversation.contactName ?: JSONObject.NULL)

        val msgArray = JSONArray()
        for (msg in messages) {
            val msgObj = JSONObject()
            msgObj.put("body", msg.body)
            msgObj.put("date", isoFormat(msg.date))
            msgObj.put("timestamp", msg.date)
            msgObj.put("type", typeLabel(msg.type))
            msgObj.put("read", msg.read)
            msgArray.put(msgObj)
        }
        root.put("messages", msgArray)

        return root.toString(2)
    }

    private fun typeLabel(type: Int): String = when (type) {
        MessageEntity.TYPE_INBOX -> "received"
        MessageEntity.TYPE_SENT -> "sent"
        MessageEntity.TYPE_DRAFT -> "draft"
        else -> "unknown"
    }

    private fun isoFormat(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(timestamp))
    }
}
