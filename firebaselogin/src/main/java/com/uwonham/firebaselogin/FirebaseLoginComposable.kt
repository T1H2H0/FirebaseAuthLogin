package com.uwonham.firebaselogin

import kotlinx.coroutines.delay
import androidx.compose.material3.OutlinedButton
import android.util.Patterns
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwonham.firebaselogin.utils.EmailValidation
import com.uwonham.firebaselogin.utils.SignInResult

private const val TAG = "FirebaseLoginComposable"

/**
 * Displays a Firebase sign-in dialog with conditional user creation based on email domain.
 * Fixed for edge-to-edge display support.
 *
 * Autofill: both the email and password fields use the modern Compose `ContentType` semantics
 * API (Compose UI 1.7+). The previous approach mixed that with a manual `AutofillNode` /
 * `LocalAutofill` path on the password field that built a brand-new, unregistered `AutofillNode`
 * on every focus change — since it was never added to `LocalAutofillTree`, `requestAutofillForNode`
 * had nothing valid to act on, so it did nothing (and could throw on some devices). That manual
 * path, plus the now-redundant `onGloballyPositioned`/`onFocusChanged` bookkeeping, has been removed.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param allowedEmailDomain The email domain required for user creation (e.g., "@company.com").
 * @param requiredCustomFields Keys in `customData` (populated by [customContent]) that must be
 * non-blank before account creation is allowed. Forwarded to [CreateAccountDialog].
 * @param onDismiss Callback function triggered when the dialog is dismissed.
 * @param onSignInSuccess Called once when sign-in succeeds, providing the signed-in [FirebaseUser].
 * @param onSignInError Optional. Called once per failed sign-in with a user-facing message.
 * @param emailValidator Optional. Decides which emails may be used to CREATE an account
 * (sign-in is never blocked, so existing accounts keep working). Returns null if OK, otherwise
 * the error to show. Defaults to [EmailValidation.default], which rejects employee-number
 * addresses such as 93604552@… or C9939333@…. Pass [EmailValidation.none] to disable.
 * @param allowAnyEmailDomain Lets any valid email create an account (for apps that vet new users
 * themselves, e.g. by admin approval). [allowedEmailDomain] is then ignored.
 * @param userCollection Firestore collection the new user's profile is written to (document id =
 * email). Pass null if the app creates its own profile.
 * @param askEngineerDetails Shows the Engineer Number and ASM fields (both required) on Create
 * Account. Turn off for apps that don't use them.
 * @param onAccountCreated Called once an account has been created (the user is then signed in,
 * with a verification email on its way).
 * @param accountLookup Optional app-supplied check of whether an account exists for an email
 * (e.g. a Cloud Function using the Admin SDK) — needed for a reliable answer when Firebase's email
 * enumeration protection is on. Return null when it can't tell.
 * @param offerCreateWhenUnknown Shows "Create Account" when it isn't known whether the account
 * exists. Turn off to offer it only once an account is known not to exist.
 * @param hidePasswordForNewAccounts When there's known to be no account, hides the password /
 * Sign In part so "Create Account" is the only way on.
 * @param customContent A composable function to customize the content of the dialog to pass to the account creation .
 *
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
 *         }  customContent = { customData ->
 *                var microsoftNumber by remember { mutableStateOf("") }
 *
 *
 *                OutlinedTextField(
 *                    value = microsoftNumber,
 *                    onValueChange = {
 *                        microsoftNumber = it
 *                        customData["microsoftNumber"] = it
 *                    },
 *                    label = { Text("MicroSoftNumber") },
 *                    modifier = Modifier.fillMaxWidth()
 *                )
 *                )
 *            }
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
    requiredCustomFields: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSignInSuccess: (user: com.google.firebase.auth.FirebaseUser) -> Unit,
    customContent: @Composable (customData: SnapshotStateMap<String, Any?>) -> Unit = {},
    onSignInError: (message: String) -> Unit = {},
    emailValidator: (String) -> String? = EmailValidation.default,
    allowAnyEmailDomain: Boolean = false,
    userCollection: String? = "Users",
    askEngineerDetails: Boolean = true,
    onAccountCreated: (user: com.google.firebase.auth.FirebaseUser) -> Unit = {},
    accountLookup: (suspend (email: String) -> Boolean?)? = null,
    offerCreateWhenUnknown: Boolean = true,
    hidePasswordForNewAccounts: Boolean = false,
) {
    val viewModel: FirebaseLoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val activity = context.findActivity()
    val domain = remember(allowedEmailDomain) { allowedEmailDomain.trim().lowercase() }
    val normalisedEmail by remember { derivedStateOf { EmailValidation.normalise(state.email) } }
    // Emails this app lets create an account (with any domain: anything that looks like an email).
    val matchesDomain by remember(domain, allowAnyEmailDomain) {
        derivedStateOf {
            if (allowAnyEmailDomain) {
                Patterns.EMAIL_ADDRESS.matcher(normalisedEmail).matches()
            } else {
                domain.isNotEmpty() && normalisedEmail.endsWith(domain)
            }
        }
    }
    // Only evaluated once the email looks complete (has the domain), so it doesn't flash while typing.
    val creationEmailError by remember(emailValidator) {
        derivedStateOf { if (matchesDomain) emailValidator(normalisedEmail) else null }
    }
    // Firebase said for certain there's no account yet: prompt to create one.
    val showDomainWarning by remember {
        derivedStateOf {
            matchesDomain && state.accountCheckConclusive && !state.accountExists &&
                creationEmailError == null
        }
    }
    // Not known (email enumeration protection): offer "Create account" alongside Sign In.
    val showCreateOption by remember {
        derivedStateOf {
            offerCreateWhenUnknown && matchesDomain && creationEmailError == null &&
                !showDomainWarning && !state.accountCheckConclusive
        }
    }
    val currentOnAccountCreated by rememberUpdatedState(onAccountCreated)
    val currentOnSignInSuccess by rememberUpdatedState(onSignInSuccess)
    val currentOnSignInError by rememberUpdatedState(onSignInError)

    // Track whether forgot password flow is active
    var isForgotPasswordMode by remember { mutableStateOf(false) }

    // Track email domain validation
    var showCreateDialog = remember { mutableStateOf(false) }

    // Focus requesters to move between fields
    val passwordFocusRequester = remember { FocusRequester() }

    // Scroll state for handling overflow
    val scrollState = rememberScrollState()

    LaunchedEffect(auth, allowedEmailDomain, emailValidator, allowAnyEmailDomain, userCollection) {
        viewModel.setAuth(auth)
        if (allowedEmailDomain.isNotEmpty()) {
            viewModel.setAllowedEmailDomain(allowedEmailDomain)
        }
        viewModel.setAllowAnyEmailDomain(allowAnyEmailDomain)
        viewModel.setUserCollection(userCollection)
        viewModel.setAccountLookup(accountLookup)
        viewModel.setEmailValidator(emailValidator)
    }
    // New address, no account: only "Create Account" is shown.
    val hideSignInFields by remember(hidePasswordForNewAccounts) {
        derivedStateOf { hidePasswordForNewAccounts && showDomainWarning }
    }

    // One-shot sign-in results, collected only while the dialog is shown. Replaces
    // observeForever, which was never removed and re-delivered the last Success each time
    // the dialog was reopened (calling onSignInSuccess again).
    LaunchedEffect(viewModel) {
        viewModel.checkForSavedCredentials()
        viewModel.signInEvents.collect { result ->
            when (result) {
                is SignInResult.Error -> currentOnSignInError(result.message)
                SignInResult.Loading -> Unit
                is SignInResult.Success -> result.user?.let { currentOnSignInSuccess(it) }
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

    LaunchedEffect(normalisedEmail) {
        if (matchesDomain && creationEmailError == null) {
            delay(400) // wait for typing to stop; restarts on each change
            viewModel.checkAccountExists(normalisedEmail)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false // Changed to true for better edge-to-edge support
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing) // Use safeDrawing for edge-to-edge
                .imePadding() // Handles keyboard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState), // Add scroll support
                verticalArrangement = Arrangement.Center
            ) {
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
                                onValueChange = { viewModel.updateEmail(it.trim()) },
                                label = { Text("Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { keyboardController?.hide() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentType = ContentType.EmailAddress }
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
                                keyboardActions = KeyboardActions(
                                    onNext = { if (hideSignInFields) focusManager.clearFocus() else passwordFocusRequester.requestFocus() }
                                ),
                                isError = (showDomainWarning && allowedEmailDomain.isNotEmpty() && !allowAnyEmailDomain) ||
                                        (creationEmailError != null && !state.accountExists),
                                supportingText = creationEmailError
                                    ?.takeIf { !state.accountExists }
                                    ?.let { error -> { Text(error) } },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (!it.isFocused) viewModel.normaliseEmail() }
                                    .semantics { contentType = ContentType.EmailAddress }
                            )

                            // Domain warning message and Create Account Button
                            if (showDomainWarning) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (allowAnyEmailDomain || allowedEmailDomain.isEmpty()) {
                                            "No account for this email yet — create one first"
                                        } else {
                                            "Note: First time login with $allowedEmailDomain will require account creation"
                                        },
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showCreateDialog.value = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Create Account")
                                    }
                                }
                            }

                            if (showCreateOption) {
                                OutlinedButton(
                                    onClick = { showCreateDialog.value = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("New here? Create Account")
                                }
                            }

                            if (showCreateDialog.value) {
                                CreateAccountDialog(
                                    auth = auth,
                                    image = image,
                                    email = normalisedEmail,
                                    allowedEmailDomain = if (allowAnyEmailDomain) "" else allowedEmailDomain,
                                    allowAnyEmailDomain = allowAnyEmailDomain,
                                    userCollection = userCollection,
                                    askEngineerDetails = askEngineerDetails,
                                    requiredCustomFields = requiredCustomFields,
                                    emailValidator = emailValidator,
                                    onDismiss = { showCreateDialog.value = false },
                                    onAccountCreated = { user ->
                                        Toast.makeText(
                                            context,
                                            "Account created for ${user.email}. Verification email sent.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        showCreateDialog.value = false
                                        currentOnAccountCreated(user)
                                        // No checkAccountExists here: the ViewModel already marks the
                                        // account as existing, and the check cleared the notice.
                                    },
                                    customContent = customContent
                                )
                            }

                            if (!hideSignInFields) {
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
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            keyboardController?.hide()
                                            activity?.let { viewModel.signIn(it) }
                                        }
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
                                        .focusRequester(passwordFocusRequester)
                                        .semantics { contentType = ContentType.Password }
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
                            }

                            // Notice (e.g. verification email sent)
                            state.infoMessage?.let { info ->
                                Text(
                                    text = info,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodySmall
                                )
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

                                if (!hideSignInFields) {
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

                        // Add extra bottom padding to ensure content is not obscured
                        Spacer(modifier = Modifier.height(16.dp))
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
