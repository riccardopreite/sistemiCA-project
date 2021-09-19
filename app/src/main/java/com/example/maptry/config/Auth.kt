package com.example.maptry.config

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.maptry.R
import com.example.maptry.config.Auth.Google.signInIntent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

object Auth {
    private const val TAG = "Auth"

    private object Google {
        lateinit var googleSignInOptions: GoogleSignInOptions

        lateinit var authCredential: AuthCredential

        var signInAccount : GoogleSignInAccount? = null

        /**
         * Loads the last signed in account (if available, otherwise `null`).
         * Updates [signInAccount].
         */
        fun loadLastSignedInAccount(context: Context) {
            signInAccount = GoogleSignIn.getLastSignedInAccount(context)
        }

        /**
         * Returns the Google Sign in Intent to use in the Activity for logging in.
         * Show the system's UI for logging in via Google.
         *
         * @param activity current activity
         * @return the Google Sign In intent
         */
        fun signInIntent(activity: Activity): Intent = signInClient(activity).signInIntent

        /**
         * The account signed in via the activity pushed through [signInIntent].
         *
         * @return the task wrapping the intent
         */
        fun getSignedInAccountFromIntent(intent: Intent): Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(intent)

        /**
         * Loads the Google credential to be used within Firebase authentication.
         *
         * @see FirebaseAuth
         */
        fun loadGoogleCredential() {
            signInAccount?.let {
                it.idToken?.let { token ->
                    authCredential = GoogleAuthProvider.getCredential(token, null)
                }
            } ?: run {
                Log.e(TAG, "In loadGoogleCredential() Google.signInAccount is null.")
            }
        }

        /**
         * Returns the Google Sign in Options based on th given context.
         *
         * @param context current context
         * @return a singleton instance of GoogleSignInOptions
         */
        private fun signInOptions(context: Context): GoogleSignInOptions {
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
        private fun signInClient(activity: Activity): GoogleSignInClient = GoogleSignIn.getClient(activity, signInOptions(activity))
    }

    private object Firebase {
        lateinit var authManager: FirebaseAuth

        fun loadAuthenticationManager() {
            authManager = FirebaseAuth.getInstance()
        }

        fun signIn(googleCredential: AuthCredential): Task<AuthResult> {
            return authManager.signInWithCredential(googleCredential)
        }
    }

    private lateinit var userToken: String

    fun loadAuthenticationManager(context: Context) {
        Firebase.loadAuthenticationManager()
        Google.loadLastSignedInAccount(context)
    }

    /**
     * Checks whether the user is authenticated via Google.
     * If yes, then a Firebase authentication is attempted and returns true if succeds.
     * If no, then the client should make the user log in via [signInIntent]
     * **ATTENTION** This method is synchronous but performs asynchronous tasks and wait
     * for them to complete. Use inside a thread or Kotlin Coroutine.
     */
    fun isUserAuthenticated(): Boolean {
        Google.signInAccount?.let {
            // The user is authenticated via Google, let's perform Firebase authentication.
            Google.loadGoogleCredential()
            try {
                val authResult = Tasks.await(Firebase.signIn(Google.authCredential))
                authResult.user?.let { firebaseUser ->
                    val tokenResult = Tasks.await(firebaseUser.getIdToken(true))
                    tokenResult.token?.let {
                        userToken = it
                        return true
                    } ?: run {
                        return false
                    }
                } ?: run {
                    return false
                }
            } catch(exc: Exception) {
                return false
            }
        } ?: run {
            Log.e(TAG, "The user is not logged in or loadAuthenticationManager() has not been called, returning false.")
            return false
        }
    }

    /**
     * Returns the Google Sign in Intent to use in the Activity for logging in.
     * Show the system's UI for logging in via Google.
     *
     * @param activity current activity
     * @return the Google Sign In intent
     */
    fun signInIntent(activity: Activity): Intent = Google.signInIntent(activity)

    /**
     * The account signed in via the activity pushed through [signInIntent].
     */
    fun loadSignedInAccountFromIntent(intent: Intent) {
        val task = Google.getSignedInAccountFromIntent(intent)
        try {
            Google.signInAccount = task.getResult(ApiException::class.java)
        } catch(exc: ApiException) {
            Log.e(TAG, "The sign in process via Google failed: " + exc.message)
        }
    }

    /**
     * Returns the user token, if available.
     */
    fun getToken(): String {
        return userToken
    }

    /**
     * Returns the user profile picture, if available.
     */
    fun getUserProfileIcon(): Uri? = Google.signInAccount?.photoUrl

    /**
     * Returns the user full name, if available.
     */
    fun getUserFullName(): String? = Google.signInAccount?.displayName

    /**
     * Returns the user email address, if available.
     */
    fun getUserEmailAddress(): String? = Google.signInAccount?.email
}