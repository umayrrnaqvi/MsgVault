package com.msgvault.app.data.export

import com.msgvault.app.data.db.entity.MessageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun export(messages: List<MessageEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("Address,Contact,Date,Type,Read,Body")

        for (msg in messages) {
            sb.appendLine(
                "${escape(msg.address)}," +
                "${escape("")}," +
                "${escape(formatDate(msg.date))}," +
                "${escape(typeLabel(msg.type))}," +
                "${msg.read}," +
                escape(msg.body)
            )
        }

        return sb.toString()
    }

    fun exportWithContacts(
        messages: List<MessageEntity>,
        contactMap: Map<String, String?>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Address,Contact,Date,Type,Read,Body")

        for (msg in messages) {
            val contact = contactMap[msg.address] ?: ""
            sb.appendLine(
                "${escape(msg.address)}," +
                "${escape(contact)}," +
                "${escape(formatDate(msg.date))}," +
                "${escape(typeLabel(msg.type))}," +
                "${msg.read}," +
                escape(msg.body)
            )
        }

        return sb.toString()
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun typeLabel(type: Int): String = when (type) {
        MessageEntity.TYPE_INBOX -> "received"
        MessageEntity.TYPE_SENT -> "sent"
        MessageEntity.TYPE_DRAFT -> "draft"
        else -> "unknown"
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
    }
}
