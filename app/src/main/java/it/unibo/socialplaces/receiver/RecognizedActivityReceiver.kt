package it.unibo.socialplaces.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.Recommendation
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import it.unibo.socialplaces.model.recommendation.ValidationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class RecognizedActivityReceiver(
    val removeActivityUpdates: () -> Unit,
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

        val humanActivity = intent.extras!!.getString("human_activity_type", "")
        val confidence = intent.extras!!.getInt("confidence", 0)

        if (humanActivity == "" || confidence < 75) {
            return
        }

        // Accessing the last available location.
        val sharedPrefLocation = context.getSharedPreferences(context.getString(R.string.sharedpreferences_location_updates), Context.MODE_PRIVATE)?: return
        // Using 200.0F as default value since it is impossible for both latitude and longitude.
        val latitude = sharedPrefLocation.getFloat("latitude", 200.0F).toDouble()
        val longitude = sharedPrefLocation.getFloat("longitude", 200.0F).toDouble()

        if(latitude == 200.0 || longitude == 200.0){
            Log.e(TAG,"Error retrieving the location from Shared Preferences!")
            return
        }

        val secondsInDay =
            (Calendar.HOUR_OF_DAY * 3600) + (Calendar.MINUTE * 60) + (Calendar.SECOND)
        val weekDay = dayDict[Calendar.DAY_OF_WEEK]!!

        val recommendation = intent.extras!!.getString("recommendation")!!

        val sharePreferenceFile = when(recommendation) {
            context.getString(R.string.recommendation_periodic_alarm) -> context.getString(R.string.sharedpreferences_place_recommendation)
            context.getString(R.string.recommendation_geofence_enter) -> context.getString(R.string.sharedpreferences_validity_recommendation)
            else -> ""
        }

        if(sharePreferenceFile == "") {
            return
        }

        val sharedPrefAlarmOrGeofence = context.getSharedPreferences(sharePreferenceFile, Context.MODE_PRIVATE)?: return

        with (sharedPrefAlarmOrGeofence.edit()) {
            putFloat("latitude", latitude.toFloat())
            putFloat("longitude", longitude.toFloat())
            putString("humanActivity", humanActivity)
            putInt("secondsInDay", secondsInDay)
            putInt("weekDay", weekDay)
            apply()
        }

        CoroutineScope(Dispatchers.IO).launch {
            when(recommendation) {
                context.getString(R.string.recommendation_periodic_alarm) -> {
                    Log.d(TAG,"Asking for place recommendation...")
                    val placeRequest = PlaceRequest(
                        latitude = latitude,
                        longitude = longitude,
                        human_activity = humanActivity,
                        seconds_in_day = secondsInDay,
                        week_day = weekDay,
                    )
                    Recommendation.recommendPlace(placeRequest)
                }
                context.getString(R.string.recommendation_geofence_enter) -> {
                    Log.d(TAG,"Asking for place recommendation of a specific category...")
                    val placeCategory = intent.getStringExtra("place_category") ?: ""
                    if(placeCategory == "") {
                        return@launch
                    }
                    val validityRequest = ValidationRequest(
                        latitude = latitude,
                        longitude = longitude,
                        human_activity = humanActivity,
                        seconds_in_day = secondsInDay,
                        week_day = weekDay,
                        place_category = placeCategory
                    )
                    Recommendation.validityPlace(validityRequest)
                }
            }

            removeActivityUpdates()
            unregisterReceiver(context)
        }
    }
}