package com.msgvault.app.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.msgvault.app.data.db.dao.ConversationDao
import com.msgvault.app.data.db.dao.MessageDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat { JSON, CSV }

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao
) {

    suspend fun exportAll(format: ExportFormat): Uri = withContext(Dispatchers.IO) {
        val conversations = conversationDao.getAllConversations().first()
        val allMessages = messageDao.getAllMessages().first()
        val messagesByAddress = allMessages.groupBy { it.address }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val (content, extension) = when (format) {
            ExportFormat.JSON -> {
                Pair(JsonExporter.exportAll(conversations, messagesByAddress), "json")
            }
            ExportFormat.CSV -> {
                Pair(CsvExporter.export(allMessages), "csv")
            }
        }

        val filename = "msgvault_backup_$timestamp.$extension"
        val file = File(context.cacheDir, filename)
        file.writeText(content)

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun createShareIntent(uri: Uri, format: ExportFormat): Intent {
        val mimeType = when (format) {
            ExportFormat.JSON -> "application/json"
            ExportFormat.CSV -> "text/csv"
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
