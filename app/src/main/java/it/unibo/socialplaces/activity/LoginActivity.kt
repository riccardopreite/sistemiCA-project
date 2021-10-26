package it.unibo.socialplaces.activity
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import it.unibo.socialplaces.activity.handler.RecommendationAlarm
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.exception.NotAuthenticatedException
import it.unibo.socialplaces.config.PushNotification
import it.unibo.socialplaces.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock


class LoginActivity : AppCompatActivity() {
    companion object {
        val TAG: String = LoginActivity::class.qualifiedName!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        Auth.loadAuthenticationManager(this)

        /**
         * First the user tries to login via Google through the Android system interface.
         * Then the user is logged in (perhaps even registered if they are not) onto Firebase.
         */
        startActivityForResult(
            Auth.signInIntent(this@LoginActivity),
            Auth.getLoginSystemRequestCode()
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Auth.getLoginSystemRequestCode()) {
            try {
                Auth.loadSignedInAccountFromIntent(data!!)
                CoroutineScope(Dispatchers.IO).launch {
                    if (Auth.isUserAuthenticated()) {
                        setResult(Auth.getLoginSuccessResultCode())
                        val username = Auth.getUsername()
                        username?.let {
                            Notification.setUserId(it)
                            Recommendation.setUserId(it)
                            Friends.setUserId(it)
                            LiveEvents.setUserId(it)
                            PointsOfInterest.setUserId(it)

                            // Loading the notification manager (doing it now since we
                            // are sure every field for the user in the database is set.
                            PushNotification.loadNotificationManager()
                        }

                    } else {
                        setResult(Auth.getLoginFailureResultCode())
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        finish()
                    }
                }
            } catch (exc: NotAuthenticatedException) {
                setResult(Auth.getLoginFailureResultCode())
                CoroutineScope(Dispatchers.Main).launch {
                    finish()
                }
            }
        }
    }
}