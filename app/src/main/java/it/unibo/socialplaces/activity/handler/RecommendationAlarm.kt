package it.unibo.socialplaces.activity.handler

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.ActivityRecognitionClient
import it.unibo.socialplaces.domain.Recommendation.recommendPlace
import it.unibo.socialplaces.model.recommendation.PlaceRequest
import it.unibo.socialplaces.service.RecognizedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class RecommendationAlarm : BroadcastReceiver() {
    companion object {
        private val TAG: String = RecommendationAlarm::class.qualifiedName!!
        private const val INTENT_ACTION: String = "activity_recognition"
    }

    /**
     *
     * Variable for PlaceRequest data class
     */
    private var seconds_in_day: Int = 0
    private var week_day: Int = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var human_activity: String
    private var dayDict =  hashMapOf<Int, Int>()

    /**
     *
     * Variable for reqest human activity
     */
    private lateinit var activityRecognitionClient: ActivityRecognitionClient
    private lateinit var recommendIntent: Intent
    private lateinit var recommendPendingIntent: PendingIntent


    /**
     *
     * Method called every time the AlarmManager fire this event
     */
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm just fired")

        checkAlarmField(context)
        registerReceiver(context)

        setActivityUpdates()

    }
    /**
     *
     * BroadcastReceiver to receive human activity answer
     */

    private val mMessageReceiver: BroadcastReceiver = object :  BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            human_activity = intent.extras?.get("type") as String
            val confidence = intent.extras?.get("confidence") as Int

            Log.d(TAG, "Got activity: $human_activity")

            if(human_activity != "" && confidence > 75){
                setTime()
                val place = PlaceRequest (
                    "",
                    latitude,
                    longitude,
                    human_activity,
                    seconds_in_day,
                    week_day
                )


                CoroutineScope(Dispatchers.IO).launch {
                    recommendPlace(place)
                    removeActivityUpdates()
                    unRegisterReceiver(context)
                }
            }
        }
    }



    /**
     *
     * Method to init Class field and set them
     */

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun checkAlarmField(context: Context){
        if (dayDict.isEmpty())
            setDict()

        if(!this::recommendIntent.isInitialized)
            recommendIntent = Intent(context.applicationContext, RecognizedActivity::class.java)

        if(!this::recommendPendingIntent.isInitialized)
            recommendPendingIntent = PendingIntent.getService(
                context.applicationContext,
                1,
                recommendIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        if(!this::activityRecognitionClient.isInitialized)
            activityRecognitionClient = ActivityRecognitionClient(context.applicationContext)
    }

    private fun setDict(){
        dayDict[Calendar.MONDAY]    = 0
        dayDict[Calendar.TUESDAY]   = 1
        dayDict[Calendar.WEDNESDAY] = 2
        dayDict[Calendar.THURSDAY]  = 3
        dayDict[Calendar.FRIDAY]    = 4
        dayDict[Calendar.SATURDAY]  = 5
        dayDict[Calendar.SUNDAY]    = 6
    }

    private fun setTime(){
        seconds_in_day = (Calendar.HOUR_OF_DAY * 3600) + (Calendar.MINUTE * 60) + (Calendar.SECOND)
        week_day = dayDict[Calendar.DAY_OF_WEEK]!!
    }

    /**
     *
     * Register and Unregister broadcast reciver for human activity
     */


    private fun registerReceiver(context: Context){
        LocalBroadcastManager
            .getInstance(context.applicationContext)
            .registerReceiver(mMessageReceiver,IntentFilter(INTENT_ACTION))
    }

    private fun unRegisterReceiver(context: Context){
        LocalBroadcastManager
            .getInstance(context.applicationContext)
            .unregisterReceiver(mMessageReceiver)
    }

    /**
     * Set and remove human activity updates.
     * Is a work around because the only way is to ask periodically so after get the first response we remove it
     *
     */

    private fun removeActivityUpdates(){
        val removeActivityUpdates = activityRecognitionClient.removeActivityUpdates(
            recommendPendingIntent
        )
        removeActivityUpdates.addOnSuccessListener {
            Log.d(TAG,"Removed activity updates successfully!")
        }
        removeActivityUpdates.addOnFailureListener{
            Log.e(TAG,"Failed to remove activity updates!")
        }
    }

    private fun setActivityUpdates(){
        val activityUpdates = activityRecognitionClient.requestActivityUpdates(
            Long.MAX_VALUE,
            recommendPendingIntent
        )

        activityUpdates.addOnSuccessListener {
            Log.v(TAG, "Start Activity Updates Success")
        }

        activityUpdates.addOnFailureListener {
            Log.e(TAG, "Start Activity Updates Fail")
        }
    }
}