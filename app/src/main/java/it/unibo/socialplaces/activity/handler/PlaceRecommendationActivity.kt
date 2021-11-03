package it.unibo.socialplaces.activity.handler

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.PushNotification

class PlaceRecommendationActivity: AppCompatActivity(R.layout.activity_main) {
    companion object {
        private val TAG: String = PlaceRecommendationActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        val extras = intent.extras!!
        val notificationId = extras.getInt("notificationId")
        val poi = extras.getParcelable<PointOfInterest>("place")
        PushNotification.notificationManager.cancel(notificationId)

        Log.v(TAG,"A new point of interest has been published: $poi.")
    }
    //Shows poi on map
}