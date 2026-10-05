package com.msgvault.app.data.sms

import android.content.Context
import android.net.Uri
import com.msgvault.app.data.db.entity.MessageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class RawSms(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val seen: Boolean
)

@Singleton
class SmsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SMS_URI: Uri = Uri.parse("content://sms")
        private val PROJECTION = arrayOf(
            "_id", "thread_id", "address", "body", "date", "type", "read", "seen"
        )
    }

    fun readAll(): List<RawSms> {
        return readSms(selection = null, selectionArgs = null)
    }

    fun readSince(lastSmsId: Long): List<RawSms> {
        return readSms(
            selection = "_id > ?",
            selectionArgs = arrayOf(lastSmsId.toString())
        )
    }

    fun readByAddress(address: String): List<RawSms> {
        return readSms(
            selection = "address = ?",
            selectionArgs = arrayOf(address)
        )
    }

    private fun readSms(selection: String?, selectionArgs: Array<String>?): List<RawSms> {
        val messages = mutableListOf<RawSms>()

        try {
            context.contentResolver.query(
                SMS_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("_id")
                val threadIdx = cursor.getColumnIndexOrThrow("thread_id")
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")
                val typeIdx = cursor.getColumnIndexOrThrow("type")
                val readIdx = cursor.getColumnIndexOrThrow("read")
                val seenIdx = cursor.getColumnIndexOrThrow("seen")

                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx) ?: continue
                    messages.add(
                        RawSms(
                            id = cursor.getLong(idIdx),
                            threadId = cursor.getLong(threadIdx),
                            address = address,
                            body = cursor.getString(bodyIdx) ?: "",
                            date = cursor.getLong(dateIdx),
                            type = cursor.getInt(typeIdx),
                            read = cursor.getInt(readIdx) == 1,
                            seen = cursor.getInt(seenIdx) == 1
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // SMS permission not granted
        }

        return messages
    }

    fun toEntity(raw: RawSms): MessageEntity {
        return MessageEntity(
            smsId = raw.id,
            threadId = raw.threadId,
            address = raw.address,
            body = raw.body,
            date = raw.date,
            type = raw.type,
            read = raw.read,
            seen = raw.seen
        )
    }
}
