package com.example.maptry.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.example.maptry.R
import com.example.maptry.config.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    companion object {
        private val TAG = SplashActivity::class.qualifiedName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        Auth.loadAuthenticationManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            val intent = if(Auth.isUserAuthenticated()) {
                Intent(this@SplashActivity, LoginActivity::class.java)
            } else {
                Intent(this@SplashActivity, MainActivity::class.java)
            }
            CoroutineScope(Dispatchers.Main).launch {
                progressBar.visibility = View.GONE
                startActivity(intent)
            }
        }
    }
}