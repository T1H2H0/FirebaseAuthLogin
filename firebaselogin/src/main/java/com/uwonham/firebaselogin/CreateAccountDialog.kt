package com.uwonham.firebaselogin

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwonham.firebaselogin.utils.SignInResult

private const val TAG = "CreateAccountDialog"

/**
 * Displays a Create Account dialog for Firebase authentication with Firestore integration.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param allowedEmailDomain The email domain required for account creation (e.g., "@company.com").
 * @param onDismiss Callback function triggered when the dialog is dismissed.
 * @param onAccountCreated Callback function triggered when account creation is successful.
 *
 * ### Example Usage:
 * ```
 * val showCreateAccountDialog = remember { mutableStateOf(true) }
 *
 * if (showCreateAccountDialog.value) {
 *     CreateAccountDialog(
 *         auth = auth,
 *         image = imageBitmap,
 *         allowedEmailDomain = "@company.com",
 *         onDismiss = { showCreateAccountDialog.value = false },
 *         onAccountCreated = { user ->
 *             Log.d(TAG, "Account created: $user")
 *             // Handle successful account creation
 *         }
 *     )
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    auth: com.google.firebase.auth.FirebaseAuth,
    image: ImageBitmap?,
    allowedEmailDomain: String = "",
    onDismiss: () -> Unit,
    onAccountCreated: (user: com.google.firebase.auth.FirebaseUser) -> Unit,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    // Form state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var engineerNumber by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var asm by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("GB") }

    // UI state
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showDomainWarning by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(true) }
    var countryExpanded by remember { mutableStateOf(false) }

    // Country options
    val countries = listOf(
        "GB" to "United Kingdom",
        "US" to "United States",
        "CA" to "Canada",
        "AU" to "Australia",
        "DE" to "Germany",
        "FR" to "France",
        "IT" to "Italy",
        "ES" to "Spain",
        "NL" to "Netherlands",
        "BE" to "Belgium"
    )

    // Initialize ViewModel
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initializing CreateAccountDialog")
        viewModel.setAuth(auth)
        if (allowedEmailDomain.isNotEmpty()) {
            viewModel.setAllowedEmailDomain(allowedEmailDomain)
            Log.d(TAG, "Set allowed email domain: $allowedEmailDomain")
        }
    }

    // Observe account creation result
    DisposableEffect(viewModel) {
        val observer = Observer<SignInResult> { result ->
            Log.d(TAG, "Account creation result: $result")
            when (result) {
                is SignInResult.Error -> {
                    Log.e(TAG, "Account creation failed: ${result.message}")
                    // Error is handled through state.errorMessage
                }
                SignInResult.Loading -> {
                    Log.d(TAG, "Account creation in progress...")
                }
                is SignInResult.Success -> {
                    Log.d(TAG, "Account created successfully: ${result.user?.email}")
                    result.user?.let { user ->
                        onAccountCreated(user)
                        onDismiss() // Close dialog on success
                    }
                }
            }
        }

        viewModel.createAccountResult.observeForever(observer)

        onDispose {
            viewModel.createAccountResult.removeObserver(observer)
        }
    }

    // Validation effects
    LaunchedEffect(email) {
        if (allowedEmailDomain.isNotEmpty() && email.isNotEmpty()) {
            showDomainWarning = !email.endsWith(allowedEmailDomain)
        } else {
            showDomainWarning = false
        }
    }

    LaunchedEffect(password, confirmPassword) {
        passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()
    }

    // Validation function
    fun isFormValid(): Boolean {
        return email.isNotBlank() &&
                password.isNotBlank() &&
                password.length >= 6 &&
                name.isNotBlank() &&
                passwordsMatch &&
                !showDomainWarning
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
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                modifier = Modifier.size(80.dp),
                                bitmap = it,
                                contentDescription = "Logo"
                            )
                        }
                    }

                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Domain requirement info
                if (allowedEmailDomain.isNotEmpty()) {
                    Text(
                        text = "Account creation requires email ending with: $allowedEmailDomain",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    isError = showDomainWarning,
                    modifier = Modifier.fillMaxWidth()
                )

                // Domain warning
                if (showDomainWarning && allowedEmailDomain.isNotEmpty()) {
                    Text(
                        text = "Email must end with $allowedEmailDomain",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password * (min. 6 characters)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
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
                    isError = password.isNotEmpty() && password.length < 6,
                    modifier = Modifier.fillMaxWidth()
                )

                // Password length warning
                if (password.isNotEmpty() && password.length < 6) {
                    Text(
                        text = "Password must be at least 6 characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Confirm Password Field
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    visualTransformation = if (confirmPasswordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible)
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = !passwordsMatch,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!passwordsMatch) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Engineer Number Field
                OutlinedTextField(
                    value = engineerNumber,
                    onValueChange = { engineerNumber = it },
                    label = { Text("Engineer Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Phone Number Field
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Country Dropdown
                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = countries.find { it.first == country }?.second ?: "United Kingdom",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Country") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = countryExpanded,
                        onDismissRequest = { countryExpanded = false }
                    ) {
                        countries.forEach { (code, countryName) ->
                            DropdownMenuItem(
                                text = { Text(countryName) },
                                onClick = {
                                    country = code
                                    countryExpanded = false
                                }
                            )
                        }
                    }
                }

                // ASM Field
                OutlinedTextField(
                    value = asm,
                    onValueChange = { asm = it },
                    label = { Text("ASM") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Error Message
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Buttons
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
                            if (isFormValid()) {
                                Log.d(TAG, "Creating account for: $email")

                                // FIXED: Include password in UserData
                                val userData = UserData(
                                    email = email,
                                    password = password, // THIS WAS MISSING!
                                    engineerNumber = engineerNumber,
                                    country = country,
                                    name = name,
                                    photo = "", // Will be auto-generated
                                    phonenumber = phoneNumber,
                                    asm = asm,
                                    role = "NEWUSER",
                                    deActivated = false,
                                    deactive = false
                                )

                                viewModel.createAccount( userData)
                            } else {
                                Log.w(TAG, "Form validation failed or activity is null")
                            }
                        },
                        enabled = !state.isLoading && isFormValid()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Account")
                        }
                    }
                }
            }
        }
    }
}