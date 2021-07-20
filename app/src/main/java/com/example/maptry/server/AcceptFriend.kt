package com.example.maptry.server

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import com.example.maptry.notification.NotifyService.Companion.jsonNotifyIdFriendRequest

class AcceptFriend : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager :NotificationManager = context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val extras = intent?.extras


        val sender = extras?.get("sender") as String
        val receiver = extras.get("receiver") as String
        val notificationId = jsonNotifyIdFriendRequest.get(sender)
        notificationManager.cancel(notificationId as Int)
        confirmFriend(sender, receiver)
    }
}