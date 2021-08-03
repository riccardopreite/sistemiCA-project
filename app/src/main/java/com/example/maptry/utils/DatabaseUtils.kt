package com.example.maptry.utils

import com.example.maptry.activity.MapsActivity.Companion.drawed
import com.example.maptry.activity.MapsActivity.Companion.friendJson
import com.example.maptry.server.getFriend
import com.example.maptry.server.getLivePoi
import com.example.maptry.server.getPoi
import org.json.JSONObject

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

    val userMarker = getPoi(id)
    if (userMarker.length() > 0) {
        val userKeys = userMarker.keys()
        userKeys.forEach { key ->
            val tempJson = userMarker.get(key) as JSONObject
            createUserMarker(tempJson)
        }
    }
}

// retrieve live collection from Firebase
fun createLiveList(id:String){
    val userLive = getLivePoi(id)
    val userKeys = userLive.keys()
    if (userLive.length() > 0) {
        userKeys.forEach { key ->
            val tempJson = userLive.get(key) as JSONObject
            createLiveMarker(tempJson)
        }
    }
    drawed = true
}


/*End Database Function*/
