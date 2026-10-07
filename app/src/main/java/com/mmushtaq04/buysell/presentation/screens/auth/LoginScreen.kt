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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Storefront
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
    onGoogleSignInClick: () -> Unit = {},
    onEmailAuthSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }

    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val credentialManager = remember { CredentialManager.create(context) }

    val errInvalidEmail = stringResource(R.string.login_err_invalid_email)
    val errPwdShort = stringResource(R.string.login_err_pwd_short)

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
            Spacer(modifier = Modifier.height(20.dp))

            // Header Icon
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.app_name),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.login_tagline),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Google Sign-In Button
            OutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    scope.launch {
                        runCatching {
                            val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                            val webClientId = if (webClientIdResId != 0) {
                                context.getString(webClientIdResId)
                            } else {
                                runCatching { context.getString(R.string.default_web_client_id) }.getOrDefault("")
                            }

                            Log.d("LoginScreen", "Verifying Web Client ID: '$webClientId'")

                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .apply {
                                    if (webClientId.isNotBlank()) setServerClientId(webClientId)
                                }
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential

                            if (credential is GoogleIdTokenCredential) {
                                val idToken = credential.idToken
                                val authResult = authManager.signInWithGoogleCredential(idToken)
                                isLoading = false
                                if (authResult.isSuccess) {
                                    onGoogleSignInClick()
                                } else {
                                    val err = authResult.exceptionOrNull()?.localizedMessage ?: "Google sign-in failed"
                                    errorMessage = err
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                isLoading = false
                                val err = "Google account credential error"
                                errorMessage = err
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        }.onFailure { e ->
                            isLoading = false
                            val msg = if (e is GetCredentialException && e.message?.contains("cancel", ignoreCase = true) == true) {
                                "Google Sign-In canceled"
                            } else {
                                "Google Sign-In Error: ${e.localizedMessage ?: "Problem logging in"}"
                            }
                            errorMessage = msg
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G  ${stringResource(R.string.login_btn_google)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
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

            // Password Field with Show/Hide Toggle
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                label = { Text(stringResource(R.string.label_password)) },
                supportingText = {
                    Text(
                        text = if (password.isNotEmpty() && password.length < 4) {
                            stringResource(R.string.login_pwd_char_count, password.length)
                        } else {
                            stringResource(R.string.login_pwd_min_length)
                        },
                        color = if (password.isNotEmpty() && password.length < 4) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                },
                isError = password.isNotEmpty() && password.length < 4,
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
                    if (password.length < 4) {
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
                            val err = result.exceptionOrNull()?.localizedMessage ?: "Authentication failed"
                            errorMessage = err
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                enabled = email.isNotBlank() && password.length >= 4 && !isLoading,
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

            // Mode Toggle Text
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
