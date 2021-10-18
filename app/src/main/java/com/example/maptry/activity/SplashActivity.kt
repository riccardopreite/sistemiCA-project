package com.example.maptry.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.example.maptry.R
import com.example.maptry.api.RetrofitInstances
import com.example.maptry.config.Auth
import com.example.maptry.domain.Friends
import com.example.maptry.domain.LiveEvents
import com.example.maptry.domain.PointsOfInterest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity(R.layout.activity_splash) {
    companion object {
        private val TAG = SplashActivity::class.qualifiedName
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
                    Friends.setUserId(username)
                    LiveEvents.setUserId(username)
                    PointsOfInterest.setUserId(username)
                }
                CoroutineScope(Dispatchers.Main).launch {
                    progressBar.visibility = View.GONE
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    startActivityForResult(
                        Intent(this@SplashActivity, LoginActivity::class.java),
                        Auth.getLoginActivityRequestCode()
                    )
                }
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == Auth.getLoginActivityRequestCode()) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}