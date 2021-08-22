package com.example.maptry.server

//import android.app.NotificationManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import com.example.maptry.notification.NotifyService.Companion.jsonNotifyIdFriendRequest
//
//// decline friend request
//class DeclineFriend : BroadcastReceiver() {
//    override fun onReceive(context: Context?, intent: Intent?) {
//        val notificationManager: NotificationManager =
//            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val extras = intent?.extras
//
//        val sender = extras?.get("sender")
//        val notificationId = jsonNotifyIdFriendRequest.get(sender as String)
//        notificationManager.cancel(notificationId as Int)
//    }
//}