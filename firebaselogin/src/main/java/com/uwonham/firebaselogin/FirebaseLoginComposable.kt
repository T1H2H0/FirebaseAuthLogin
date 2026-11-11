package com.uwonham.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uwonham.firebaselogin.utils.EmailValidation
import com.uwonham.firebaselogin.utils.SignInResult

private const val TAG = "FirebaseLoginComposable"

/**
 * Displays a Firebase sign-in dialog.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSignInSuccess Called once when sign-in succeeds, with the signed-in [FirebaseUser].
 * @param onSignInError Optional. Called once per failed sign-in attempt with a user-facing message.
 * @param emailValidator Optional. Returns null if the email is acceptable, or an error message.
 * Defaults to [EmailValidation.default], which rejects employee-number addresses
 * (e.g. 93604552@… or C9939333@…). Pass [EmailValidation.none] to disable.
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
    auth: FirebaseAuth,
    image: ImageBitmap?,
    onDismiss: () -> Unit,
    onSignInSuccess: (user: FirebaseUser) -> Unit,
    onSignInError: (message: String) -> Unit = {},
    emailValidator: (String) -> String? = EmailValidation.default,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val autofill = LocalAutofill.current
    val activity = LocalContext.current.findActivity()

    val currentOnSuccess by rememberUpdatedState(onSignInSuccess)
    val currentOnError by rememberUpdatedState(onSignInError)

    LaunchedEffect(auth, emailValidator) {
        viewModel.setAuth(auth)
        viewModel.setEmailValidator(emailValidator)
    }

    // One-shot results, collected only while the dialog is shown. Replaces observeForever,
    // which leaked and re-fired the last Success every time the dialog was reopened.
    LaunchedEffect(viewModel) {
        viewModel.checkForSavedCredentials()
        viewModel.events.collect { result ->
            when (result) {
                is SignInResult.Success -> result.user?.let { currentOnSuccess(it) }
                is SignInResult.Error -> currentOnError(result.message)
                SignInResult.Loading -> Unit
            }
        }
    }

    LaunchedEffect(state.useSavedCredentials) {
        if (state.useSavedCredentials) {
            if (activity != null) {
                viewModel.loadSavedCredentials(activity)
            } else {
                Log.e(TAG, "No Activity found for Credential Manager")
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
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // "Use saved password" option
                if (state.useSavedCredentials && state.email.isEmpty()) {
                    Button(
                        onClick = { activity?.let { viewModel.loadSavedCredentials(it) } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Use Saved Password")
                    }

                    Text(
                        text = "Or sign in with another account",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Email field with autofill
                val emailAutofillNode = remember {
                    AutofillNode(
                        autofillTypes = listOf(AutofillType.EmailAddress, AutofillType.Username),
                        onFill = { viewModel.updateEmail(it.trim()) }
                    )
                }
                var emailHadFocus by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = { Text("Email") },
                    singleLine = true,
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            emailAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                emailHadFocus = true
                                autofill?.requestAutofillForNode(emailAutofillNode)
                            } else {
                                autofill?.cancelAutofillForNode(emailAutofillNode)
                                if (emailHadFocus) viewModel.onEmailFocusLost()
                            }
                        }
                )

                // Password field with autofill
                var passwordVisible by remember { mutableStateOf(false) }

                val passwordAutofillNode = remember {
                    AutofillNode(
                        autofillTypes = listOf(AutofillType.Password),
                        onFill = { viewModel.updatePassword(it) }
                    )
                }

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
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            passwordAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                autofill?.requestAutofillForNode(passwordAutofillNode)
                            } else {
                                autofill?.cancelAutofillForNode(passwordAutofillNode)
                            }
                        }
                )

                // Remember me
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

                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

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
                            if (activity != null) {
                                viewModel.signIn(activity)
                            } else {
                                Log.e(TAG, "No Activity found; cannot sign in")
                            }
                        },
                        enabled = !state.isLoading &&
                            state.emailError == null &&
                            state.email.isNotBlank() &&
                            state.password.isNotBlank()
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

/** Walks ContextWrappers to find the hosting Activity (LocalContext isn't always the Activity). */
internal fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
