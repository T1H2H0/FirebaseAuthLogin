package com.uwonham.firebaselogin

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwonham.firebaselogin.utils.SignInResult

private const val TAG = "FirebaseLoginComposable"

/**
 * Displays a Firebase sign-in dialog with conditional user creation based on email domain.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param allowedEmailDomain The email domain required for user creation (e.g., "@company.com").
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
 *         allowedEmailDomain = "@company.com",
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
    allowedEmailDomain: String = "",
    onDismiss: () -> Unit,
    onSignInSuccess: (user: com.google.firebase.auth.FirebaseUser) -> Unit,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val autofill = LocalAutofill.current
    val context = LocalContext.current
    val activity = context as? Activity
    val showDomainWarning by remember {
        derivedStateOf {
            allowedEmailDomain.isNotEmpty() &&
                    state.email.isNotEmpty() &&
                    state.email.endsWith(allowedEmailDomain) &&
                    !state.accountExists &&
                    state.email.isNotBlank()
        }
    }

    // Track whether forgot password flow is active
    var isForgotPasswordMode by remember { mutableStateOf(false) }

    // Track email domain validation
    var showCreateDialog = remember { mutableStateOf(false) }

    // Track if components are positioned for autofill
    var emailFieldPositioned by remember { mutableStateOf(false) }
    var passwordFieldPositioned by remember { mutableStateOf(false) }

    LaunchedEffect(true) {
        viewModel.setAuth(auth)
        if (allowedEmailDomain.isNotEmpty()) {
            viewModel.setAllowedEmailDomain(allowedEmailDomain)
        }
        viewModel.checkForSavedCredentials()
        viewModel.signInResult.observeForever { result ->
            when (result) {
                is SignInResult.Error -> {}
                SignInResult.Loading -> {}
                is SignInResult.Success -> onSignInSuccess(result.user!!)
            }
        }
    }

    LaunchedEffect(state.useSavedCredentials) {
        if (state.useSavedCredentials) {
            Log.d(TAG, "SignInDialog: ${state.useSavedCredentials}")
            if (activity != null) {
                viewModel.loadSavedCredentials(activity)
            } else {
                Log.e("LoginScreen", "Activity context is null")
            }
        }
    }

    LaunchedEffect(state.email) {
        if (allowedEmailDomain.isNotEmpty() &&
            state.email.isNotEmpty() &&
            state.email.endsWith(allowedEmailDomain)) {
            viewModel.checkAccountExists(state.email)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnClickOutside = true,
        decorFitsSystemWindows = false
    )) {
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
                            color = if (state.errorMessage!!.contains("sent"))
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
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
                                viewModel.sendPasswordResetEmail()
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
                        isError = showDomainWarning && allowedEmailDomain.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                emailFieldPositioned = true
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && emailFieldPositioned) {
                                    try {
                                        autofill?.requestAutofillForNode(
                                            AutofillNode(
                                                autofillTypes = listOf(AutofillType.EmailAddress),
                                                onFill = { viewModel.updateEmail(it) }
                                            )
                                        )
                                    } catch (e: IllegalStateException) {
                                        Log.w(TAG, "Autofill request failed: ${e.message}")
                                    }
                                }
                            }
                    )

                    // Domain warning message
                    if (showDomainWarning && allowedEmailDomain.isNotEmpty()) {
                        Text(
                            text = "Note: First time login with $allowedEmailDomain will require account creation",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { showCreateDialog.value = true }) {
                            Text("Create Account")
                        }
                    }

                    if (showCreateDialog.value) {
                        CreateAccountDialog(
                            auth = auth,
                            image = image,
                            email = state.email,
                            allowedEmailDomain = allowedEmailDomain,
                            onDismiss = { showCreateDialog.value = false },
                            onAccountCreated = { user ->
                                Toast.makeText(context, "Account created: ${user.displayName}", Toast.LENGTH_SHORT).show()
                                showCreateDialog.value = false
                                viewModel.checkAccountExists(state.email)
                            }
                        )
                    }

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
                            .onGloballyPositioned {
                                passwordFieldPositioned = true
                            }
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && passwordFieldPositioned) {
                                    try {
                                        autofill?.requestAutofillForNode(
                                            AutofillNode(
                                                autofillTypes = listOf(AutofillType.Password),
                                                onFill = { viewModel.updatePassword(it) }
                                            )
                                        )
                                    } catch (e: IllegalStateException) {
                                        Log.w(TAG, "Autofill request failed: ${e.message}")
                                    }
                                }
                            }
                    )

                    // Remember me checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                    state.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                        val creationTime = state.auth?.currentUser?.metadata?.creationTimestamp
                        if (
                            errorMessage.contains("User not verified") &&
                            creationTime != null &&
                            creationTime < System.currentTimeMillis() - 1000 * 60 * 10
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please wait for 10 minutes before trying to resend verification email")

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = {
                                state.auth?.currentUser?.let { user ->
                                    viewModel.sendEmailVerification(user = user)
                                }
                            }) {
                                Text("Resend Verification Email ")
                            }
                        }
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