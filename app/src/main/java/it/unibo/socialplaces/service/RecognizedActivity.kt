package it.unibo.socialplaces.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.DetectedActivity.*
class RecognizedActivity : IntentService(TAG) {
    companion object {
        private val TAG = RecognizedActivity::class.java.name
        private const val INTENT_ACTION: String = "activity_recognition"
    }


    private lateinit var acceptedActivity: HashMap<Int,String>

    /**
     * Method that receive the request to find the HA
     */

    override fun onHandleIntent(receivedIntent: Intent?) {
        if(!this::acceptedActivity.isInitialized)
            setAcceptedActivity()
        val recognitionResult = ActivityRecognitionResult.extractResult(receivedIntent!!)
        val detectedActivity = recognitionResult!!.mostProbableActivity
        Log.v(TAG,"FOUND ACTIVITY on hangle "+detectedActivity.type)

        sendResponse(detectedActivity)

    }

    /**
     *
     * Method used to send response to the broadcast receiver in RecommendationAlarm
     */

    private fun sendResponse(activityDetected:DetectedActivity){
        val sendHAR = Intent(INTENT_ACTION)
        Log.v(TAG,"FOUND ACTIVITY "+activityDetected.type)
        sendHAR.putExtra("type", acceptedActivity[activityDetected.type])
        sendHAR.putExtra("confidence", activityDetected.confidence)
        LocalBroadcastManager.getInstance(this).sendBroadcast(sendHAR)

    }

    /**
     *
     * Init acceptedActivity
     */
    private fun setAcceptedActivity(){
        acceptedActivity = HashMap<Int,String>()
        acceptedActivity[WALKING] = "walk"
        acceptedActivity[STILL] = "still"
        acceptedActivity[IN_VEHICLE] = "car"

    }
}
