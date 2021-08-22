package com.example.maptry.server

import android.util.Log
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import com.example.maptry.utils.toJsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

const val endpointLive = "live-events/"
val baseUrlLive = "https://${ip}${port}/$endpointLive"

//https://casadiso.ddns.net:3000/live-events/
fun getLivePoi(user:String): JSONObject {
    Log.v("serverLiveUtils", "getLivePoi")
    println("IN GET LIVE POI")
    val url = URL(baseUrlLive + "?user=" + URLEncoder.encode(user, "UTF-8"))
    var result = JSONObject()
    val client =
        OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory, trustManager)
            .hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful) {
        result = toJsonObject(JSONArray(response.body()?.string()!!))
        println("Get poi live from friend is success")
    } else {
        println("Get poi live from friend is error")
        println(response.message())
    }
    return result
}

//https://casadiso.ddns.net:3000/live-events/add

fun addLivePOI(poiToAdd:String){
    Log.v("serverLiveUtils", "addLivePOI")
    println("IN ADD POI")
    val body: RequestBody = RequestBody.create(JSON, poiToAdd)

    val url = URL(baseUrlLive + addPOIUrl)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .post(body)
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("Add poi is success")
    }
    else{
        println("Add poi is error")
        println(response.message())
    }
}