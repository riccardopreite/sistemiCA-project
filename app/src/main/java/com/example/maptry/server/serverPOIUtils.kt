package com.example.maptry.server

import com.example.maptry.activity.MapsActivity.Companion.ip
import com.example.maptry.activity.MapsActivity.Companion.port
import okhttp3.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

const val endpoint = "points-of-interest/"
const val addPOIUrl = "add"
const val removePOIUrl = "remove"
val baseUrl = "https://$ip$port/$endpoint"
val JSON = MediaType.parse("application/json; charset=utf-8")

//https://casadiso.ddns.net:3000/points-of-interest/
fun getPoiFromFriend(friend:String): JSONObject {
    println("IN GET POI")
    val url = URL(baseUrl + "?friend=" + URLEncoder.encode(friend, "UTF-8"))
    var result = JSONObject()
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()

    val request = Request.Builder()
        .url(url)
        .build()
    val response = client.newCall(request).execute()
    if (response.isSuccessful){
        result = JSONObject(response.body()?.string()!!)
        println("Get poi from friend is success")
    }
    else{
        println("Get poi from friend is error")
        println(response.message())
    }
    return result
}
//https://casadiso.ddns.net:3000/points-of-interest/add/
fun addPOI(poiToAdd:String): String{
    println("IN GET POI")

    val body: RequestBody = RequestBody.create(JSON, poiToAdd)

    val url = URL(baseUrl + addPOIUrl)
    var result = ""
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
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
fun removePOI(poiId:String) {
    println("IN GET POI")

    val formBody: RequestBody = FormBody.Builder()
        .add("poiId", poiId)
        .build()

    val url = URL(baseUrl + removePOIUrl)
    val client = OkHttpClient().newBuilder().sslSocketFactory(sslContext.socketFactory,trustManager).hostnameVerifier(hostnameVerifier).build()
    val request = Request.Builder()
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