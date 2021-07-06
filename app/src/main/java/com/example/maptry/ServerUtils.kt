package com.example.maptry

import com.example.maptry.MapsActivity.Companion.context
import com.example.maptry.MapsActivity.Companion.ip
import com.example.maptry.MapsActivity.Companion.port
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


/*Start Server Function*/

fun resetTimerAuto(car:JSONObject){
     var url = URL("http://"+ip+port+"/reminderAuto?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(car.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(car.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(car.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(car.get("addr") as String, "UTF-8"))

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            println(response.body()?.string())
        }
    })
}

fun reminderAuto(car:JSONObject){
    var url = URL("http://"+ip+port+"/reminderAuto?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(car.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(car.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(car.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(car.get("addr") as String, "UTF-8"))

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            println(response.body()?.string())
        }
    })
}

/*fun getPoiFromFriend(friend:String):JSONObject{
    println("IN GET POI")
    var url = URL("http://"+ip+port+"/getPoiFromFriend?"+ URLEncoder.encode("friend", "UTF-8") + "=" + URLEncoder.encode(friend, "UTF-8"))
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
    var url = URL("http://"+ip+port+"/confirmFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            println(response.body()?.string())
        }
    })
}

fun removeFriend(sender:String,receiver:String){
    var url = URL("http://"+ip+port+"/removeFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            println(response.body()?.string())
        }
    })
}

fun sendFriendRequest(username:String,sender:String){
    var url = URL("https://"+ip+port+"/addFriend?"+ URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))
    println("CIAO");
    println(url);

    // SSL certificate configuration
    val trustStore = KeyStore.getInstance("BKS")
    val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = "SistemiContextAware2021@*"

    context.resources.openRawResource(R.raw.mystore).use {
        trustStore.load(it,keyPair.toCharArray())
    }
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
        init(trustStore)
    }
    val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, tmf.trustManagers, SecureRandom())
    }
    val trustManager = tmf.trustManagers[0] as X509TrustManager



    var client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).build()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {println(e)}
        override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
    })
}
fun startLive(live:JSONObject){
    var url = URL("http://"+ip+port+"/startLive?"+ URLEncoder.encode("owner", "UTF-8") + "=" + URLEncoder.encode(live.get("owner") as String, "UTF-8")+"&"+ URLEncoder.encode("timer", "UTF-8") + "=" + URLEncoder.encode(live.get("timer").toString(), "UTF-8")+"&"+ URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(live.get("name") as String, "UTF-8")+"&"+ URLEncoder.encode("addr", "UTF-8") + "=" + URLEncoder.encode(live.get("addr") as String, "UTF-8"))
    val client = OkHttpClient()
    val request = Request.Builder()
        .url(url)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: okhttp3.Call, e: IOException) {
            println("something went wrong")
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            println(response.body()?.string())
        }
    })
}

/*End Server Function*/