package com.example.aasha.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.aasha.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BoosterReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val patientName = inputData.getString("patientName") ?: "Patient"
        val vaccineName = inputData.getString("vaccineName") ?: "Vaccine"

        showNotification(patientName, vaccineName)

        return Result.success()
    }

    private fun showNotification(patientName: String, vaccineName: String) {
        val channelId = "booster_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.booster_reminders_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.booster_reminders_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use a proper icon if available
            .setContentTitle(context.getString(R.string.upcoming_vaccination_title))
            .setContentText(context.getString(R.string.booster_reminder_msg, patientName, vaccineName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
