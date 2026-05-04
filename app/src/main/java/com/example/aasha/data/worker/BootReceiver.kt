package com.example.aasha.data.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: MainRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                repository.rescheduleAllReminders()
                DailyReminderWorker.scheduleNext(context, sessionManager)
            }
        }
    }
}
