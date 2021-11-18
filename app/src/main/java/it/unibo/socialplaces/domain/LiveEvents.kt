package it.unibo.socialplaces.domain

import android.util.Log
import it.unibo.socialplaces.api.ApiError
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.model.liveevents.AddLiveEvent
import it.unibo.socialplaces.model.liveevents.LiveEvent
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

object LiveEvents {
    private val TAG = LiveEvents::class.qualifiedName!!

    private val api by lazy {
        ApiConnectors.liveEventsApi
    }

    private val handleApiError: (ResponseBody?) -> ApiError = ApiConnectors::handleApiError

    private lateinit var userId: String

    private val liveEvents: MutableList<LiveEvent> = emptyList<LiveEvent>().toMutableList()

    private lateinit var createMarker: (Double,Double,String,String,String,Boolean) -> Unit
    private var validCreateMarkerCallback: Boolean = false
    private lateinit var updateList: () -> Unit
    private var validUpdateListCallback: Boolean = false

    /**
     * Sets the default user (logged in user) for API calls.
     * @param user the logged in user.
     */
    fun setUserId(user: String) {
        Log.v(TAG, "setUserId")
        userId = user
    }

    /**
     * Sets a callback to be invoked every time [addLiveEvent] is called.
     * This callback will stop being invoked once [disableCallbacks] is invoked.
     */
    fun setCreateMarkerCallback(createMarker: (Double,Double,String,String,String,Boolean) -> Unit){
        this.createMarker = createMarker
        validCreateMarkerCallback = true
    }

    /**
     * Sets a callback to be invoked every time [addLiveEvent] is called.
     * This callback will stop being invoked once [disableCallbacks] is invoked.
     */
    fun setUpdateLiveCallback(updatePoi: () -> Unit){
        this.updateList = updatePoi
        validUpdateListCallback = true
    }

    /**
     * Disables the invocation of the callbacks set via [setCreateMarkerCallback] and
     * [setUpdateLiveCallback].
     */
    fun disableCallbacks() {
        validCreateMarkerCallback = false
        validUpdateListCallback = false
    }

    /**
     * Calls GET /live-events in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.LiveEventsApi.getLiveEvents]
     * @param forceSync when `true` actually calls the remote APIs, otherwise retrieves the cached data
     * (which could be empty).
     * @return the list of live events of the logged user and their friends.
     */
    suspend fun getLiveEvents(forceSync: Boolean = false): List<LiveEvent> {
        Log.v(TAG, "getLiveEvents")
        if(!forceSync) {
            return liveEvents
        }

        val response = try {
            api.getLiveEvents(userId)
        } catch (e: IOException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty live events list.") }
            return emptyList()
        } catch (e: HttpException) {
            e.message?.let { Log.e(TAG, "$it\nReturning empty live events list.") }
            return emptyList()
        }

        val body = response.body()
        if(response.isSuccessful && body != null) {
            Log.i(TAG, "Found ${body.size} live events.")
            liveEvents.clear()
            liveEvents.addAll(body)
            return liveEvents
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return emptyList()
    }

    /**
     * Calls POST /live-events/add in the SocialPlaces API.
     * @see [it.unibo.socialplaces.api.LiveEventsApi.addLiveEvent]
     * @param addLiveEvent the data for the live event to add.
     * @return the id of the added live event.
     */
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

        val addedLiveEvent = response.body()
        if(response.isSuccessful && addedLiveEvent != null) {
            Log.i(TAG, "Live events successfully added.")
            val currentLive = LiveEvent(
                addedLiveEvent.id,
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
                    addedLiveEvent.id,
                    true
                )
            }
            if(validUpdateListCallback) {
               updateList()
            }

            return addedLiveEvent.id
        } else {
            Log.e(TAG, handleApiError(response.errorBody()).toString())
        }

        return ""
    }

    /**
     * Adds a live event locally and sorts the list of live events.
     * @param liveEvent new live event to add (locally).
     */
    private fun addLiveEventLocally(liveEvent: LiveEvent) {
        Log.v(TAG, "addLiveEventLocally")

        liveEvents.add(liveEvent)
        liveEvents.sortBy { it.name }
    }
}