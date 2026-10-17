package com.msgvault.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.msgvault.app.data.repository.FirestoreBackupRepository
import com.msgvault.app.data.repository.SmsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SmsRepository,
    private val firestoreBackup: FirestoreBackupRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val stats = repository.getStats()
            if (stats.first == 0) {
                repository.syncAllMessages()
            } else {
                repository.syncNewMessages()
            }
            try {
                firestoreBackup.syncToCloud()
            } catch (_: Exception) { }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "msgvault_auto_backup"
    }
}
