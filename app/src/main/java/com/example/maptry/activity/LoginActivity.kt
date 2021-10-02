package com.example.maptry.activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        Auth.loadAuthenticationManager(this)

        /**
         * First the user tries to login via Google through the Android system interface.
         * Then the user is logged in (perhaps even registered if they are not) onto Firebase.
         */
        startActivityForResult(Auth.signInIntent(this@LoginActivity), Auth.getLoginSystemRequestCode())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Auth.getLoginSystemRequestCode()) {
            Auth.loadSignedInAccountFromIntent(data!!)
            CoroutineScope(Dispatchers.IO).launch {
                if(Auth.isUserAuthenticated()) {
                    setResult(Auth.getLoginSuccessResultCode())
                    val username = Auth.getUsername()
                    username?.let {
                        Friends.setUserId(username)
                        LiveEvents.setUserId(username)
                        PointsOfInterest.setUserId(username)
                    }
                } else {
                    setResult(Auth.getLoginFailureResultCode())
                }
                CoroutineScope(Dispatchers.Main).launch {
                    finish()
                }
            }
        }
    }
}