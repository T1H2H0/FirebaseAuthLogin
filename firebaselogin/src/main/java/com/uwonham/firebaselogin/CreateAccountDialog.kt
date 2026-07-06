package com.uwonham.firebaselogin

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uwonham.firebaselogin.utils.SignInResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CreateAccountDialog"

/**
 * Displays a Create Account dialog for Firebase authentication with Firestore integration.
 * Fixed version with proper keyboard navigation and scroll behavior.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun CreateAccountDialog(
    auth: com.google.firebase.auth.FirebaseAuth,
    image: ImageBitmap?,
    email: String = "",
    allowedEmailDomain: String = "",
    onDismiss: () -> Unit,
    onAccountCreated: (user: com.google.firebase.auth.FirebaseUser) -> Unit,
    customContent: @Composable (customData: SnapshotStateMap<String, Any?>) -> Unit = {}
) {
    val viewModel: FirebaseLoginViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current
    val density = LocalDensity.current

    // Focus and keyboard management
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Focus requesters for each field
    val emailFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val firstNameFocusRequester = remember { FocusRequester() }
    val lastNameFocusRequester = remember { FocusRequester() }
    val engineerNumberFocusRequester = remember { FocusRequester() }
    val phoneNumberFocusRequester = remember { FocusRequester() }
    val asmFocusRequester = remember { FocusRequester() }
    val countiesFocusRequester = remember { FocusRequester() }

    // Form state
    var emailValue by remember { mutableStateOf(email) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
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
    val customData = remember { mutableStateMapOf<String, Any?>() }
    // Field positions for scrolling - store actual Y coordinates
    var fieldPositions by remember { mutableStateOf(mapOf<String, Float>()) }

    // Autofill nodes
    val emailAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = { emailValue = it }
        )
    }
    val passwordAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.NewPassword),
            onFill = { password = it }
        )
    }
    val firstNameAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.PersonFirstName),
            onFill = { firstName = it }
        )
    }
    val lastNameAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.PersonLastName),
            onFill = { lastName = it }
        )
    }
    val phoneAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.PhoneNumber),
            onFill = { phoneNumber = it }
        )
    }
    val countryAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.AddressCountry),
            onFill = { country = it }
        )
    }
    val engineerNumberAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.Username),
            onFill = { engineerNumber = it }
        )
    }
    val asmAutofillNode = remember {
        AutofillNode(
            autofillTypes = listOf(AutofillType.EmailAddress),
            onFill = { asm = it }
        )
    }

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

    val imeInsets = WindowInsets.ime
    val isKeyboardVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }

    // Improved helper function to scroll to focused field
    fun scrollToField(fieldKey: String) {
        coroutineScope.launch {
            // Give keyboard time to appear
            delay(300)

            fieldPositions[fieldKey]?.let { fieldTop ->
                // Calculate scroll position to keep field visible above keyboard
                // Use a simple offset approach since we can't access WindowInsets here
                val keyboardOffset = if (isKeyboardVisible) 400f else 0f // Approximate keyboard height
                val scrollTarget = (fieldTop - keyboardOffset).coerceAtLeast(0f)

                scrollState.animateScrollTo(scrollTarget.toInt())
            }
        }
    }

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
                }
                SignInResult.Loading -> {
                    Log.d(TAG, "Account creation in progress...")
                }
                is SignInResult.Success -> {
                    Log.d(TAG, "Account created successfully: ${result.user?.email}")
                    result.user?.let { user ->
                        onAccountCreated(user)
                        onDismiss()
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
    LaunchedEffect(emailValue) {
        if (allowedEmailDomain.isNotEmpty() && emailValue.isNotEmpty()) {
            showDomainWarning = !emailValue.endsWith(allowedEmailDomain)
        } else {
            showDomainWarning = false
        }
    }

    LaunchedEffect(password, confirmPassword) {
        passwordsMatch = password == confirmPassword || confirmPassword.isEmpty()
    }

    // Validation function
    fun isFormValid(): Boolean {
        return emailValue.isNotBlank() &&
                password.isNotBlank() &&
                password.length >= 6 &&
                firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                engineerNumber.isNotBlank() &&
                engineerNumber.length >= 5 &&
                asm.isNotBlank() &&
                asm.contains("@") &&
                passwordsMatch &&
                !showDomainWarning
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            decorFitsSystemWindows = false // Important: let us handle insets
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)

                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding() // This handles keyboard padding automatically
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title, Logo and Country Dropdown Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Logo and Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        image?.let {
                            Image(
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(end = 8.dp),
                                bitmap = it,
                                contentDescription = "Logo"
                            )
                        }

                    }

                    // Right side: Country Dropdown
                    ExposedDropdownMenuBox(
                        expanded = countryExpanded,
                        onExpandedChange = { countryExpanded = it },
                        modifier = Modifier.width(210.dp) // Fixed width for country dropdown
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
                }
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Domain requirement info
                if (allowedEmailDomain.isNotEmpty()) {
                    Text(
                        text = "Account will need to be verified via email",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Email Field
                OutlinedTextField(
                    value = emailValue,
                    onValueChange = { emailValue = it },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password * ") },
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
                        modifier = Modifier.weight(1f)                    )



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
                            IconButton(onClick = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }) {
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
                        modifier = Modifier.weight(1f)                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Password length warning
                    if (password.isNotEmpty() && password.length < 6) {
                        Text(
                            text = "Password must be at least 6 characters",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,

                            )
                    }
                    if (!passwordsMatch) {
                        Text(
                            text = "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // First Name and Last Name in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // First Name Field
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Last Name Field
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
if(customContent != {}){
                    customContent(customData)
}
                // Engineer Number Field
                OutlinedTextField(
                    value = engineerNumber,
                    onValueChange = { engineerNumber = it },
                    label = { Text("Engineer Number *") },
                    singleLine = true,
                    isError = engineerNumber.isNotEmpty() && engineerNumber.length < 5,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (engineerNumber.isNotEmpty() && engineerNumber.length < 5) {
                    Text(
                        text = "Please Enter a Valid Engineer Number",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

                // ASM Field
                OutlinedTextField(
                    value = asm,
                    onValueChange = { asm = it },
                    label = { Text("ASM's Email Address *") },
                    singleLine = true,
                    isError = asm.isNotEmpty() && !asm.contains("@"),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (asm.isNotEmpty() && !asm.contains("@")) {
                    Text(
                        text = "ASM must be a valid email address",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp) // Fixed height to prevent layout shift
                        .padding(top = 8.dp), // Extra spacing from form
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            keyboardController?.hide()
                            onDismiss()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (isFormValid()) {
                                keyboardController?.hide()
                                focusManager.clearFocus()

                                Log.d(TAG, "Creating account for: $emailValue")

                                val userData = UserData(
                                    uid = ""  ,
                                    email = emailValue,
                                    password = password,
                                    engineernumber = engineerNumber,
                                    country = country,
                                    name = "$firstName $lastName",
                                    photo = "",
                                    phonenumber = phoneNumber,
                                    asm = asm,
                                    role = "NEWUSER",
                                    deActivated = false,
                                    deactive = false,
                                    customData = customData.toMap()
                                )

                                viewModel.createAccount(userData)
                            } else {
                                Log.w(TAG, "Form validation failed")
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

//@Composable
//fun customFields(modifier: Modifier = Modifier,customData = customData) {
//
//}


//@Preview
//@Composable
//private fun test() {
//    val auth = FirebaseAuth.getInstance()
//    CreateAccountDialog(auth = auth,null,"peter.wonham@beko.com","beko.com",{}) { }
//}