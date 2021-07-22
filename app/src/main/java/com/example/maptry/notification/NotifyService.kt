package com.example.maptry.notification


import android.app.*
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.maptry.*
import com.example.maptry.activity.MapsActivity.Companion.account
import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.dataFromfirestore
import com.example.maptry.activity.MapsActivity.Companion.db
import com.example.maptry.activity.MapsActivity.Companion.geocoder
import com.example.maptry.activity.MapsActivity.Companion.listAddr
import com.example.maptry.activity.MapsActivity.Companion.myList
import com.example.maptry.activity.MapsActivity.Companion.myLive
import com.example.maptry.activity.MapsActivity.Companion.mymarker
import com.example.maptry.changeUI.*
import com.example.maptry.server.AcceptFriend
import com.example.maptry.server.DeclineFriend
import com.example.maptry.utils.createLiveJsonMarker
import com.example.maptry.utils.createMarker
import com.example.maptry.utils.notificationJson
import com.example.maptry.utils.reDraw
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.util.*
import kotlin.math.abs


class NotifyService : Service() {
    companion object {
        var jsonNotifyIdFriendRequest = JSONObject()
        var jsonNotifyIdRemind = JSONObject()
        var jsonNotifyIdExpired = JSONObject()
    }

        private lateinit var notificationManager : NotificationManager
        private var notificationChannelId : String = ""
        private var wakeLock: PowerManager.WakeLock? = null
        private var isServiceStarted = false

        override fun onBind(intent: Intent): IBinder? {
            return null
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            println("onStartCommand executed with startId: $startId")
            if (intent != null) {
                startService()
            } else {
                println(
                    "with a null intent. It has been probably restarted by the system."
                )
            }
            // by returning this we make sure the service is restarted if the system kills the service
            return START_STICKY
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun onCreate() {
            super.onCreate()
            println("The service has been created".uppercase(Locale.getDefault()))
            val notification = this.createNotification()
            startForeground(1, notification)


        }

        override fun onDestroy() {
            super.onDestroy()
            println("The service has been destroyed".uppercase(Locale.getDefault()))
        }

        private fun startService() {
            val channel =
                "findMyCarChannel"
            val serviceName: CharSequence =
                "findMyCar"
            if (isServiceStarted) return
            println("Starting the foreground service task")
            isServiceStarted = true
            // Ensures that the CPU is running, screen and keyboard backlight will be allowed to go off.
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                        acquire(10*60*1000L /*10 minutes*/)
                    }
                }
//                        var notification: Notification
//                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        val idDB = account?.email?.replace("@gmail.com", "")
                        if (idDB != null) {
                            //Listener for live marker
                            db.collection("user").document(idDB).collection("live")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    val notificationId = abs(System.nanoTime().toInt())
                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents

                                        Log.d("TAG", "Current data: ${querySnapshot.documents}")
                                        querySnapshot.documents.forEach { child ->
                                            var json: JSONObject
                                            child.data?.forEach { chi ->
                                                json = JSONObject(chi.value as HashMap<*, *>)

//                                                val nmLive =
//                                                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                                                val showLiveEvent = Intent(
                                                        context,
                                                        ShowLiveEvent::class.java
                                                )
                                                showLiveEvent.flags = FLAG_ACTIVITY_NEW_TASK or
                                                        FLAG_ACTIVITY_CLEAR_TASK

                                                showLiveEvent.putExtra(
                                                    "owner",
                                                    json.get("owner") as String
                                                )
                                                showLiveEvent.putExtra(
                                                    "address",
                                                    json.get("address") as String
                                                )
                                                showLiveEvent.putExtra(
                                                    "name",
                                                    json.get("name") as String
                                                )
                                                showLiveEvent.putExtra(
                                                    "timer",
                                                    json.get("timer") as String
                                                )
                                                val clickLiveIntent = PendingIntent.getActivity(
                                                    context,
                                                    87,
                                                    showLiveEvent,
                                                    FLAG_UPDATE_CURRENT
                                                )
//                                                stackBuilder.addParentStack(ShowLiveEvent::class.java)
//                                                stackBuilder.addNextIntent(showLiveEvent)
//                                                val clickLiveIntent =
//                                                    stackBuilder.getPendingIntent(
//                                                        87,
//                                                        FLAG_UPDATE_CURRENT
//                                                    )
                                                val notificationLive =
                                                    NotificationCompat.Builder(context, "first")
                                                        .setContentTitle("Evento Live")
                                                        .setContentText(
                                                            json.getString(
                                                                "owner"
                                                            ) + ": Ha aggiunto un nuovo POI live!"
                                                        )
                                                        .setSmallIcon(R.drawable.ic_live)
                                                        .setAutoCancel(true)
                                                        .setContentIntent(clickLiveIntent)
                                                        .setChannelId(channel)
                                                val importance = NotificationManager.IMPORTANCE_HIGH
                                                val mChannel = NotificationChannel(
                                                                channel,
                                                                serviceName,
                                                                importance
                                                              )
//                                                nmLive.createNotificationChannel(mChannel)
//
//                                                nmLive.notify(notificationId, notificationLive.build())
                                                with(NotificationManagerCompat.from(this)) {
                                                    createNotificationChannel(mChannel)
                                                    notify(notificationId, notificationLive.build())
                                                }
                                                //create live marker
                                                val list = geocoder.getFromLocationName(json.get("address") as String,1)
                                                val lat = list[0].latitude
                                                val lon = list[0].longitude
                                                val p0 = LatLng(lat,lon)

                                                val mark = createMarker(p0)
                                                mark?.setIcon(
                                                    BitmapDescriptorFactory.defaultMarker(
                                                        BitmapDescriptorFactory.HUE_GREEN))
                                                val name = json.get("name") as String
                                                val address = json.get("address") as String
                                                val timer = json.get("expiresAfter") as String
                                                val owner = json.get("owner") as String

                                                val newLiveJsonMark = createLiveJsonMarker(name,address,timer,owner)
                                                myLive.put(p0.toString(), newLiveJsonMark)
                                                myList.put(p0.toString(), newLiveJsonMark)

                                            }
                                            // eliminate item from db
                                            db.collection("user").document(idDB) .collection("live").document(child.id).delete()
                                        }
                                    }
                                }
                            //Listener for friend Request
                            db.collection("user").document(idDB).collection("friendrequest")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents

                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->
                                            child.data?.forEach {    chi ->
                                                val notificationId = abs(System.nanoTime().toInt())
                                                jsonNotifyIdFriendRequest.put(
                                                    chi.value as String,
                                                    notificationId
                                                )
                                                val notificationClickIntent =
                                                    Intent(
                                                        context,
                                                        ShowFriendRequest::class.java
                                                    )
                                                val stackBuilder = TaskStackBuilder.create(context)
                                                stackBuilder.addParentStack(ShowFriendList::class.java)
                                                stackBuilder.addNextIntent(notificationClickIntent)

                                                val acceptFriendIntent =
                                                    Intent(context, AcceptFriend::class.java)

                                                val declineFriendIntent =
                                                    Intent(context, DeclineFriend::class.java)

                                                acceptFriendIntent.putExtra(
                                                    "sender",
                                                    chi.value as String
                                                )
                                                acceptFriendIntent.putExtra("receiver", idDB)
                                                
                                                declineFriendIntent.putExtra(
                                                    "sender",
                                                    chi.value as String
                                                )

                                                notificationClickIntent.putExtra(
                                                    "sender",
                                                    chi.value as String
                                                )
                                                notificationClickIntent.putExtra(
                                                    "receiver",
                                                    idDB
                                                )

                                                val clickPendingIntent =
                                                    stackBuilder.getPendingIntent(
                                                        88,
                                                        FLAG_UPDATE_CURRENT
                                                    )


                                                val acceptPendingIntent =
                                                    PendingIntent.getBroadcast(
                                                        context,
                                                        90,
                                                        acceptFriendIntent,
                                                        FLAG_ONE_SHOT
                                                    )

                                                val declinePendingIntent =
                                                    PendingIntent.getBroadcast(
                                                        context,
                                                        91,
                                                        declineFriendIntent,
                                                        FLAG_ONE_SHOT
                                                    )


                                                val notificationFriendRequest =
                                                    NotificationCompat.Builder(context, "first")
                                                        .setContentTitle("Richiesta d'amicizia")
                                                        .setContentText(chi.value as String + ": Ti ha inviato una richiesta di amicizia!")
                                                        .setSmallIcon(R.drawable.ic_addfriend)
                                                        .setAutoCancel(true)
                                                        .setContentIntent(
                                                            clickPendingIntent
                                                        )
                                                        .addAction(
                                                            R.drawable.ic_add,
                                                            "Accetta",
                                                            acceptPendingIntent
                                                        )
                                                        // second button click intent
                                                        .addAction(
                                                            R.drawable.ic_close,
                                                        "Rifiuta",
                                                            declinePendingIntent
                                                        )
                                                        .setChannelId(channel)
                                                val importance =
                                                    NotificationManager.IMPORTANCE_HIGH
                                                val mChannel = NotificationChannel(
                                                    channel,
                                                    serviceName,
                                                    importance
                                                )
//                                                nm.createNotificationChannel(mChannel)
//
//                                                nm.notify(notificationId, notificationFriendRequest.build())
                                                with(NotificationManagerCompat.from(this)) {
                                                    createNotificationChannel(mChannel)
                                                    notify(notificationId, notificationFriendRequest.build())
                                                }
                                                db.collection("user").document(idDB)
                                                    .collection("friendrequest")
                                                    .document(child.id).delete()
                                            }
                                        }
                                    }
                                }
                            //Listener for accepted Request
                            db.collection("user").document(idDB).collection("addedfriend")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    val notificationId = abs(System.nanoTime().toInt())
                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents

                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->
                                            child.data?.forEach { chi ->
                                                val string = "Tu e " + chi.value + " ora siete Amici!"

                                                val notificationClickIntent =
                                                    Intent(
                                                        context,
                                                        ShowFriendList::class.java
                                                    )
                                                val stackBuilder = TaskStackBuilder.create(context)
                                                stackBuilder.addParentStack(ShowFriendList::class.java)
                                                stackBuilder.addNextIntent(notificationClickIntent)
                                                val clickPendingIntent =
                                                    stackBuilder.getPendingIntent(
                                                        92,
                                                        FLAG_UPDATE_CURRENT
                                                    )
                                                val notificationAddedFriend = NotificationCompat.Builder(context, "first")
                                                    .setContentTitle("Nuovo Amico!")
                                                    .setContentText(string)
                                                    .setSmallIcon(R.drawable.ic_accessibility)
                                                    .setAutoCancel(true) //collegato a tap notification
                                                    .setContentIntent(clickPendingIntent)
                                                    .setChannelId(channel)
                                                
                                                val importance =
                                                    NotificationManager.IMPORTANCE_HIGH
                                                val mChannel = NotificationChannel(
                                                    channel,
                                                    serviceName,
                                                    importance
                                                )
                                                with(NotificationManagerCompat.from(this)) {
                                                    createNotificationChannel(mChannel)
                                                    notify(notificationId, notificationAddedFriend.build())
                                                }
                                                //delete item from db
                                                db.collection("user").document(idDB)
                                                    .collection("addedfriend").document(child.id).delete()
                                                }
                                            }
                                        }
                                    }
                            
                            //Listener for timer expired live
                            db.collection("user").document(idDB).collection("timedLiveExpired")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    val notificationId = abs(System.nanoTime().toInt())

                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents
//                                        var key = ""
                                        var json: JSONObject
                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->


                                            child.data?.forEach { chi ->

                                                json = JSONObject(chi.value as HashMap<*, *>)
                                                val nameLiveExp = json.get("name") as String
                                                val address = json.get("address") as String
                                                val id = account?.email?.replace("@gmail.com","")
                                                val voidIntent = Intent()
                                                val voidPendingIntent = PendingIntent.getActivity(
                                                    context,
                                                    103,
                                                    voidIntent,
                                                    FLAG_ONE_SHOT
                                                )
                                                val notificationLiveExpired =
                                                    NotificationCompat.Builder(context, "first")
                                                        .setContentTitle("Live")
                                                        .setContentText("E' finito l'evento " + json.getString("name") + ".")
                                                        .setSmallIcon(R.drawable.ic_live)
                                                        .setAutoCancel(true)
                                                        .setChannelId(channel)
                                                        .setContentIntent(voidPendingIntent)
                                                val importance =
                                                    NotificationManager.IMPORTANCE_HIGH
                                                val mChannel = NotificationChannel(
                                                    channel,
                                                    nameLiveExp,
                                                    importance
                                                )
                                                with(NotificationManagerCompat.from(this)) {
                                                    createNotificationChannel(mChannel)
                                                    notify(notificationId, notificationLiveExpired.build())
                                                }

                                                // found and delete marker from map
                                                listAddr = geocoder.getFromLocationName(address, 1)
                                                for (i in myLive.keys()){
                                                    if(myLive.getJSONObject(i).get("name") as String == nameLiveExp){
                                                        myLive.remove(i)
                                                        myList.remove(i)
                                                        val mark = mymarker[i] as Marker
                                                        mark.remove()
                                                        mymarker.remove(i)

                                                        id?.let { it1 -> db.collection("user").document(it1).collection("living").get()
                                                            .addOnSuccessListener { result ->
                                                                for (document in result) {
                                                                    val namedb = document.data["name"]
                                                                    if(namedb == nameLiveExp)  {
                                                                        db.document("user/"+id+"/living/"+document.id).delete()
                                                                        return@addOnSuccessListener
                                                                    }
                                                                }
                                                            }
                                                            .addOnFailureListener { exception ->
                                                                Log.d("FAIL", "Error getting documents: ", exception)
                                                            }
                                                        }

                                                        reDraw()
                                                        break
                                                    }
                                                }
                                                //val list = geocoder.getFromLocationName(address,1)
                                            }

                                            // delete item from db
                                            db.collection("user").document(idDB)
                                                .collection("timedLiveExpired").document(child.id).delete()
                                        }
                                    }
                                }
                        }
        }
    private fun createNotification(): Notification {
        //set up foreground notification
        notificationChannelId = "ENDLESS SERVICE CHANNEL"
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern =
                    longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }
        val builder: Notification.Builder =Notification.Builder(
            this,
            notificationChannelId
        )
            .setContentTitle("Servizio Live")
            .setContentText("Rimaniamo in ascolto per tenerti sempre aggiornato!")
            .setSmallIcon(R.drawable.ic_location)
        return builder.build()
    }
}
