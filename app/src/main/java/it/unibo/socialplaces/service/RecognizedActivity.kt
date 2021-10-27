package it.unibo.socialplaces.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity.*

class RecognizedActivity : IntentService(TAG) {
    companion object {
        private val TAG = RecognizedActivity::class.qualifiedName
        private const val INTENT_ACTION: String = "activity_recognition"
    }

    private var acceptedActivity = mapOf(
        WALKING to "walk",
        STILL to "still",
        IN_VEHICLE to "car"
    )

    /**
     * Method that receive the request to find the HA
     */

    override fun onHandleIntent(receivedIntent: Intent?) {
        Log.v(TAG, "onHandleIntent")
        val recognitionResult = ActivityRecognitionResult.extractResult(receivedIntent!!)
        val detectedActivity = recognitionResult!!.mostProbableActivity
        Log.d(TAG,"Detected human activity: ${detectedActivity.type} (confidence = ${detectedActivity.confidence}) ")

        val sendHaIntent = Intent(INTENT_ACTION).apply {
            putExtra("type", acceptedActivity[detectedActivity.type])
            putExtra("confidence", detectedActivity.confidence)
        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(sendHaIntent)
    }
}
