package com.example.maptry.utils

import com.example.maptry.activity.MapsActivity.Companion.friendJson
import com.example.maptry.server.getFriend
import com.example.maptry.server.getLivePoi
import com.example.maptry.server.getLivePoiFromFriends
import com.example.maptry.server.getPoi
import org.json.JSONObject

/*Start Database Function*/

// retrieve friends collection from Server
fun createFriendList(id:String){
    var count = 0
    friendJson = JSONObject()
    val userFriend = getFriend(id)
    val userKeys = userFriend.keys()
    userKeys.forEach{ key ->
        val tempJson = userFriend.get(key) as JSONObject
        createUserMarker(tempJson)
        friendJson.put(count.toString(),tempJson.get("friend"))
        count++
    }
}

// retrieve poi collection from Firebase
fun createPoiList(id:String){

    val userMarker = getPoi(id)
    val userKeys = userMarker.keys()
    userKeys.forEach{ key ->
        val tempJson = userMarker.get(key) as JSONObject
        createUserMarker(tempJson)
    }
}

// retrieve live collection from Firebase
fun createLiveList(id:String){
    val userLive = getLivePoi(id)
    val userFriendLive = getLivePoiFromFriends(id)
    val userKeys = userLive.keys()
    val userFriendKeys = userFriendLive.keys()
    userKeys.forEach{ key ->
        val tempJson = userLive.get(key) as JSONObject
        createLiveMarker(tempJson)
    }
    userFriendKeys.forEach{ key ->
        val tempJson = userFriendLive.get(key) as JSONObject
        createLiveMarker(tempJson)
    }
}


/*End Database Function*/
