package com.example.maptry.utils

import android.util.Log
import com.example.maptry.activity.MapsActivity
import com.example.maptry.activity.MapsActivity.Companion.drawed
import com.example.maptry.activity.MapsActivity.Companion.friendsList
import com.example.maptry.api.RetrofitInstances
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/*Start Database Function*/

// retrieve friends collection from Server
fun createFriendList(id:String){
    Log.v("DatabaseUtils", "createFriendList")
    CoroutineScope(Dispatchers.IO).launch {
        val response = try {
            RetrofitInstances.friendsApi.getFriends(id)
        } catch (e: IOException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        } catch (e: HttpException) {
            e?.message?.let { it1 -> Log.e(MapsActivity.TAG, it1) }
            return@launch
        }

        if(response.isSuccessful && response.body() != null) {
            friendsList.clear()
            friendsList.addAll(response.body()!!)
        }
    }
}

// retrieve poi collection from Firebase
fun createPoiList(id:String){
    Log.v("DatabaseUtils", "createPoiList")
    CoroutineScope(Dispatchers.IO).launch {
        val response = try {
            RetrofitInstances.pointOfInterestsApi.getPointsOfInterest(id)
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
    Log.v("DatabaseUtils", "createLiveList")
    CoroutineScope(Dispatchers.IO).launch {
        val response = try {
            RetrofitInstances.liveEventsApi.getLiveEvents(id)
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
