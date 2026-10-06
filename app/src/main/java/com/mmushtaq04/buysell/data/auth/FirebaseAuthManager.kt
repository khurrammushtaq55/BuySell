package com.mmushtaq04.buysell.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser?> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user
    }

    suspend fun createAccountWithEmail(email: String, password: String): Result<FirebaseUser?> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        result.user
    }

    fun signOut() {
        auth.signOut()
    }
}
