package com.example.maptry


import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.db
import com.example.maptry.MapsActivity.Companion.myList
import com.example.maptry.MapsActivity.Companion.myLive
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.dataFromfirestore
import com.example.maptry.MapsActivity.Companion.geocoder
import com.example.maptry.MapsActivity.Companion.isRunning
import com.example.maptry.MapsActivity.Companion.listAddr
import com.example.maptry.MapsActivity.Companion.myCar
import com.example.maptry.MapsActivity.Companion.mymarker
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject


class NotifyService : Service() {
    companion object {
        var jsonNotifIdLive = JSONObject()
        var jsonNotifIdFriendRequest = JSONObject()
        var jsonNotifIdRemind = JSONObject()
        var jsonNotifIdExpired = JSONObject()
    }

        lateinit var notificationManager : NotificationManager
        var notificationChannelId : String = ""
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
            println("The service has been created".toUpperCase())
            var notification = this.createNotification()
            startForeground(1, notification)


        }

        override fun onDestroy() {
            super.onDestroy()
            println("The service has been destroyed".toUpperCase())
        }

        private fun startService() {
            val CHANNEL_ID =
                "findMyCarChannel"
            val name: CharSequence =
                "findMyCar"
            if (isServiceStarted) return
            println("Starting the foreground service task")
            isServiceStarted = true
            // Ensures that the CPU is running, screen and keyboard backlight will be allowed to go off.
            wakeLock =
                (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                        newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                        acquire()
                    }
                }
                        var notification: Notification
                        val nm =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        var idDB = account?.email?.replace("@gmail.com", "")
                        if (idDB != null) {
                            //Listner for live marker
                            db.collection("user").document(idDB).collection("live")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    var notificationId = Math.abs(System.nanoTime().toInt())
                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents

                                        Log.d("TAG", "Current data: ${querySnapshot.documents}")
                                        querySnapshot.documents.forEach { child ->
                                            var json = JSONObject()
                                            child.data?.forEach { chi ->
                                                json = JSONObject(chi.value as HashMap<*, *>)

                                                val nm =
                                                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                                var notification =
                                                    NotificationCompat.Builder(context, "first")
                                                        .apply {
                                                            setContentTitle("Evento Live")
                                                            setContentText(
                                                                json.getString(
                                                                    "owner"
                                                                ) + ": Ha aggiunto un nuovo POI live!"
                                                            )
                                                            setSmallIcon(R.drawable.ic_live)
                                                            setAutoCancel(true)
                                                            // set up intent for on click
                                                                val showLiveEvent: Intent =
                                                                    Intent(
                                                                        context,
                                                                        ShowLiveEvent::class.java
                                                                    )
                                                                showLiveEvent.putExtra(
                                                                    "owner",
                                                                    json.get("owner") as String
                                                                )
                                                                showLiveEvent.putExtra(
                                                                    "address",
                                                                    json.get("addr") as String
                                                                )
                                                                showLiveEvent.putExtra(
                                                                    "name",
                                                                    json.get("name") as String
                                                                )
                                                                showLiveEvent.putExtra(
                                                                    "timer",
                                                                    json.get("timer") as String
                                                                )
                                                                setContentIntent(
                                                                    PendingIntent.getActivity(
                                                                        context,
                                                                        87,
                                                                        showLiveEvent,
                                                                        FLAG_UPDATE_CURRENT
                                                                    )
                                                                )
                                                         priority =
                                                                NotificationCompat.PRIORITY_DEFAULT

                                                            // channel for android O
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                                                val importance =
                                                                    NotificationManager.IMPORTANCE_HIGH
                                                                val mChannel = NotificationChannel(
                                                                    CHANNEL_ID,
                                                                    name,
                                                                    importance
                                                                )
                                                                nm.createNotificationChannel(mChannel)
                                                                setChannelId(CHANNEL_ID)
                                                            }
                                                        }.build()

                                                //create live marker
                                                val list = geocoder.getFromLocationName(json.get("addr") as String,1)
                                                val lat = list[0].latitude
                                                val lon = list[0].longitude
                                                val p0 = LatLng(lat,lon)
                                                nm.notify(notificationId, notification)
                                                val mark = createMarker(p0)
                                                mark?.setIcon(
                                                    BitmapDescriptorFactory.defaultMarker(
                                                        BitmapDescriptorFactory.HUE_GREEN))
                                                myLive.put(p0.toString(), json)
                                                json.put("cont", "Live")
                                                json.put("type", "Pubblico")
                                                json.put("marker", mark)
                                                json.put("url", "da implementare")
                                                json.put("phone", "da implementare")
                                                myList.put(p0.toString(), json)
                                                if (mark != null) {
                                                    writeNewLive(idDB,json.get("name") as String,json.get("addr") as String,json.get("timer") as String,json.get("owner") as String,mark,"da implementare","da implementare","Pubblico","Live")
                                                }
                                            }
                                            // eliminate item from db
                                            db.collection("user").document(idDB)
                                                .collection("live").document(child.id).delete()
                                        }
                                    }
                                }
                            //Listner for friend Request
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
                                                var notificationId = Math.abs(System.nanoTime().toInt())
                                                jsonNotifIdFriendRequest.put(
                                                    chi.value as String,
                                                    notificationId
                                                )
                                                notification =
                                                    NotificationCompat.Builder(context, "first").apply {
                                                        setContentTitle("Richiesta d'amicizia")
                                                        setContentText(chi.value as String + ": Ti ha inviato una richiesta di amicizia!")
                                                        setSmallIcon(R.drawable.ic_addfriend)
                                                        setAutoCancel(true)


                                                        // on click intent
                                                            val notificationClickIntent: Intent =
                                                                Intent(
                                                                    context,
                                                                    ShowFriendRequest::class.java
                                                                )
                                                            notificationClickIntent.putExtra(
                                                                "sender",
                                                                chi.value as String
                                                            )
                                                            notificationClickIntent.putExtra(
                                                                "receiver",
                                                                idDB
                                                            )
                                                            setContentIntent(
                                                                PendingIntent.getActivity(
                                                                    context,
                                                                    88,
                                                                    notificationClickIntent,
                                                                    FLAG_UPDATE_CURRENT
                                                                )
                                                            )

                                                        priority = NotificationCompat.PRIORITY_DEFAULT

                                                        // first button click intent
                                                        val acceptFriendIntent: Intent =
                                                            Intent(context, AcceptFriend::class.java)
                                                        acceptFriendIntent.putExtra(
                                                            "sender",
                                                            chi.value as String
                                                        )
                                                        acceptFriendIntent.putExtra("receiver", idDB);


                                                        val acceptPendingIntent =
                                                            PendingIntent.getBroadcast(
                                                                context,
                                                                90,
                                                                acceptFriendIntent,
                                                                PendingIntent.FLAG_ONE_SHOT
                                                            )

                                                        addAction(
                                                            R.drawable.ic_add,
                                                            "Accetta",
                                                            acceptPendingIntent
                                                        )

                                                        // second button click intent
                                                        val declineFriendIntent: Intent =
                                                            Intent(context, DeclineFriend::class.java)
                                                        declineFriendIntent.putExtra(
                                                            "sender",
                                                            chi.value as String
                                                        )
                                                        val declinePendingIntent =
                                                            PendingIntent.getBroadcast(
                                                                context,
                                                                91,
                                                                declineFriendIntent,
                                                                PendingIntent.FLAG_ONE_SHOT
                                                            )
                                                        addAction(
                                                            R.drawable.ic_close,
                                                            "Rifiuta",
                                                            declinePendingIntent
                                                        )
                                                        // channel for android O
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            val importance =
                                                                NotificationManager.IMPORTANCE_HIGH
                                                            val mChannel = NotificationChannel(
                                                                CHANNEL_ID,
                                                                name,
                                                                importance
                                                            )
                                                            nm.createNotificationChannel(mChannel)
                                                            setChannelId(CHANNEL_ID)
                                                        }

                                                    }.build()
                                                nm.notify(notificationId, notification)
                                                db.collection("user").document(idDB)
                                                    .collection("friendrequest")
                                                    .document(child.id).delete()
                                            }
                                        }
                                    }
                                }
                            //Listner for accepted Request
                            db.collection("user").document(idDB).collection("addedfriend")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    var notificationId = Math.abs(System.nanoTime().toInt())
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
                                                        notification =
                                                            NotificationCompat.Builder(context, "first").apply {
                                                                setContentTitle("Nuovo Amico!")
                                                                setContentText(string)
                                                                setSmallIcon(R.drawable.ic_accessibility)
                                                                setAutoCancel(true) //collegato a tap notification

                                                                // show friend on click notification
                                                                    val notificationClickIntent: Intent =
                                                                        Intent(
                                                                            context,
                                                                            ShowFriendList::class.java
                                                                        )
                                                                    setContentIntent(
                                                                        PendingIntent.getActivity(
                                                                            context,
                                                                            92,
                                                                            notificationClickIntent,
                                                                            FLAG_UPDATE_CURRENT
                                                                        )
                                                                    )

                                                                priority = NotificationCompat.PRIORITY_DEFAULT

                                                                // channel for android O
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                    val importance =
                                                                        NotificationManager.IMPORTANCE_HIGH
                                                                    val mChannel = NotificationChannel(
                                                                        CHANNEL_ID,
                                                                        name,
                                                                        importance
                                                                    )
                                                                    nm.createNotificationChannel(mChannel)
                                                                    setChannelId(CHANNEL_ID)
                                                                }

                                                            }.build()
                                                        nm.notify(notificationId, notification)
                                                //delete item from db
                                                db.collection("user").document(idDB)
                                                    .collection("addedfriend").document(child.id).delete()
                                                }
                                            }
                                        }
                                    }
//                                }
                            //Listner for timer almost expired car
                            db.collection("user").document(idDB).collection("timed")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    var notificationId = Math.abs(System.nanoTime().toInt())

                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents
                                        var key = ""
                                        var json = JSONObject()
                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->


                                            child.data?.forEach { chi ->
                                                var exist = false
                                                json = JSONObject(chi.value as HashMap<*, *>)
                                                key =
                                                    json.get("owner") as String + json.get("name") as String
                                                var name = json.get("name") as String
                                                var owner = json.get("owner") as String
                                                //check if car was eliminated before timer expired, in this case doesnt show notification
                                                for (i in myCar.keys()) {
                                                    if (name == myCar.getJSONObject(i)
                                                            .get("name") as String
                                                    ) {
                                                        exist = true
                                                        break
                                                    }
                                                }
                                                if (exist){
                                                    jsonNotifIdRemind.put(
                                                        json.getString("owner"),
                                                        notificationId
                                                    )
                                                notification =
                                                    NotificationCompat.Builder(context, "first")
                                                        .apply {
                                                            setContentTitle("Reminder auto")
                                                            setContentText(
                                                                "Sta finendo il timer di " + json.getString(
                                                                    "name"
                                                                ) + ". 5 minuti rimanenti"
                                                            )

                                                            setSmallIcon(R.drawable.ic_car)
                                                            setAutoCancel(true) //collegato a tap notification

                                                            // prepare intent for all action on notification
                                                            val notificationClickIntent: Intent =
                                                                Intent(context, ShowCar::class.java)
                                                            notificationClickIntent.putExtra(
                                                                "name",
                                                                name
                                                            )
                                                            setContentIntent(
                                                                PendingIntent.getActivity(
                                                                    context,
                                                                    95,
                                                                    notificationClickIntent,
                                                                    FLAG_UPDATE_CURRENT
                                                                )
                                                            )
                                                            //intent for open car
                                                            priority =
                                                                NotificationCompat.PRIORITY_DEFAULT
                                                            val acceptReminderIntent: Intent =
                                                                Intent(
                                                                    context, /*ShowCar::class.java*/
                                                                    RemindTimer::class.java
                                                                ) // change intent
                                                            acceptReminderIntent.putExtra(
                                                                "name",
                                                                name
                                                            )
                                                            acceptReminderIntent.putExtra(
                                                                "owner",
                                                                owner
                                                            )

                                                            val acceptPendingIntent =
                                                                PendingIntent.getActivity(
                                                                    context,
                                                                    0,
                                                                    acceptReminderIntent,
                                                                    PendingIntent.FLAG_UPDATE_CURRENT
                                                                )

                                                            addAction(
                                                                R.drawable.ic_add,
                                                                "Rimanda",
                                                                acceptPendingIntent
                                                            )

                                                            // set channel for android O
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                val importance =
                                                                    NotificationManager.IMPORTANCE_HIGH
                                                                val mChannel = NotificationChannel(
                                                                    CHANNEL_ID,
                                                                    name,
                                                                    importance
                                                                )
                                                                nm.createNotificationChannel(
                                                                    mChannel
                                                                )
                                                                setChannelId(CHANNEL_ID)
                                                            }

                                                        }.build()
                                                nm.notify(notificationId, notification)

                                                notificationJson.put(key, json)
                                            }
                                            }
                                            //delete item from db
                                            db.collection("user").document(idDB)
                                                .collection("timed").document(child.id).delete()

                                        }
                                    }
                                }
                            //Listner for timer expired car
                            db.collection("user").document(idDB).collection("timedExpired")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    var notificationId = Math.abs(System.nanoTime().toInt())

                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents
                                        var key = ""
                                        var json = JSONObject()
                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->


                                            child.data?.forEach { chi ->
                                                var exist = false
                                                json = JSONObject(chi.value as HashMap<*, *>)
                                                key =
                                                    json.get("owner") as String + json.get("name") as String
                                                val name = json.get("name") as String
                                                val owner = json.get("owner") as String
                                                val address = json.get("addr") as String
                                                //check if car was eliminated before timer expired, in this case doesnt show notification
                                                for (i in myCar.keys()) {
                                                    if (name == myCar.getJSONObject(i)
                                                            .get("name") as String
                                                    ) {
                                                        exist = true
                                                        break
                                                    }
                                                }
                                                if(exist){
                                                    jsonNotifIdExpired.put(
                                                        json.getString("owner"),
                                                        notificationId
                                                    )
                                                    notification =
                                                        NotificationCompat.Builder(context, "first")
                                                            .apply {
                                                                setContentTitle("Reminder auto")
                                                                setContentText(
                                                                    "E' finito il timer di " + json.getString(
                                                                        "name"
                                                                    ) + "."
                                                                )

                                                                setSmallIcon(R.drawable.ic_car)
                                                                setAutoCancel(true)

                                                                // prepare intent for all action on notification
                                                                setContentIntent(
                                                                    PendingIntent.getActivity(
                                                                        context,
                                                                        92,
                                                                        Intent(),
                                                                        FLAG_UPDATE_CURRENT
                                                                    )
                                                                )

                                                                val notificationClickIntent: Intent =
                                                                    Intent(context, ShowCar::class.java)
                                                                notificationClickIntent.putExtra(
                                                                    "name",
                                                                    name
                                                                )
                                                                setContentIntent(
                                                                    PendingIntent.getActivity(
                                                                        context,
                                                                        99,
                                                                        notificationClickIntent,
                                                                        FLAG_UPDATE_CURRENT
                                                                    )
                                                                )

                                                                priority =
                                                                    NotificationCompat.PRIORITY_DEFAULT
                                                                val acceptReminderIntent: Intent =
                                                                    Intent(
                                                                        context,
                                                                        RemindTimer::class.java
                                                                    )
                                                                acceptReminderIntent.putExtra(
                                                                    "name",
                                                                    name
                                                                )
                                                                acceptReminderIntent.putExtra(
                                                                    "owner",
                                                                    owner
                                                                )

                                                                val acceptPendingIntent =
                                                                    PendingIntent.getActivity(
                                                                        context,
                                                                        97,
                                                                        acceptReminderIntent,
                                                                        PendingIntent.FLAG_UPDATE_CURRENT
                                                                    )

                                                                addAction(
                                                                    R.drawable.ic_add,
                                                                    "Rimanda",
                                                                    acceptPendingIntent
                                                                )
                                                                val deleteReminderIntent: Intent =
                                                                    Intent(
                                                                        context,
                                                                        DeleteTimer::class.java
                                                                    ) // change intent
                                                                deleteReminderIntent.putExtra(
                                                                    "name",
                                                                    name
                                                                )
                                                                deleteReminderIntent.putExtra(
                                                                    "owner",
                                                                    owner
                                                                )
                                                                deleteReminderIntent.putExtra(
                                                                    "address",
                                                                    address
                                                                )

                                                                val deletePendingIntent =
                                                                    PendingIntent.getActivity(
                                                                        context,
                                                                        98,
                                                                        deleteReminderIntent,
                                                                        FLAG_UPDATE_CURRENT
                                                                    )

                                                                addAction(
                                                                    R.drawable.ic_closenotification,
                                                                    "Elimina",
                                                                    deletePendingIntent
                                                                )

                                                                // set channel for android O
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                    val importance =
                                                                        NotificationManager.IMPORTANCE_HIGH
                                                                    val mChannel = NotificationChannel(
                                                                        CHANNEL_ID,
                                                                        name,
                                                                        importance
                                                                    )
                                                                    nm.createNotificationChannel(
                                                                        mChannel
                                                                    )
                                                                    setChannelId(CHANNEL_ID)
                                                                }

                                                            }.build()
                                                    nm.notify(notificationId, notification)

                                                    notificationJson.put(key, json)
                                            }
                                            }
                                            // delete item from db
                                            db.collection("user").document(idDB)
                                                .collection("timedExpired").document(child.id).delete()
                                        }
                                    }
                                }
                            //Listner for timer expired live
                            db.collection("user").document(idDB).collection("timedLiveExpired")
                                .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                                    var notificationId = Math.abs(System.nanoTime().toInt())

                                    if (firebaseFirestoreException != null) {
                                        Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                                        return@addSnapshotListener
                                    }

                                    if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                                        notificationJson = JSONObject()
                                        dataFromfirestore = querySnapshot.documents
                                        var key = ""
                                        var json = JSONObject()
                                        Log.d(
                                            "TAGnotify",
                                            "Current data: ${querySnapshot.documents}"
                                        )
                                        querySnapshot.documents.forEach { child ->


                                            child.data?.forEach { chi ->

                                                json = JSONObject(chi.value as HashMap<*, *>)
                                                key =
                                                    json.get("owner") as String + json.get("name") as String
                                                val name = json.get("name") as String
                                                val owner = json.get("owner") as String
                                                val address = json.get("addr") as String
                                                val id = account?.email?.replace("@gmail.com","")

                                                notification =
                                                    NotificationCompat.Builder(context, "first")
                                                        .apply {
                                                            setContentTitle("Live")
                                                            setContentText(
                                                                "E' finito l'evento " + json.getString(
                                                                    "name"
                                                                ) + "."
                                                            )
                                                            setSmallIcon(R.drawable.ic_live)
                                                            setAutoCancel(true)
                                                            var intent = Intent()

                                                            priority =
                                                                NotificationCompat.PRIORITY_DEFAULT

                                                            // set channel for android O
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                val importance =
                                                                    NotificationManager.IMPORTANCE_HIGH
                                                                val mChannel = NotificationChannel(
                                                                    CHANNEL_ID,
                                                                    name,
                                                                    importance
                                                                )
                                                                nm.createNotificationChannel(
                                                                    mChannel
                                                                )
                                                                setChannelId(CHANNEL_ID)
                                                            }

                                                        }.build()
                                                nm.notify(notificationId, notification)

                                                // found and delete marker from map
                                                listAddr = geocoder.getFromLocationName(address, 1)
                                                for (i in myLive.keys()){
                                                    if(myLive.getJSONObject(i).get("name") as String == name){
                                                        myLive.remove(i)
                                                        myList.remove(i)
                                                        val mark = mymarker[i] as Marker
                                                        mark.remove()
                                                        mymarker.remove(i)

                                                        id?.let { it1 -> db.collection("user").document(it1).collection("living").get()
                                                            .addOnSuccessListener { result ->
                                                                for (document in result) {
                                                                    val namedb = document.data["name"]
                                                                    if(namedb == name)  {
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
                                                val list = geocoder.getFromLocationName(address,1)
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
                notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
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
                val builder: Notification.Builder =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
                        this,
                        notificationChannelId
                    ) else Notification.Builder(this)
                var notification =builder
                    .setContentTitle("Servizio Live")
                    .setContentText("Rimaniamo in ascolto per tenerti sempre aggiornato!")
                    .setSmallIcon(R.drawable.ic_location)
                    .build()
                return  notification
            }
}