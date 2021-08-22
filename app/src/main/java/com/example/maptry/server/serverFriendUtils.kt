package com.example.maptry.server

import com.example.maptry.config.Api
import com.example.maptry.config.Auth
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


val baseUrlFriend = "https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}/$endpointFriend"

//https://casadiso.ddns.net:3000/friend/confirm
fun confirmFriend(jsonToAdd:String){
    val url = URL(baseUrlFriend + confirmFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToAdd)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong confirm friend")
            println(e)

        }

        override fun onResponse(call: Call, response: Response) {
            println(response.body?.string())
        }
    })
}
//https://casadiso.ddns.net:3000/friend/remove

fun removeFriend(jsonToRemove:String){
    val url = URL(baseUrlFriend + removeFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToRemove)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .delete(body)
        .build()


    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong remove friend")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body?.string())
    })
}

//https://casadiso.ddns.net:3000/friend/add
fun sendFriendRequest(jsonToAdd:String){
    val url = URL(baseUrlFriend + addFriendUrl)
    val body: RequestBody = RequestBody.create(JSON, jsonToAdd)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("something went wrong sendfriend request")
            println(e)
        }
        override fun onResponse(call: Call, response: Response) = println(response.body?.string())
    })
}