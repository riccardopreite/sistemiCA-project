package it.unibo.socialplaces.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionClient
import it.unibo.socialplaces.R
import it.unibo.socialplaces.service.RecognizedActivity

class RecommendationAlarm : BroadcastReceiver() {
    companion object {
        private val TAG: String = RecommendationAlarm::class.qualifiedName!!
    }

    /**
     *
     * Variable for request human activity
     */
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var recommendPendingIntent: PendingIntent
    private lateinit var recognizedActivityReceiver: RecognizedActivityReceiver


    /**
     *
     * Method called every time the AlarmManager fire this event
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        val placeCategory = intent.getStringExtra("place_category") ?: ""

        initARClientAndIntents(context.applicationContext, intent.action!!, placeCategory)
        registerReceiver(context.applicationContext)

        setActivityUpdates()
    }

    /**
     * Method to init Class field and set them
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initARClientAndIntents(context: Context, action: String, category: String) {
        if(!this::recommendPendingIntent.isInitialized) {
            val activityRecognitionIntent = Intent(context, RecognizedActivity::class.java).apply {
                this.action = action
                putExtra("place_category", category)
            }
            recommendPendingIntent = PendingIntent.getService(
                context,
                0,
                activityRecognitionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        if(!this::activityRecognitionClient.isInitialized) {
            activityRecognitionClient = ActivityRecognitionClient(context)
        }
    }



    /**
     *
     * Register and Unregister broadcast reciver for human activity
     */
    private fun registerReceiver(context: Context) {
        recognizedActivityReceiver = RecognizedActivityReceiver(
            this::removeActivityUpdates,
            this::unregisterReceiver
        )

        LocalBroadcastManager
            .getInstance(context)
            .registerReceiver(
                recognizedActivityReceiver,
                IntentFilter(context.getString(R.string.recognized_ha))
            )
    }

    private fun unregisterReceiver(context: Context){
        LocalBroadcastManager
            .getInstance(context)
            .unregisterReceiver(recognizedActivityReceiver)
    }

    /**
     * Set and remove human activity updates.
     * Is a work around because the only way is to ask periodically so after get the first response we remove it
     *
     */

    private fun removeActivityUpdates(){
        activityRecognitionClient.removeActivityUpdates(
            recommendPendingIntent
        ).apply {
            addOnSuccessListener {
                Log.d(TAG,"Removed human activity updates successfully!")
            }
            addOnFailureListener{
                Log.e(TAG,"Failed to remove human activity updates!")
            }
        }
    }

    private fun setActivityUpdates() {
        activityRecognitionClient.requestActivityUpdates(
            Long.MAX_VALUE,
            recommendPendingIntent
        ).apply {
            addOnSuccessListener {
                Log.v(TAG, "Started human activity updates successfully!")
            }
            addOnFailureListener {
                Log.e(TAG, "Failed to start human activity updates!")
            }
        }
    }
}