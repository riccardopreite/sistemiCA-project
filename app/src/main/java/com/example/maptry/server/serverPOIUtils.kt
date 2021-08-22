package com.example.maptry.server

import android.util.Log
import com.example.maptry.activity.MapsActivity.Companion.firebaseAuth
import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import com.example.maptry.utils.toJsonObject
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

const val endpoint = "points-of-interest/"
const val addPOIUrl = "add"
const val removePOIUrl = "remove"
const val friendPOIUrl = "friend"
val baseUrl = "https://$ip$port/$endpoint"
val JSON = MediaType.parse("application/json; charset=utf-8")

//https://casadiso.ddns.net:3000/points-of-interest/
fun getPoi(user:String): JSONObject {
    Log.v("serverPOIUtils", "getPoi")
    println("IN GET POI")
    val url = URL(baseUrl + "?user=" + URLEncoder.encode(user, "UTF-8"))
    var result = JSONObject()
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        result = toJsonObject(JSONArray(response.body()?.string()!!))
        println("Get poi is success")
        println(result)
    }
    else{
        println("Get poi is error")
        println(response.message())
    }
    return result
}



//https://casadiso.ddns.net:3000/points-of-interest/
fun getPoiFromFriend(user:String,friend:String): JSONObject {
    Log.v("serverPOIUtils", "getPoiFromFriend")
    println("IN GET POI of friend")
    val url = URL(baseUrl + "?user=" + URLEncoder.encode(user, "UTF-8") + "&friend=" + URLEncoder.encode(friend, "UTF-8"))
    var result = JSONObject()
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("FRIEND RESPONSE")
        result = toJsonObject(JSONArray(response.body()?.string()!!))
        println("Get poi from friend is success")
        println(result)
    }
    else{
        println("Get poi from friend is error")
        println(response.message())
    }
    return result
}
//https://casadiso.ddns.net:3000/points-of-interest/add/
fun addPOI(poiToAdd:String): String{
    Log.v("serverPOIUtils", "addPOI")
    println("IN ADD POI")
    println(poiToAdd)
    val body: RequestBody = RequestBody.create(JSON, poiToAdd)

    val url = URL(baseUrl + addPOIUrl)
    var result = ""
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .post(body)
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        result = response.body()?.string()!!
        println("Add poi is success")
    }
    else{
        println("Add poi is error")
        println(response.message())
    }
    return result
}

//https://casadiso.ddns.net:3000/points-of-interest/remove/
fun removePOI(poiId:String,user: String) {
    Log.v("serverPOIUtils", "removePOI")
    println("IN REMOVE POI")
    val formBody: RequestBody = FormBody.Builder()
        .add("poiId", poiId)
        .add("user", user)
        .build()

    val url = URL(baseUrl + removePOIUrl)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer $token")
        .url(url)
        .delete(formBody)
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("Remove poi is success")
    }
    else{
        println("Remove poi is error")
        println(response.message())
    }
}