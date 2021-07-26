package com.example.maptry.server

import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import com.example.maptry.utils.toJsonObject
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import java.net.URLEncoder
const val endpointFriend = "friends/"
const val confirmFriendUrl = "confirm"
const val addFriendUrl = "add"
const val removeFriendUrl = "remove"


val baseUrlFriend = "https://${ip}${port}/$endpointFriend"

//https://casadiso.ddns.net:3000/friend/
fun getFriend(user:String): JSONObject {
    println("IN GET POI")
    val url = URL(baseUrlFriend + "?user=" + URLEncoder.encode(user, "UTF-8"))
    println(url)
    var result = JSONObject()
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        result = toJsonObject(JSONArray(response.body()?.string()!!))
        println("Get poi from friend is success")
    }
    else{
        println("Get poi from friend is error")
        println(response.message())
    }
    return result
}
//https://casadiso.ddns.net:3000/friend/confirm

fun confirmFriend(jsonToAdd:String){
    val url = URL(baseUrlFriend + confirmFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToAdd)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .post(body)
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
//https://casadiso.ddns.net:3000/friend/remove

fun removeFriend(jsonToRemove:String){
    val url = URL(baseUrlFriend + removeFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToRemove)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .delete(body)
        .build()


    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong remove friend")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
    })
}

//https://casadiso.ddns.net:3000/friend/add
fun sendFriendRequest(jsonToAdd:String){
    val url = URL(baseUrlFriend + addFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToAdd)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong sendfriend request")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body()?.string())
    })
}