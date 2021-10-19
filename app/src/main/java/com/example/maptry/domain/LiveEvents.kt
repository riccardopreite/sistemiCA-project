package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import retrofit2.HttpException
import java.io.IOException

object LiveEvents {
    private const val TAG = "domain.LiveEvents"

    private val api by lazy {
        RetrofitInstances.liveEventsApi
    }

    private lateinit var userId: String

    private val liveEvents: MutableList<LiveEvent> = emptyList<LiveEvent>().toMutableList()

    private lateinit var createMarker: (Double,Double,String,String,String,Boolean) -> Unit
    private lateinit var updateList: () -> Unit
    private var validCreateMarkerCallback: Boolean = false
    private var validUpdateListCallback: Boolean = false

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    fun setCreateMarkerCallback(createMarker:(Double,Double,String,String,String,Boolean) -> Unit){
        this.createMarker = createMarker
        validCreateMarkerCallback = true
    }

    fun disableCallback() {
        validCreateMarkerCallback = false
        validUpdateListCallback = false
    }

    fun setUpdateLiveCallback(updatePoi:() -> Unit){
        this.updateList = updatePoi
        validUpdateListCallback = true
    }

    suspend fun getLiveEvents(forceSync: Boolean = false): List<LiveEvent> {
        Log.v(TAG, "getLiveEvents")
        if(!forceSync) {
            return liveEvents
        }

        val response = try {
            api.getLiveEvents(userId)
        } catch (e: IOException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty live events list.")
            }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let {
                Log.e(TAG, it)
                Log.e(TAG, "Returning empty live events list.")
            }
            return emptyList()
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Found ${response.body()!!.size} live events.")
            liveEvents.clear()
            liveEvents.addAll(response.body()!!)
            return liveEvents
        } else {
            Log.e(TAG, response.errorBody().toString())
        }

        return emptyList()
    }

    suspend fun addLiveEvent(addLiveEvent: AddLiveEvent): String {
        Log.v(TAG, "addLiveEvent")
        val response = try {
            if(addLiveEvent.owner == "") {
                api.addLiveEvent(addLiveEvent.copy(owner = userId))
            } else {
                api.addLiveEvent(addLiveEvent)
            }
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, it) }
            return ""
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, it) }
            return ""
        }

        if(response.isSuccessful && response.body() != null) {
            Log.i(TAG, "Live events successfully added.")
            val markId = response.body()!!
            val currentLive = LiveEvent(
                markId,
                addLiveEvent.address,
                addLiveEvent.latitude,
                addLiveEvent.longitude,
                addLiveEvent.name,
                addLiveEvent.owner,
                addLiveEvent.expiresAfter.toLong()
            )
            addLiveEventLocally(currentLive)
            if(validCreateMarkerCallback) {
                createMarker(
                    addLiveEvent.latitude,
                    addLiveEvent.longitude,
                    addLiveEvent.name,
                    addLiveEvent.address,
                    markId,
                    true
                )
            }
            if(validUpdateListCallback) {
               updateList()
            }

            return markId
        } else {
            Log.e(TAG, response.errorBody().toString())
        }
        return ""
    }

    private fun addLiveEventLocally(liveEvent: LiveEvent) {
        Log.v(TAG, "addLiveEventLocally")

        liveEvents.add(liveEvent)
        liveEvents.sortBy { it.name }
    }
}