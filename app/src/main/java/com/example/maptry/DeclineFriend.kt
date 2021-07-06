package com.example.maptry

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.maptry.NotifyService.Companion.jsonNotifIdFriendRequest

// decline friend request
class DeclineFriend : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var notificationManager: NotificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var extras = intent?.extras

        var sender = extras?.get("sender")
        var notificaionId = jsonNotifIdFriendRequest.get(sender as String)
        notificationManager.cancel(notificaionId as Int)
    }
}