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

class RecognizedActivityReceiver(
    val removeActivityUpdates: () -> Unit,
    val unregisterReceiver: (Context) -> Unit
) : BroadcastReceiver() {
    private val TAG = RecognizedActivityReceiver::class.qualifiedName!!

    private val dayDict = mapOf(
        Calendar.MONDAY to 0,
        Calendar.TUESDAY to 1,
        Calendar.WEDNESDAY to 2,
        Calendar.THURSDAY to 3,
        Calendar.FRIDAY to 4,
        Calendar.SATURDAY to 5,
        Calendar.SUNDAY to 6
    )

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onReceive(context: Context, intent: Intent) {
        Log.v(TAG, "onReceive")
        val humanActivity = intent.extras!!.get("type") as String
        val confidence = intent.extras!!.get("confidence") as Int

        if (humanActivity == "" || confidence < 75) {
            return
        }

        val secondsInDay =
            (Calendar.HOUR_OF_DAY * 3600) + (Calendar.MINUTE * 60) + (Calendar.SECOND)
        val weekDay = dayDict[Calendar.DAY_OF_WEEK]!!

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