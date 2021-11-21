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

        val humanActivity = intent.extras!!.getString("type", "")
        val confidence = intent.extras!!.getInt("confidence", 0)

        if (humanActivity == "" || confidence < 75) {
            return
        }

        // Accessing the last available location.
        val sharedPrefLocation = context.getSharedPreferences(context.getString(R.string.location_preference),Context.MODE_PRIVATE)?: return
        // Using 200.0F as default value since it is impossible for both latitude and longitude.
        val latitude = sharedPrefLocation.getFloat("latitude", 200.0F).toDouble()
        val longitude = sharedPrefLocation.getFloat("longitude", 200.0F).toDouble()

        if(latitude >= 200 || longitude >= 200){
            Log.e(TAG,"Location error")
            return
        }

        val secondsInDay =
            (Calendar.HOUR_OF_DAY * 3600) + (Calendar.MINUTE * 60) + (Calendar.SECOND)
        val weekDay = dayDict[Calendar.DAY_OF_WEEK]!!

        val apiType = intent.extras!!.getString("apiType")!!

        val sharePreferenceFile =
            when(apiType){
                context.getString(R.string.alarm_recommendation) -> context.getString(R.string.recommendation_preference)
                context.getString(R.string.geofence_recommendation) -> context.getString(R.string.validity_preference)
                else -> ""
            }
        if(sharePreferenceFile == ""){
            return
        }


        val sharedPrefAlarmOrGeofence = context.getSharedPreferences(sharePreferenceFile,Context.MODE_PRIVATE)?: return

        with (sharedPrefAlarmOrGeofence.edit()) {
            putFloat("latitude", latitude.toFloat())
            putFloat("longitude", longitude.toFloat())
            putString("humanActivity", humanActivity)
            putInt("secondsInDay", secondsInDay)
            putInt("weekDay", weekDay)
            apply()
        }

        CoroutineScope(Dispatchers.IO).launch {
            when(apiType){
                context.getString(R.string.alarm_recommendation) -> {

                    val placeRequest = PlaceRequest(
                        latitude = latitude,
                        longitude = longitude,
                        human_activity = humanActivity,
                        seconds_in_day = secondsInDay,
                        week_day = weekDay,
                    )
                    Log.v(TAG,"Recommendation api")
                    Recommendation.recommendPlace(placeRequest)

                }
                context.getString(R.string.geofence_recommendation) -> {

                    val placeCategory = intent.getStringExtra("place_category") ?: ""
                    Log.v(TAG,"Place category is: $placeCategory")
                    if(placeCategory == ""){
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
                    Log.v(TAG,"Validation api")

                    Recommendation.validityPlace(validityRequest)
                }
            }

            removeActivityUpdates()
            unregisterReceiver(context)
        }
    }
}