package it.unibo.socialplaces.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import it.unibo.socialplaces.R
import it.unibo.socialplaces.domain.PointsOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private val TAG = GeofenceBroadcastReceiver::class.qualifiedName!!
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.v(TAG, "onReceive")
        if(intent == null) {
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeredGeofence = geofencingEvent.triggeringGeofences[0]
            Log.d(TAG, "GEOFENCE TRANSITION ENTER TO PointOfInterest with markId=${triggeredGeofence.requestId}")

            CoroutineScope(Dispatchers.IO).launch {
                val poisList = PointsOfInterest.getPointsOfInterest()
                val pois = poisList.filter { poi -> poi.markId ==  triggeredGeofence.requestId }
                if (pois.isEmpty()) {
                    return@launch
                }
                val poi = pois[0]
                val geofenceRecommendationIntent = Intent(context, RecommendationAlarm::class.java).apply {
                    action = context?.getString(R.string.recommendation_geofence_enter)
                    putExtra("place_category", poi.type)
                }

                context?.sendBroadcast(geofenceRecommendationIntent)
                Log.i(TAG, "Entered in the geofence of: " + poi.name)
            }


        }
    }
}