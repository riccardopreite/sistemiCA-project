package com.example.maptry.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.maptry.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object Auth {
    lateinit var authManager: FirebaseAuth

    var signInAccount : GoogleSignInAccount? = null

    lateinit var googleSignInOptions: GoogleSignInOptions

    lateinit var userToken: String

    fun loadAuthenticationManager(): FirebaseAuth {
        authManager = FirebaseAuth.getInstance()
        return authManager
    }

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
    private fun getSignInClient(activity: Activity): GoogleSignInClient = GoogleSignIn.getClient(activity, getGoogleSignInOptions(activity))

    /**
     * Returns the Google Sign in Intent based on the given activity.
     *
     * @param activity current activity
     * @return the Google Sign in intent
     */
    fun getSignInIntent(activity: Activity): Intent = getSignInClient(activity).signInIntent

    /**
     * Returns the last signed in account (if available, otherwise `null`).
     *
     * @return the account signed in last time (could be `null`).
     */
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    /**
     * The account signed in via the activity pushed through [getSignInIntent].
     *
     * @return the task wrapping the intent
     */
    fun getSignedInAccountFromIntent(data: Intent?): Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)

    /**
     * Returns a Google credential to be used inside [authManager]'s method [signInWithCredential].
     *
     * @see FirebaseAuth
     */
    fun getGoogleCredential(idToken: String): AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
}