package com.example.maptry.service

import android.content.Intent
import android.util.Log
import com.example.maptry.domain.Notification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception

class PushNotificationService: FirebaseMessagingService() {
    companion object {
        private const val TAG = "service.PushNotificationService"
    }

    override fun handleIntent(intent: Intent) {
        Log.v(TAG, "handleIntent")
        super.handleIntent(intent)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.v(TAG, "onMessageReceived")
        super.onMessageReceived(message)
        println(message.notification?.title)
    }

    override fun onDeletedMessages() {
        Log.v(TAG, "onDeletedMessages")
        super.onDeletedMessages()
    }

    override fun onMessageSent(p0: String) {
        Log.v(TAG, "onMessageSent")
        super.onMessageSent(p0)
    }

    override fun onSendError(p0: String, p1: Exception) {
        Log.v(TAG, "onSendError")
        super.onSendError(p0, p1)
    }

    override fun onNewToken(token: String) {
        Log.v(TAG, "onNewToken")
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            Notification.addNotificationToken(token)
        }
    }
}