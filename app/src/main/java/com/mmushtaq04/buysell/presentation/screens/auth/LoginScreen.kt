package com.mmushtaq04.buysell.presentation.screens.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
fun LoginScreen(
    initialRegisterMode: Boolean = false,
    onNavigateBackToWelcome: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {},
    onEmailAuthSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }

    var isRegisterMode by remember(initialRegisterMode) { mutableStateOf(initialRegisterMode) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val credentialManager = remember { CredentialManager.create(context) }

    val errInvalidEmail = stringResource(R.string.login_err_invalid_email)
    val errPwdShort = stringResource(R.string.login_err_pwd_short)
    val errGoogleFailed = stringResource(R.string.login_err_google_failed)
    val errGoogleCred = stringResource(R.string.login_err_google_cred)
    val errGoogleCanceled = stringResource(R.string.login_err_google_canceled)
    val errAuthFailed = stringResource(R.string.login_err_auth_failed)

    fun launchGoogleSignIn() {
        isLoading = true
        errorMessage = null
        scope.launch {
            runCatching {
                Log.d("LoginScreen", "Starting Google Sign-In flow...")
                val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val webClientId = if (webClientIdResId != 0) {
                    context.getString(webClientIdResId)
                } else {
                    runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
                }

                Log.i("LoginScreen", "Resolved Web Client ID: '$webClientId'")

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
                        errorMessage = err
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                } else {
                    isLoading = false
                    val err = "$errGoogleCred (${credential.type})"
                    errorMessage = err
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                }
            }.onFailure { e ->
                isLoading = false
                val msg = if (e is GetCredentialException && e.message?.contains("cancel", ignoreCase = true) == true) {
                    errGoogleCanceled
                } else {
                    "Google Sign-In Error: ${e.localizedMessage ?: errGoogleFailed}"
                }
                errorMessage = msg
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Back Arrow Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBackToWelcome) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.action_back),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explicit Header Text
            Text(
                text = if (isRegisterMode) stringResource(R.string.login_header_signup) else stringResource(R.string.login_header_login),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.login_subtitle),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text(stringResource(R.string.label_email)) },
                leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text(stringResource(R.string.label_password)) },
                supportingText = {
                    Text(
                        text = if (password.isNotEmpty() && password.length < 6) {
                            stringResource(R.string.login_pwd_char_count, password.length)
                        } else {
                            stringResource(R.string.login_pwd_min_length)
                        },
                        color = if (password.isNotEmpty() && password.length < 6) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                },
                isError = password.isNotEmpty() && password.length < 6,
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (isPasswordVisible) "Hide password" else "Show password"
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            // Submit Button
            Button(
                onClick = {
                    if (email.isBlank() || !email.contains("@")) {
                        errorMessage = errInvalidEmail
                        return@Button
                    }
                    if (password.length < 6) {
                        errorMessage = errPwdShort
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        val result = if (isRegisterMode) {
                            authManager.createAccountWithEmail(email.trim(), password)
                        } else {
                            authManager.signInWithEmail(email.trim(), password)
                        }
                        isLoading = false
                        if (result.isSuccess) {
                            onEmailAuthSuccess()
                        } else {
                            val err = result.exceptionOrNull()?.localizedMessage ?: errAuthFailed
                            errorMessage = err
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = email.isNotBlank() && password.length >= 6 && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = if (isRegisterMode) stringResource(R.string.login_btn_register) else stringResource(R.string.login_btn_submit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  ${stringResource(R.string.login_or_email)}  ",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            // Google Sign-In Option
            OutlinedButton(
                onClick = { launchGoogleSignIn() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(
                    text = "G  ${stringResource(R.string.login_btn_google)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // Toggle Mode Link
            Text(
                text = if (isRegisterMode) stringResource(R.string.login_toggle_login) else stringResource(R.string.login_toggle_register),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .clickable { isRegisterMode = !isRegisterMode }
                    .padding(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    BuySellTheme {
        LoginScreen()
    }
}
