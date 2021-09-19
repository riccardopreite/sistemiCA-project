package com.example.maptry.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import com.example.maptry.R
import com.example.maptry.config.Auth

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
        Auth.loadAuthenticationManager()
        val intent = if(Auth.getLastSignedInAccount(this) == null) {
            progressBar.visibility = View.VISIBLE
            Intent(this, LoginActivity::class.java)
        } else {
            progressBar.visibility = View.VISIBLE
            Intent(this, MainActivity::class.java).apply {

            }
        }
        startActivity(intent)
    }
}