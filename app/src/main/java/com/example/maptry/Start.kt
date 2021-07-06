package com.example.maptry

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Button
import androidx.core.app.NotificationCompat
import com.example.maptry.MapsActivity.Companion.account
import com.example.maptry.MapsActivity.Companion.context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore



//created to start service at boot time but cause problem with context and activity
class Start : BroadcastReceiver() {

    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions
    override fun onReceive(context: Context, intent: Intent) {
        println("BOOT FINISHED")
        MapsActivity.firebaseAuth = FirebaseAuth.getInstance()
        MapsActivity.context = context
        FirebaseFirestore.setLoggingEnabled(true)
        MapsActivity.db = FirebaseFirestore.getInstance()
        account = GoogleSignIn.getLastSignedInAccount(context)
        println("maybe done")
        println(account?.email)
        account?.let { firebaseAuthWithGoogle(it) }

    }
    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        MapsActivity.firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val i = Intent(context, NotifyService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    println("STARTING FOREGROUND")
                    context.startForegroundService(i)
                } else {

                    println("STARTING SERVICE")
                    context.startService(i)
                }
            } else {
                configureGoogleSignIn()
            }
        }
    }
    private fun configureGoogleSignIn() {
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(context, mGoogleSignInOptions)
        account = GoogleSignIn.getLastSignedInAccount(context)
        println("DONE")
    }
}