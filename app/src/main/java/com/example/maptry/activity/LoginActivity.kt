package com.example.maptry.activity
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import com.example.maptry.R
import com.example.maptry.activity.MapsActivity.Companion.REQUEST_LOCATION_PERMISSION

import com.example.maptry.activity.MapsActivity.Companion.firebaseAuth
import com.example.maptry.activity.MapsActivity.Companion.locationCallback
import com.example.maptry.activity.MapsActivity.Companion.mLocationRequest
import com.example.maptry.activity.MapsActivity.Companion.newBundy

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import java.lang.Exception

class LoginActivity : AppCompatActivity() {

    //Login
    private val rcSignIn: Int = 1
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions

    private var account: GoogleSignInAccount? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        firebaseAuth = FirebaseAuth.getInstance()


        val data = Intent()

        data.data = Uri.parse("done")

        // ask gps permission if not allowed yet
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions()
        }
        else {
            // simply get last account
            startAccount()
        }


    }

    private fun configureGoogleSignIn() {
        // set google key and prepare for sign in
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
         account = GoogleSignIn.getLastSignedInAccount(this)
        setResultLogin(account)
    }


    /*Start SignIn Function*/
    private fun signIn() {
        // intent to sign in
        val signInIntent : Intent = mGoogleSignInClient.signInIntent
        println(signInIntent)

        startActivityForResult(signInIntent, rcSignIn)
    }

    private fun setResultLogin( account: GoogleSignInAccount?){
        if(account != null){
            val data = Intent()
            data.data = Uri.parse("done")
            setResult(50, data)

        }else {
            val data = Intent()
            data.data = Uri.parse("Not logged")
            setResult(40, data)
        }
    }

    private fun handleSignInResult( completedTask:Task<GoogleSignInAccount>) {
        try {
            account  = completedTask.getResult(ApiException::class.java)
            // connection with firebase
            account?.let { firebaseAuthWithGoogle(it) }
            account?.let { setResultLogin(it) }
        } catch (e: ApiException) {
            Log.w("IN FAIL", "signInResult:failed code=" + e.statusCode)
            setResultLogin(null)
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                this.finish()
            } else {
                configureGoogleSignIn()
                signIn()
            }
        }
    }

    private fun startAccount(){
        if(Build.VERSION.SDK_INT >= 23 && checkPermission()) {
            try {
                //try to set up map location
                MapsActivity.mMap.isMyLocationEnabled = true
                val loop = Looper.myLooper() as Looper
                LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(
                    mLocationRequest as LocationRequest, locationCallback, loop
                )
            }
            catch (e:Exception){}
        }
        if(GoogleSignIn.getLastSignedInAccount(this) != null) {
            account = GoogleSignIn.getLastSignedInAccount(this)
            account?.let { firebaseAuthWithGoogle(it) }
            // Signed in successfully, show authenticated UI.
            account?.let { setResultLogin(it) }
        }
        else{
            configureGoogleSignIn()
            signIn()
        }
    }

    private fun checkPermission() : Boolean {
        return if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            requestPermissions()
            false
        }
    }

/*End SignIn Function*/

    fun get(): GoogleSignInAccount? {
        return this.account
    }

/*Start Override Function*/

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {



        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == rcSignIn) {
            val task:Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }
    override fun onBackPressed() { }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Login independently of the result of the permission request
        startAccount()
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

    fun closeDrawer(view: View) {}
    fun addFriend(view: View) {}

/*End Override Function*/
}