package it.unibo.socialplaces.activity

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import it.unibo.socialplaces.R
import it.unibo.socialplaces.api.ApiConnectors
import it.unibo.socialplaces.config.Api
import it.unibo.socialplaces.config.Auth
import it.unibo.socialplaces.domain.Notification
import it.unibo.socialplaces.security.RSA
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
        if(it.resultCode == Auth.getLoginSuccessResultCode()) {
            CoroutineScope(Dispatchers.IO).launch {
                Notification.addPublicKey(RSA.devicePublicKey)
                CoroutineScope(Dispatchers.Main).launch {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        } else {
            // Login is required.
            val snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                R.string.login_is_required,
                5000
            ).apply {
                setActionTextColor(Color.DKGRAY)
                view.setBackgroundColor(Color.BLACK)

                addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        finish()
                    }
                })
            }

            snackbar.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)

        ApiConnectors.loadStore(resources.openRawResource(R.raw.mystore))
        RSA.loadServerPublicKey(this)
        if(!RSA.loadDeviceKeys()) {
            RSA.generateDeviceKeys()
        }

        val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
        progressBar.visibility = View.VISIBLE

        Auth.loadAuthenticationManager(this)
        CoroutineScope(Dispatchers.IO).launch {
            if(Auth.isUserAuthenticated()) {
                Api.setUserId(Auth.getUsername())

                CoroutineScope(Dispatchers.IO).launch {
                    Notification.addPublicKey(RSA.devicePublicKey)
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