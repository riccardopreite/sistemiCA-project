package com.example.maptry.config

import android.app.Activity
import android.content.Context
import com.example.maptry.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

object Auth {
    lateinit var authenticationManager: FirebaseAuth
    var signInAccount : GoogleSignInAccount? = null

    lateinit var googleSignInOptions: GoogleSignInOptions

    lateinit var userToken: String

    /**
     * Returns the Google Sign in Options based on th given context.
     *
     * @param context current context
     * @return a singleton instance of GoogleSignInOptions
     */
    private fun getGoogleSignInOptions(context: Context): GoogleSignInOptions {
        if(!(this::googleSignInOptions.isInitialized)) {
            googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        }
        return googleSignInOptions
    }

    /**
     * Returns the Google Sign in Client based on the given activity.
     *
     * @param activity current activity
     * @return the Google Sign in client
     */
    fun getSignInClient(activity: Activity): GoogleSignInClient = GoogleSignIn.getClient(activity, getGoogleSignInOptions(activity))

    /**
     * Returns the last signed in account (if available, otherwise `null`).
     *
     * @return
     */
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount = GoogleSignIn.getLastSignedInAccount(context)


}