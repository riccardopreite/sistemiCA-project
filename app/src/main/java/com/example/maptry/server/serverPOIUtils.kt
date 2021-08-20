package com.example.maptry.server

import com.example.maptry.config.Api
import com.example.maptry.config.Auth
import com.example.maptry.utils.toJsonObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

const val endpoint = "points-of-interest/"
const val addPOIUrl = "add"
const val removePOIUrl = "remove"
const val friendPOIUrl = "friend"
val baseUrl = "https://${Api.ip}${if (Api.port.isNotEmpty()) ":${Api.port}" else ""}/$endpoint"
val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()


//https://casadiso.ddns.net:3000/points-of-interest/
fun getPoiFromFriend(user:String,friend:String): JSONObject {
    println("IN GET POI of friend")
    val url = URL(baseUrl + "?user=" + URLEncoder.encode(user, "UTF-8") + "&friend=" + URLEncoder.encode(friend, "UTF-8"))
    var result = JSONObject()
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("FRIEND RESPONSE")
        result = toJsonObject(JSONArray(response.body?.string()!!))
        println("Get poi from friend is success")
        println(result)
    }
    else{
        println("Get poi from friend is error")
        println(response.message)
    }
    return result
}
//https://casadiso.ddns.net:3000/points-of-interest/add/
fun addPOI(poiToAdd:String): String{
    println("IN ADD POI")
    println(poiToAdd)
    val body: RequestBody = RequestBody.create(JSON, poiToAdd)

    val url = URL(baseUrl + addPOIUrl)
    var result = ""
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .post(body)
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        result = response.body?.string()!!
        println("Add poi is success")
    }
    else{
        println("Add poi is error")
        println(response.message)
    }
    return result
}

//https://casadiso.ddns.net:3000/points-of-interest/remove/
fun removePOI(poiId:String,user: String) {
    println("IN REMOVE POI")
    val formBody: RequestBody = FormBody.Builder()
        .add("poiId", poiId)
        .add("user", user)
        .build()

    val url = URL(baseUrl + removePOIUrl)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
        .addHeader("Authorization", "Bearer ${Auth.userToken}")
        .url(url)
        .delete(formBody)
        .build()

    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        println("Remove poi is success")
    }
    else{
        println("Remove poi is error")
        println(response.message)
    }
}