package com.example.aasha.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.aasha.R
import com.example.aasha.data.local.AppointmentDao
import com.example.aasha.data.local.SessionManager
import com.example.aasha.data.local.VaccinationDao
import com.example.aasha.domain.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appointmentDao: AppointmentDao,
    private val vaccinationDao: VaccinationDao,
    private val sessionManager: SessionManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val workerId = sessionManager.workerId.first() ?: return Result.success()
        
        val calendar = Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        
        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 50)
        }.timeInMillis

        // This is a simplified check. In a real app, you'd have specific queries for today's tasks.
        val appointments = appointmentDao.getAllAppointments(workerId).first()
        val todayAppointments = appointments.filter { it.dateTime in startOfDay..endOfDay }
        
        if (todayAppointments.isNotEmpty()) {
            showNotification(
                context.getString(R.string.todays_appointments_title),
                context.getString(R.string.appointments_scheduled_msg, todayAppointments.size)
            )
        }

        // Reschedule for next day
        scheduleNext(context, sessionManager)

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "daily_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.daily_reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.daily_reminders_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        suspend fun scheduleNext(context: Context, sessionManager: SessionManager) {
            val timeStr = sessionManager.notificationTime.first()
            val parts = timeStr.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            val delay = calendar.timeInMillis - now

            val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "daily_reminder",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
