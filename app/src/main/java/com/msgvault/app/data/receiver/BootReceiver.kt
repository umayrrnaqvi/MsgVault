package com.msgvault.app.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.msgvault.app.data.worker.BackupScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var backupScheduler: BackupScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            backupScheduler.schedulePeriodicBackup()
        }
    }
}
