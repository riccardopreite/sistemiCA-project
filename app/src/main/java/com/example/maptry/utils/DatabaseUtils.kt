package com.example.maptry.utils

import android.util.Log
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.drawed
import com.example.maptry.activity.MapsActivity.Companion.friendJson
import com.example.maptry.api.Retrofit
import com.example.maptry.server.getFriend
//import com.example.maptry.server.getLivePoi
//import com.example.maptry.server.getPoi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

/*Start Database Function*/

// retrieve friends collection from Server
fun createFriendList(id:String){
    var count = 0
    friendJson = JSONObject()
    val userFriend = getFriend(id)
    if (userFriend.length() > 0){
        val userKeys = userFriend.keys()
        println("AMICO")

        println(userFriend)
        userKeys.forEach{ key ->
            val tempJson = userFriend.get(key) as JSONObject
            println(tempJson.get("friendUsername"))
            friendJson.put(count.toString(),tempJson.get("friendUsername"))
            count++
        }
    }


}

// retrieve poi collection from Firebase
fun createPoiList(id:String){
    CoroutineScope(Dispatchers.IO).launch {
        val response = try {
            Retrofit.pointOfInterestsApi.getPointsOfInterest(id)
        } catch (e: IOException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        } catch (e: HttpException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        }

        if(response.isSuccessful && response.body() != null) {
            val pois = response.body()!!
            pois.forEach {
                createUserMarker(it)
            }
        }
    }
}

// retrieve live collection from Firebase
fun createLiveList(id:String){
    CoroutineScope(Dispatchers.IO).launch {
        val response = try {
            Retrofit.liveEventsApi.getLiveEvents(id)
        } catch (e: IOException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        } catch (e: HttpException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        }

        if(response.isSuccessful && response.body() != null) {
            val liveEvents = response.body()!!
            liveEvents.forEach {
                createLiveMarker(it)
            }
        }
    }
    drawed = true
}


/*End Database Function*/
