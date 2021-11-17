package it.unibo.socialplaces.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import it.unibo.socialplaces.R
import it.unibo.socialplaces.api.RetrofitInstances
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity(R.layout.activity_splash) {
    companion object {
        private val TAG = SplashActivity::class.qualifiedName
    }

    /**
     * Replaces [onActivityResult] call after [startActivityForResult].
     * Handles the behavior of the application after the login.
     */
    private val loginActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        RetrofitInstances.loadStore(resources.openRawResource(R.raw.mystore))

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE

        Auth.loadAuthenticationManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            if(Auth.isUserAuthenticated()) {
                val username = Auth.getUsername()
                username?.let {
                    Notification.setUserId(it)
                    Recommendation.setUserId(it)
                    Friends.setUserId(it)
                    LiveEvents.setUserId(it)
                    PointsOfInterest.setUserId(it)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    progressBar.visibility = View.GONE
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    progressBar.visibility = View.GONE
                    loginActivityLauncher.launch(Intent(this@SplashActivity, LoginActivity::class.java))
                }
            }
        }
    }
}