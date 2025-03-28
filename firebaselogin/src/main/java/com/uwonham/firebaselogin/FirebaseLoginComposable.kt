package com.uwonham.firebaselogin

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwonham.firebaselogin.utils.SignInResult
private const val TAG = "FirebaseLoginComposable"
/**
 * Displays a Firebase sign-in dialog.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param onDismiss Callback function triggered when the dialog is dismissed.
 * @param onSignInSuccess Callback function triggered when sign-in is successful, providing the signed-in [FirebaseUser].
 *
 * ### Example Usage:
 * ```
 * val showSignInDialog = remember { mutableStateOf(true) }
 * Button(onClick = { showSignInDialog.value = !showSignInDialog.value }) {
 *     Text(text = "Login")
 * }
 *
 * if (showSignInDialog.value) {
 *     FirebaseSignInDialog(
 *         auth = auth,
 *         image = imageBitmap,
 *         onDismiss = { showSignInDialog.value = false },
 *         onSignInSuccess = { user ->
 *             Log.d(TAG, "Login successful: $user")
 *             viewModel.userLoggedIn(user)
 *         }
 *     )
 * }
 * ```
 */

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FirebaseSignInDialog(
    auth: com.google.firebase.auth.FirebaseAuth,
    image: ImageBitmap?,
    onDismiss: () -> Unit,
    onSignInSuccess: (user: com.google.firebase.auth.FirebaseUser) -> Unit,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val autofill = LocalAutofill.current
    val context = LocalContext.current
    val activity = context as? Activity

    // Track whether forgot password flow is active
    var isForgotPasswordMode by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.setAuth(auth)
        viewModel.checkForSavedCredentials()
        viewModel.signInResult.observeForever { result ->
            when (result) {
                is SignInResult.Error -> {}
                SignInResult.Loading -> {}
                is SignInResult.Success -> onSignInSuccess(result.user!!)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and Logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        image?.let {
                            Image(
                                modifier = Modifier.size(100.dp),
                                bitmap = it,
                                contentDescription = "Logo"
                            )
                        }
                    }

                    Text(
                        text = if (isForgotPasswordMode) "Reset Password" else "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Conditional content based on forgot password mode
                if (isForgotPasswordMode) {
                    // Forgot Password Flow
                    Text(
                        text = "Enter your email to reset your password",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error or Success Message
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { isForgotPasswordMode = false }) {
                            Text("Back to Sign In")
                        }

                        Button(
                            onClick = {
                                activity?.let {
                                    viewModel.sendPasswordResetEmail(it)
                                }
                            },
                            enabled = !state.isLoading && state.email.isNotBlank()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Reset Password")
                            }
                        }
                    }
                } else {
                    // Regular Sign In Flow
                    // [Keep existing email and password fields from previous implementation]
                    // Email Field
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { viewModel.updateEmail(it) },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Password Field
                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { viewModel.updatePassword(it) },
                        label = { Text("Password") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible)
                                        androidx.compose.material.icons.Icons.Default.Visibility
                                    else
                                        androidx.compose.material.icons.Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()

                    )

                    // Remember me checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var rememberCredentials =
                            Checkbox(
                                checked = state.rememberCredentials,
                                onCheckedChange = { viewModel.setRememberCredentials(it) }
                            )
                        Text(
                            text = "Remember me",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    // Forgot Password Link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isForgotPasswordMode = true }) {
                            Text("Forgot Password?")
                        }
                    }

                    // Error Message
                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Sign In Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                activity?.let { viewModel.signIn(it) }
                            },
                            enabled = !state.isLoading && state.email.isNotBlank() && state.password.isNotBlank()
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Sign In")
                            }
                        }
                    }
                }
            }
        }
    }
}