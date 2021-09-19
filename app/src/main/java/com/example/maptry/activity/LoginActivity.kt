package com.example.maptry.activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.newBundy
import com.example.maptry.config.Auth
import com.example.maptry.domain.Friends
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class LoginActivity : AppCompatActivity() {
    companion object {
        val TAG: String = LoginActivity::class.qualifiedName!!

        /**
         * Result code returned when the activity closes and the user is authenticated.
         */
        const val resultCodeSignedIn: Int = 200

        /**
         * Result code returned when the activity closes and the user is not authenticated.
         */
        const val resultCodeNotSignedIn: Int = 201

        /**
         * Request code to check whether the result returned by the activity comes from the Google Sign In intent.
         */
        const val requestCodeSignIn: Int = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Auth.loadAuthenticationManager(this)

        /**
         * First the user tries to login via Google through the Android system interface.
         * Then the user is logged in (perhaps even registered if they are not) onto Firebase.
         */
        CoroutineScope(Dispatchers.IO).launch {
            if(Auth.isUserAuthenticated()) {
                Log.i(TAG, "The user has already logged in.")
                setResult(resultCodeSignedIn, Intent().apply {
                    this.data = Uri.parse("signed-in") // this is referred to the Intent.
                })
                val username = Auth.getUsername()
                username?.let {
                    Friends.setUserId(username)
                    LiveEvents.setUserId(username)
                    PointsOfInterest.setUserId(username)
                }
                // Carica su tutte api
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    startActivityForResult(Auth.signInIntent(this@LoginActivity), requestCodeSignIn)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCodeSignIn) {
            Auth.loadSignedInAccountFromIntent(data!!)
            if(Auth.isUserAuthenticated()) {
                setResult(resultCodeSignedIn, Intent().apply {
                    this.data = Uri.parse("signed-in") // this is referred to the Intent.
                })
            } else {
                setResult(resultCodeNotSignedIn, Intent().apply {
                    this.data = Uri.parse("not-signed-in")
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onSaveInstanceState(newBundy)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle("newBundy", newBundy)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getBundle("newBundy")
    }
}