package com.example.maptry.activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.maptry.R
import android.Manifest
import com.example.maptry.activity.MapsActivity.Companion.locationCallback
import com.example.maptry.activity.MapsActivity.Companion.mLocationRequest
import com.example.maptry.activity.MapsActivity.Companion.newBundy

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import androidx.appcompat.app.AlertDialog
import com.example.maptry.config.Auth


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



    /**
     * Flag to memorize whether the user requested to utilize location services or not.
     */
    private var locationPermissionAllowed: Boolean = false

    /**
     * Permission request launcher (for location permission).
     */
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        isGranted: Boolean ->
        if(isGranted) {
            locationPermissionAllowed = true
            Log.i(TAG, "Permission to use location GRANTED")
        } else {
            locationPermissionAllowed = false
            Log.i(TAG, "Permission to use location DENIED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Auth.loadAuthenticationManager()

        checkPermissionsAndSignIn()
    }

    /**
     * Starts a new activity that asks the user to sign in. The result is returned through an intent
     * that gets processed in [onActivityResult].
     */
    private fun signInWithGoogle() {
        Log.v(TAG, "signInWithGoogle")
        Log.i(TAG, "The user tries to log in.")
        startActivityForResult(Auth.getSignInIntent(this), requestCodeSignIn)
    }

    /**
     * After authenticating via Google, the user is authenticated against Firebase Auth
     * in order to retrieve the user token to use on the webservices.
     */
    private fun firebaseAuthWithGoogle() {
        Log.v(TAG, "firebaseAuthWithGoogle")
        val credential = Auth.getGoogleCredential(Auth.signInAccount!!.idToken!!) // Auth.signInAccount, idToken are surely not null.
        Auth.authManager.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Auth.authManager.currentUser!!.getIdToken(true).addOnCompleteListener {
                    if(it.isSuccessful) {
                        Log.i(TAG, "Token for APIs successfully retrieved")
                        Auth.userToken = it.result.token!!
                        finish()
                    }
                }
            } else {
                Auth.getLastSignedInAccount(this)
                signInWithGoogle()
            }
        }
    }

    /**
     * This method checks permissions to ACCESS_FINE_LOCATION and if enabled:
     * - enables the retrieval of location updates
     * - checks if the user is logged in via Google (authentication provided by the device) and if so, logs in via Firebase
     * - if the use is not logged in, it asks the user to log in
     * If
     */
    private fun checkPermissionsAndSignIn() {
        Log.v(TAG, "checkPermissionsAndSignIn")
        // Location permission check
        when {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "Enabling location services since permissions were given.")
                locationPermissionAllowed = true

                MapsActivity.mMap.isMyLocationEnabled = true
                LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                    mLocationRequest as LocationRequest,
                    locationCallback,
                    Looper.myLooper()!!
                )

                retrieveGoogleAccountAndSignInOnFirebase()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Log.i(TAG, "Asking the user for permissions (rationale).")
                // addition rationale should be displayed
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(title)
                    .setMessage(R.string.location_permission_required)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        requestPermissionLauncher.launch(
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    } // The user can only accept location functionalities.
                builder.create().show()
            }
            else -> {
                Log.i(TAG, "Asking the user for permissions.")
                requestPermissionLauncher.launch(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    /**
     * Retrieves the account via Google Play Services API, if it is null then asks the user to
     * login ([signInWithGoogle]), otherwise it logs the user in via Firebase.
     */
    private fun retrieveGoogleAccountAndSignInOnFirebase() {
        Log.v(TAG, "retrieveGoogleAccountAndSignInOnFirebase")
        Auth.getLastSignedInAccount(this)
        if(Auth.signInAccount == null) {
            Log.i(TAG, "Asking the user to log in.")
            signInWithGoogle()
        } else {
            Auth.signInAccount?.let {
                Log.i(TAG, "The user has already logged in.")
                setResult(resultCodeSignedIn, Intent().apply {
                    this.data = Uri.parse("signed-in") // this is referred to the Intent.
                })
                firebaseAuthWithGoogle()
                // Signed in successfully, show authenticated UI.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.v(TAG, "onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCodeSignIn) {
            val task = Auth.getSignedInAccountFromIntent(data)
            try {
                Auth.signInAccount = task.getResult(ApiException::class.java)!!
                Auth.signInAccount?.let {
                    setResult(resultCodeSignedIn, Intent().apply {
                        this.data = Uri.parse("signed-in") // this is referred to the Intent.
                    })
                    firebaseAuthWithGoogle()
                }
            } catch (e: ApiException) {
                Log.w(TAG, "The sign in process via Google failed: " + e.message)
                setResult(resultCodeNotSignedIn, Intent().apply {
                    this.data = Uri.parse("not-signed-in")
                })
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.v(TAG, "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Login independently of the result of the permission request (of course the app functions are limited)
        if(locationPermissionAllowed) {
            checkPermissionsAndSignIn()
        } else {
            retrieveGoogleAccountAndSignInOnFirebase()
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