package it.unibo.socialplaces.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.ActivityRecognitionClient
import it.unibo.socialplaces.R
import it.unibo.socialplaces.receiver.RecognizedActivityReceiver
import it.unibo.socialplaces.receiver.RecommendationAlarm
import it.unibo.socialplaces.service.RecognizedActivity

class PeriodicRecommendationWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {
    /**
     * The application context
     */
    val context = context

    /**
     * The parameters received in input to the worker.
     */
    val workerParameters = workerParameters

    companion object {
        private val TAG: String = RecommendationAlarm::class.qualifiedName!!
    }

    /**
     * Variable for requesting human activities.
     */
    private lateinit var harClient: ActivityRecognitionClient
    private lateinit var recommendPendingIntent: PendingIntent
    private lateinit var recognizedActivityReceiver: RecognizedActivityReceiver

    override fun doWork(): Result {
        Log.d(TAG, "Alarm just fired")

        initHARClientAndIntents(context)
        registerReceiver(context)

        setupHumanActivityUpdates()

        return Result.success()
    }

    /**
     * Method to init Class field and set them
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initHARClientAndIntents(context: Context) {
        // Init HAR client
        if(!this::harClient.isInitialized) {
            harClient = ActivityRecognitionClient(context)
        }

        // Init pending intent
        if(!this::recommendPendingIntent.isInitialized) {
            recommendPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                Intent(context, RecognizedActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    /**
     *
     * Register and Unregister broadcast receiver for human activity
     */
    private fun registerReceiver(context: Context) {
        recognizedActivityReceiver = RecognizedActivityReceiver(
            this::removeHumanActivityUpdates,
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
     * Sets up HAR updates.
     */
    private fun setupHumanActivityUpdates() {
        harClient.requestActivityUpdates(
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

    /**
     * Set and remove human activity updates.
     * Is a work around because the only way is to ask periodically so after get the first response we remove it
     *
     */

    private fun removeHumanActivityUpdates(){
        harClient.removeActivityUpdates(
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


}