package com.example.maptry.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import com.example.maptry.notification.NotifyService.Companion.jsonNotifIdFriendRequest
import com.example.maptry.server.confirmFriend

class AcceptFriend : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var notificationManager :NotificationManager = context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        var extras = intent?.extras


        var sender = extras?.get("sender") as String
        var receiver = extras.get("receiver") as String
        var notificaionId = jsonNotifIdFriendRequest.get(sender)
        notificationManager.cancel(notificaionId as Int);
        confirmFriend(sender,receiver)
    }


}