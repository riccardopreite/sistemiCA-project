package com.example.maptry.server

import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder



fun confirmFriend(sender:String,receiver:String){
    val url = URL("https://"+ MapsActivity.ip + MapsActivity.port +"/confirmFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))
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
    val url = URL("https://"+ MapsActivity.ip + MapsActivity.port +"/removeFriend?"+ URLEncoder.encode("receiver", "UTF-8") + "=" + URLEncoder.encode(receiver, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

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
    val url = URL("https://"+ MapsActivity.ip + MapsActivity.port +"/addFriend?"+ URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8")+"&"+ URLEncoder.encode("sender", "UTF-8") + "=" + URLEncoder.encode(sender, "UTF-8"))

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