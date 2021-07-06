package com.example.maptry

import android.util.Log
import com.example.maptry.MapsActivity.Companion.dataFromfirestore
import com.example.maptry.MapsActivity.Companion.db
import com.example.maptry.MapsActivity.Companion.drawed
import com.example.maptry.MapsActivity.Companion.friendJson
import com.example.maptry.MapsActivity.Companion.mMap
import com.example.maptry.MapsActivity.Companion.myCar
import com.example.maptry.MapsActivity.Companion.myList
import com.example.maptry.MapsActivity.Companion.myLive
import com.example.maptry.MapsActivity.Companion.myjson
import com.example.maptry.MapsActivity.Companion.mymarker
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONObject

/*Start Database Function*/

/*These function create and object from a Data Class and upload it to Firebase*/
fun writeNewPOI(userId: String, name:String,addr:String,cont:String,type:String,marker:Marker,url:String,phone:String) {
    val user = UserMarker(name,addr,cont,type,marker.position.latitude.toString(),marker.position.longitude.toString(),url,phone)
    db.collection("user").document(userId).collection("marker").add(user).addOnSuccessListener {
        Log.d("TAG", "success")
    }
        .addOnFailureListener { ex : Exception ->
            Log.d("TAG", ex.toString())

        }
}

fun writeNewLive(userId: String, name:String,addr:String,timer:String,owner:String,marker:Marker,url:String,phone:String,type: String,cont: String) {
    val user = UserLive(name,addr,timer,owner,marker.position.latitude.toString(),marker.position.longitude.toString(),url,phone,type,cont)
    db.collection("user").document(userId).collection("living").add(user).addOnSuccessListener {
        Log.d("TAG", "success")
    }
        .addOnFailureListener { ex : Exception ->
            Log.d("TAG", ex.toString())

        }
}

fun writeNewCar(userId: String, name:String,addr:String,timer:String,owner:String,marker:Marker,url:String,phone:String,type: String,cont: String) {
    val user = UserCar(name,addr,timer,owner,marker.position.latitude.toString(),marker.position.longitude.toString(),url,phone,type,cont)
    db.collection("user").document(userId).collection("car").add(user).addOnSuccessListener {
        Log.d("TAG", "success")
    }
        .addOnFailureListener { ex : Exception ->
            Log.d("TAG", ex.toString())

        }
}

// retrieve friends collection from Firebase
fun createFriendList(id:String){
    var count = 0
    db.collection("user").document(id).collection("friend")
        .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                Log.w("TAG", "Listen failed.", firebaseFirestoreException)
                return@addSnapshotListener
            }
            if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                dataFromfirestore = querySnapshot.documents
                friendJson = JSONObject()
                querySnapshot.documents.forEach { child ->
                    child.data?.forEach { chi ->
                        friendJson.put(count.toString(),chi.value)
                        count++
                    }
                }
            }
        }
}

// retrieve poi collection from Firebase
fun createPoiList(id:String){
    db.collection("user").document(id).collection("marker")
        .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                Log.w("TAG", "Listen failed.", firebaseFirestoreException)
            }
            if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                dataFromfirestore = querySnapshot.documents
                querySnapshot.documents.forEach { child ->
                    myjson = JSONObject()
                    child.data?.forEach     { chi ->
                        myjson.put(chi.key, chi.value)
                    }
                    var pos: LatLng = LatLng(
                        myjson.getString("lat").toDouble(),
                        myjson.getString("lon").toDouble()
                    )
                    var mark = createMarker(pos)
                    mymarker.put(pos.toString(), mark)
                    myList.put(pos.toString(), myjson)
                            mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                }
            }
            drawed = true
        }
}

// retrieve car collection from Firebase
fun createCarList(id:String){
    db.collection("user").document(id).collection("car")
        .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                Log.w("TAG", "Listen failed.", firebaseFirestoreException)
            }
            if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                dataFromfirestore = querySnapshot.documents
                querySnapshot.documents.forEach { child ->
                    myjson = JSONObject()
                    child.data?.forEach { chi ->
                        myjson.put(chi.key, chi.value)
                    }
                    var pos: LatLng = LatLng(
                        myjson.getString("lat").toDouble(),
                        myjson.getString("lon").toDouble()
                    )
                    var mark = createMarker(pos)
                    mymarker.put(pos.toString(), mark)
                    myCar.put(pos.toString(), myjson)
                    myList.put(pos.toString(), myjson)
                    mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                }
            }
            drawed = true
        }
}

// retrieve live collection from Firebase
fun createLiveList(id:String){
    db.collection("user").document(id).collection("living")
        .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                Log.w("TAG", "Listen failed.", firebaseFirestoreException)
            }
            if (querySnapshot != null && querySnapshot.documents.isNotEmpty()) {
                dataFromfirestore = querySnapshot.documents
                querySnapshot.documents.forEach { child ->
                    myjson = JSONObject()
                    child.data?.forEach { chi ->
                        myjson.put(chi.key, chi.value)
                    }
                    var pos: LatLng = LatLng(
                        myjson.getString("lat").toDouble(),
                        myjson.getString("lon").toDouble()
                    )
                    var mark = createMarker(pos)
                    mymarker.put(pos.toString(), mark)
                    myLive.put(pos.toString(), myjson)
                    myList.put(pos.toString(), myjson)
                    mark?.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                }
            }
            drawed = true
        }
}

/*End Database Function*/
