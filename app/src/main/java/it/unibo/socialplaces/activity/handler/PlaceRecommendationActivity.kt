package it.unibo.socialplaces.activity.handler

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import it.unibo.socialplaces.R
import it.unibo.socialplaces.activity.MainActivity
import it.unibo.socialplaces.model.pointofinterests.PointOfInterest

class PlaceRecommendationActivity: AppCompatActivity() {
    companion object {
        private val TAG: String = PlaceRecommendationActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        val recommendedPlace = PointOfInterest(
            intent.getStringExtra("markId")!!,
            intent.getStringExtra("address")!!,
            intent.getStringExtra("type")!!,
            intent.getStringExtra("latitude")!!.toDouble(),
            intent.getStringExtra("longitude")!!.toDouble(),
            intent.getStringExtra("name")!!,
            intent.getStringExtra("phoneNumber")!!,
            intent.getStringExtra("visibility")!!,
            intent.getStringExtra("url")!!
        )

        Log.i(TAG,"Received recommendation for point of interest: $recommendedPlace.")
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            action =
                when(intent.action){
                    getString(R.string.notification_validity_recommendation) -> getString(R.string.activity_place_validity_recommendation)
                    getString(R.string.notification_place_recommendation) -> getString(R.string.activity_place_place_recommendation)
                    else -> ""
                }
            putExtra("place", recommendedPlace)
            putExtra("notification", true)
        }

        startActivity(notificationIntent)
        finish()
    }

}