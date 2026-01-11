package com.arkus.shoppyjuan.data.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arkus.shoppyjuan.R
import com.arkus.shoppyjuan.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShoppyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushNotificationManager: PushNotificationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token to Supabase or backend
        pushNotificationManager.saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Handle notification payload
        message.notification?.let { notification ->
            val title = notification.title ?: "ShoppyJuan"
            val body = notification.body ?: ""
            val listId = message.data["list_id"]

            showNotification(title, body, listId)
        }

        // Handle data payload
        message.data.isNotEmpty().let {
            val type = message.data["type"]
            handleDataMessage(type, message.data)
        }
    }

    private fun showNotification(title: String, body: String, listId: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ShoppyJuan Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de listas compartidas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            listId?.let { putExtra("list_id", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(NOTIFICATION_ID++, notification)
    }

    private fun handleDataMessage(type: String?, data: Map<String, String>) {
        when (type) {
            "item_added" -> {
                // Handle item added to list
                val listName = data["list_name"] ?: ""
                val itemName = data["item_name"] ?: ""
                val actorName = data["actor_name"] ?: "Alguien"
                showNotification(
                    "Nuevo artículo en $listName",
                    "$actorName añadió: $itemName",
                    data["list_id"]
                )
            }
            "item_checked" -> {
                // Handle item checked
                val listName = data["list_name"] ?: ""
                val itemName = data["item_name"] ?: ""
                val actorName = data["actor_name"] ?: "Alguien"
                showNotification(
                    "Artículo marcado en $listName",
                    "$actorName marcó: $itemName",
                    data["list_id"]
                )
            }
            "list_shared" -> {
                // Handle list shared
                val listName = data["list_name"] ?: ""
                val actorName = data["actor_name"] ?: "Alguien"
                showNotification(
                    "Nueva lista compartida",
                    "$actorName te compartió: $listName",
                    data["list_id"]
                )
            }
            "note_added" -> {
                // Handle note added
                val listName = data["list_name"] ?: ""
                val actorName = data["actor_name"] ?: "Alguien"
                showNotification(
                    "Nueva nota en $listName",
                    "$actorName añadió una nota",
                    data["list_id"]
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "shoppy_juan_channel"
        private var NOTIFICATION_ID = 0
    }
}
