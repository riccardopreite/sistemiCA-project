package com.example.maptry.domain

import android.util.Log
import com.example.maptry.api.ApiError
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.model.liveevents.AddLiveEvent
import com.example.maptry.model.liveevents.LiveEvent
import com.example.maptry.model.pointofinterests.PointOfInterest
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
    private lateinit var updatePoi: () -> Unit
    private var validCallback: Boolean = false
    private var validPoiCallback: Boolean = false

    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    fun setCreateMarkerCallback(createMarker:(Double,Double,String,String,String,Boolean) -> Unit){
        this.createMarker = createMarker
        validCallback = true
    }

    fun disableCallback() {
        validCallback = false
        validPoiCallback = false
    }

    fun setUpdateLiveCallback(updatePoi:() -> Unit){
        this.updatePoi = updatePoi
        validPoiCallback = true
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
            Log.e(TAG, (response.errorBody() as ApiError).message)
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
            if(validCallback) {
                createMarker(
                    addLiveEvent.latitude,
                    addLiveEvent.longitude,
                    addLiveEvent.name,
                    addLiveEvent.address,
                    markId,
                    true
                )
            }
            if(validPoiCallback){
               updatePoi()
            }
            liveEvents.add(currentLive)

            return markId
        } else {
            Log.e(TAG, (response.errorBody() as ApiError).message)
        }
        return ""
    }
}