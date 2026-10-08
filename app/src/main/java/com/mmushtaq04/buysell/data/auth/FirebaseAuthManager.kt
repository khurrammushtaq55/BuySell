package com.mmushtaq04.buysell.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "FirebaseAuthManager"
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> = runCatching {
        Log.d(TAG, "Attempting email sign-in for: $email")
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Log.i(TAG, "Email sign-in successful. User Uid: ${result.user?.uid}")
        result.user
    }.onFailure { e ->
        Log.e(TAG, "signInWithEmail failed for $email", e)
    }

    suspend fun createAccountWithEmail(email: String, password: String): Result<FirebaseUser?> = runCatching {
        Log.d(TAG, "Attempting account creation for: $email")
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        Log.i(TAG, "Account creation successful. User Uid: ${result.user?.uid}")
        result.user
    }.onFailure { e ->
        Log.e(TAG, "createAccountWithEmail failed for $email", e)
    }

    suspend fun signInWithGoogleCredential(idToken: String): Result<FirebaseUser?> = runCatching {
        Log.d(TAG, "Attempting Google Auth credential sign-in. ID Token length: ${idToken.length}")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user
        Log.i(TAG, "Google sign-in successful! User Uid: ${user?.uid}, Email: ${user?.email}, DisplayName: ${user?.displayName}")
        user
    }.onFailure { e ->
        Log.e(TAG, "signInWithGoogleCredential failed. ID Token length: ${idToken.length}", e)
    }

    fun signOut() {
        Log.i(TAG, "Signing out user: ${auth.currentUser?.uid}")
        auth.signOut()
    }
}
