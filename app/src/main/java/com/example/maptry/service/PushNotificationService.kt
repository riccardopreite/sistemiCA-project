package com.example.maptry.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.maptry.activity.MainActivity
import com.example.maptry.domain.Notification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private const val TAG = "service.PushNotificationService"

        const val NOTIFICATION_CHANNEL_ID = "com.example"
        const val CHANNEL_NAME = "Maptry: Push notification"
    }

    private val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_NONE
    ).apply {
        lightColor = Color.BLUE
        lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)

        message.notification?.let {
            val intent = when(it.clickAction) {
                "new-friend-request" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "friend-request-accepted" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "new-live-event" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "place-recommendation" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                "model-retrained" -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                }
                else -> {
                    Intent(this, MainActivity::class.java) // TODO Sostituire con corretti intent
                } // TODO Capire quali altri casi ci sono, per ora ho messo solo quelli di sopra
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

            val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(true)
                .setContentTitle(it.title)
                .setContentText(it.body)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setContentIntent(pendingIntent)
                .setCategory(android.app.Notification.CATEGORY_RECOMMENDATION)

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(Random.nextInt(), notificationBuilder.build())
        }
    }

    override fun onNewToken(token: String) {
        Log.v(TAG, "onNewToken")
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            Notification.addNotificationToken(token)
            Log.i(TAG, "The Firebase Messaging token was updated.")
        }
    }
}