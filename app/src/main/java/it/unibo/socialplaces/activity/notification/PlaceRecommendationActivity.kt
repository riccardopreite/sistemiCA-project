package it.unibo.socialplaces.activity.notification

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.model.PointOfInterest
import it.unibo.socialplaces.R
import it.unibo.socialplaces.config.PushNotification.getManager

class PlaceRecommendationActivity: AppCompatActivity(R.layout.activity_main) {
    companion object {
        private val TAG: String = PlaceRecommendationActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        val extras = intent.extras!!
        val poi = extras.get("place") as PointOfInterest
        val notificationId = extras.getInt("notificationId")
        println(poi)
        getManager().cancel(notificationId)
        Log.v(TAG,"Here show poi on map")
    }
    //Shows poi on map
}