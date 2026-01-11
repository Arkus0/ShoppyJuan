package com.arkus.shoppyjuan.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.arkus.shoppyjuan.R
import com.arkus.shoppyjuan.data.local.ShoppyDatabase
import com.arkus.shoppyjuan.domain.model.RecurrenceSettings
import com.arkus.shoppyjuan.presentation.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringListWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: ShoppyDatabase
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "recurring_list_check"
        const val NOTIFICATION_CHANNEL_ID = "recurring_lists"
        private const val NOTIFICATION_ID_BASE = 3000

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<RecurringListWorker>(
                1, TimeUnit.HOURS // Check every hour
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override suspend fun doWork(): Result {
        createNotificationChannel()

        try {
            val now = System.currentTimeMillis()

            // Get all recurring lists
            val lists = database.shoppingListDao().getAllLists().first()
            val recurringLists = lists.filter { it.isRecurring && it.nextRecurrenceAt != null }

            for (list in recurringLists) {
                val settings = list.recurrenceSettingsJson?.let {
                    try {
                        Json.decodeFromString<RecurrenceSettings>(it)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (settings == null) continue

                // Check if it's time to send a reminder (before recurrence)
                if (settings.notifyBeforeRecurrence) {
                    val reminderTime = list.nextRecurrenceAt!! - (settings.notifyHoursBefore * 60 * 60 * 1000L)
                    if (now >= reminderTime && now < list.nextRecurrenceAt) {
                        sendReminderNotification(list.id, list.name, settings)
                    }
                }

                // Check if it's time to reset the list
                if (list.nextRecurrenceAt!! <= now) {
                    if (settings.resetOnRecurrence) {
                        // Uncheck all items
                        database.listItemDao().uncheckAllItems(list.id)
                    }

                    // Update next recurrence time
                    val nextRecurrence = settings.getNextOccurrence()
                    database.shoppingListDao().updateRecurrence(
                        listId = list.id,
                        nextRecurrenceAt = nextRecurrence,
                        lastResetAt = now
                    )

                    // Send reset notification
                    sendResetNotification(list.id, list.name)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Listas Recurrentes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de listas de compras recurrentes"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendReminderNotification(listId: String, listName: String, settings: RecurrenceSettings) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "list_detail")
            putExtra("list_id", listId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            listId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Proxima compra: $listName")
            .setContentText("Tu lista se restablecera en ${settings.notifyHoursBefore} hora(s)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + listId.hashCode(), notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun sendResetNotification(listId: String, listName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "list_detail")
            putExtra("list_id", listId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            listId.hashCode() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Lista restablecida")
            .setContentText("$listName esta lista para tu proxima compra")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(NOTIFICATION_ID_BASE + listId.hashCode() + 1000, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
}
