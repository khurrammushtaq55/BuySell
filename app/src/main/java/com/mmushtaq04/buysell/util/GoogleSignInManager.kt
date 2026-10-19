package com.mmushtaq04.buysell.util

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager

object GoogleSignInManager {
    private const val TAG = "GoogleSignInManager"

    suspend fun launchGoogleSignIn(
        context: Context,
        authManager: FirebaseAuthManager = FirebaseAuthManager()
    ): Result<FirebaseUser?> {
        return runCatching {
            Log.d(TAG, "Starting Google Sign-In flow...")
            val credentialManager = CredentialManager.create(context)

            val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            val webClientId = if (webClientIdResId != 0) {
                context.getString(webClientIdResId)
            } else {
                runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
            }

            if (webClientId.isBlank()) {
                throw IllegalStateException("Web Client ID is missing. Check google-services.json and OAuth configuration.")
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = try {
                credentialManager.getCredential(context, request)
            } catch (e: GetCredentialException) {
                if (e.message?.contains("cancel", ignoreCase = true) == true) {
                    throw e
                }
                Log.w(TAG, "GetGoogleIdOption failed (${e.message}), trying GetSignInWithGoogleOption fallback...")
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val fallbackRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                credentialManager.getCredential(context, fallbackRequest)
            }

            val credential = result.credential
            val googleIdTokenCredential = when (credential) {
                is GoogleIdTokenCredential -> credential
                else -> runCatching { GoogleIdTokenCredential.createFrom(credential.data) }
                    .getOrElse { throw IllegalStateException("Unsupported credential type: ${credential.type}") }
            }

            val idToken = googleIdTokenCredential.idToken
            val authResult = authManager.signInWithGoogleCredential(idToken)
            if (authResult.isSuccess) {
                authResult.getOrNull()
            } else {
                throw authResult.exceptionOrNull() ?: Exception("Google Sign-In failed")
            }
        }
    }
}
