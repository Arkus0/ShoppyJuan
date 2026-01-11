package com.arkus.shoppyjuan.data.push

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationManager @Inject constructor(
    // Inject Supabase client here when ready
) {
    private val firebaseMessaging = FirebaseMessaging.getInstance()

    suspend fun getToken(): String? {
        return try {
            firebaseMessaging.token.await()
        } catch (e: Exception) {
            Log.e("PushNotificationManager", "Error getting FCM token", e)
            null
        }
    }

    fun saveToken(token: String) {
        // Save to local preferences or Supabase
        Log.d("PushNotificationManager", "New FCM token: $token")
        // TODO: Upload to Supabase push_subscriptions table
    }

    suspend fun subscribeToTopic(topic: String) {
        try {
            firebaseMessaging.subscribeToTopic(topic).await()
            Log.d("PushNotificationManager", "Subscribed to topic: $topic")
        } catch (e: Exception) {
            Log.e("PushNotificationManager", "Error subscribing to topic", e)
        }
    }

    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            firebaseMessaging.unsubscribeFromTopic(topic).await()
            Log.d("PushNotificationManager", "Unsubscribed from topic: $topic")
        } catch (e: Exception) {
            Log.e("PushNotificationManager", "Error unsubscribing from topic", e)
        }
    }

    suspend fun subscribeToList(listId: String) {
        subscribeToTopic("list_$listId")
    }

    suspend fun unsubscribeFromList(listId: String) {
        unsubscribeFromTopic("list_$listId")
    }
}
