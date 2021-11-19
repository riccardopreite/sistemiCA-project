package it.unibo.socialplaces.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.domain.Recommendation
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class RecognizedActivityReceiver(val removeActivityUpdates: () -> Unit,
    val unregisterReceiver: (Context) -> Unit
) : BroadcastReceiver() {
    companion object {
        private val TAG = RecognizedActivityReceiver::class.qualifiedName!!
    }

    /**
     * Map for converting calendar days to integers.
     */
    private val dayDict = mapOf(
        Calendar.MONDAY to 0,
        Calendar.TUESDAY to 1,
        Calendar.WEDNESDAY to 2,
        Calendar.THURSDAY to 3,
        Calendar.FRIDAY to 4,
        Calendar.SATURDAY to 5,
        Calendar.SUNDAY to 6
    )

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(TAG, "onReceive")
        // TODO Converted generic get() as Type with getType(). Check!
        val humanActivity = intent.extras!!.getString("type", "")
        val confidence = intent.extras!!.getInt("confidence", 0)

        if (humanActivity == "" || confidence < 75) {
            return
        }

        val secondsInDay =
            (Calendar.HOUR_OF_DAY * 3600) + (Calendar.MINUTE * 60) + (Calendar.SECOND)
        val weekDay = dayDict[Calendar.DAY_OF_WEEK]!!

        // Accessing the last available location.
        val sharedPref = context.getSharedPreferences("sharePlaces",Context.MODE_PRIVATE)?: return
        // Using 200.0F as default value since it is impossible for both latitude and longitude.
        val latitude = sharedPref.getFloat("latitude", 200.0F).toDouble()
        val longitude = sharedPref.getFloat("longitude", 200.0F).toDouble()

        if(latitude >= 200 || longitude >= 200){
            return
        }

        val placeRequest = PlaceRequest(
            Auth.getUsername()!!,
            latitude,
            longitude,
            humanActivity,
            secondsInDay,
            weekDay
        )

        CoroutineScope(Dispatchers.IO).launch {
            Recommendation.recommendPlace(placeRequest)
            removeActivityUpdates()
            unregisterReceiver(context)
        }
    }
}