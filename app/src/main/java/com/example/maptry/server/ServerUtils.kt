package com.example.maptry.server

import com.example.maptry.activity.MapsActivity.Companion.context
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import com.example.maptry.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.*


// SSL certificate configuration
val trustStore = KeyStore.getInstance("BKS")
val keyPair = "SistemiContextAware2021@*"

val res = context.resources.openRawResource(R.raw.mystore).use {
    trustStore.load(it,keyPair.toCharArray())
}
val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
    init(trustStore)
}
val sslContext = SSLContext.getInstance("TLS").apply {
    init(null, tmf.trustManagers, SecureRandom())
}
val trustManager = tmf.trustManagers[0] as X509TrustManager


val hostnameVerifier = HostnameVerifier { _, session -> //first
    val hv: HostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()
    hv.verify(ip, session)
    return@HostnameVerifier true
}

/*Start Server Function*/

fun resetTimerAuto(car:JSONObject){
     val url = URL("https://"+ip+port+"/reminderAuto?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(car.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(car.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(car.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(car.get("addr") as String, "UTF-8"))

    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong reset timer auto")
            println(e)
        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.string())
        }
    })
}

/*fun getPoiFromFriend(friend:String):JSONObject{
    println("IN GET POI")
    var url = URL("https://"+ip+port+"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(friend, "UTF-8"))
    var result = JSONObject()
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {

            val x:String = response.body()?.string()!!
            result = JSONObject(x)
        }
    })
    return result
}*/

fun confirmFriend(sender:String,receiver:String){
    val url = URL("https://"+ip+port+"/confirmFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong confirm friend")
            println(e)

        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.string())
        }
    })
}

fun removeFriend(sender:String,receiver:String){
    val url = URL("https://"+ip+port+"/removeFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .url(url)
        .build()


    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong remove friend")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
    })
}

fun sendFriendRequest(username:String,sender:String){
    val url = URL("https://"+ip+port+"/addFriend?"+ URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong sendfriend request")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
    })
}
fun startLive(live:JSONObject){
    val url = URL("https://"+ip+port+"/startLive?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(live.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(live.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(live.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(live.get("addr") as String, "UTF-8"))
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong start live")
            println(e)

        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body()?.string())
        }
    })
}

/*End Server Function*/