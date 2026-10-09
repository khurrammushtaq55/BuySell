package com.mmushtaq04.buysell.presentation.screens.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.mmushtaq04.buysell.R
import com.mmushtaq04.buysell.data.auth.FirebaseAuthManager
import com.mmushtaq04.buysell.ui.theme.BuySellTheme
import kotlinx.coroutines.launch

@Composable
fun WelcomeLandingScreen(
    onNavigateToRegister: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }

    val errGoogleFailed = stringResource(R.string.login_err_google_failed)
    val errGoogleCred = stringResource(R.string.login_err_google_cred)
    val errGoogleCanceled = stringResource(R.string.login_err_google_canceled)

    fun launchGoogleSignIn() {
        isLoading = true
        scope.launch {
            runCatching {
                Log.d("WelcomeLandingScreen", "Starting Google Sign-In flow...")
                val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val webClientId = if (webClientIdResId != 0) {
                    context.getString(webClientIdResId)
                } else {
                    runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
                }

                Log.i("WelcomeLandingScreen", "Resolved Web Client ID: '$webClientId'")

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .apply {
                        if (webClientId.isNotBlank()) setServerClientId(webClientId)
                    }
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                val googleIdTokenCredential = when (credential) {
                    is GoogleIdTokenCredential -> credential
                    else -> runCatching { GoogleIdTokenCredential.createFrom(credential.data) }.getOrNull()
                }

                if (googleIdTokenCredential != null) {
                    val idToken = googleIdTokenCredential.idToken
                    val authResult = authManager.signInWithGoogleCredential(idToken)
                    isLoading = false
                    if (authResult.isSuccess) {
                        onGoogleSignInClick()
                    } else {
                        val exception = authResult.exceptionOrNull()
                        val err = exception?.localizedMessage ?: errGoogleFailed
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                } else {
                    isLoading = false
                    val err = "$errGoogleCred (${credential.type})"
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            }.onFailure { e ->
                isLoading = false
                val msg = if (e is GetCredentialException && e.message?.contains("cancel", ignoreCase = true) == true) {
                    errGoogleCanceled
                } else {
                    "Google Sign-In Error: ${e.localizedMessage ?: errGoogleFailed}"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // App Logo Header
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(92.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.welcome_landing_title, stringResource(R.string.app_name)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.welcome_landing_sub),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3 Feature Highlight Cards
            FeatureHighlightCard(
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                title = stringResource(R.string.welcome_feature_1_title),
                sub = stringResource(R.string.welcome_feature_1_sub)
            )

            FeatureHighlightCard(
                icon = Icons.Default.CloudDone,
                title = stringResource(R.string.welcome_feature_2_title),
                sub = stringResource(R.string.welcome_feature_2_sub)
            )

            FeatureHighlightCard(
                icon = Icons.Default.Group,
                title = stringResource(R.string.welcome_feature_3_title),
                sub = stringResource(R.string.welcome_feature_3_sub)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Big Green Button: Create New Shop
            Button(
                onClick = onNavigateToRegister,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = stringResource(R.string.welcome_btn_get_started),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Secondary Outlined Button: Log In to Existing Shop
            OutlinedButton(
                onClick = onNavigateToLogin,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_btn_login_existing),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Google Sign-In Direct Option
            OutlinedButton(
                onClick = { launchGoogleSignIn() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "G  ${stringResource(R.string.login_btn_google)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    sub: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = sub,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeLandingScreenPreview() {
    BuySellTheme {
        WelcomeLandingScreen()
    }
}
