package com.uwonham.firebaselogin

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
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
 * Now includes proper keyboard navigation, scroll behavior, and working autofill.
 *
 * @param auth The Firebase authentication instance.
 * @param image An optional [ImageBitmap] to display in the dialog.
 * @param allowedEmailDomain The email domain required for account creation (e.g., "@company.com").
 * @param onDismiss Callback function triggered when the dialog is dismissed.
 * @param onAccountCreated Callback function triggered when account creation is successful.
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
) {
    val viewModel: LoginViewModel = hiltViewModel()
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

    // Field positions for scrolling
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

    // Helper function to scroll to focused field
    fun scrollToField(fieldKey: String) {
        coroutineScope.launch {
            delay(150) // Delay to ensure keyboard is shown and layout is settled
            fieldPositions[fieldKey]?.let { position ->
                val targetScroll = (position - 200f).coerceAtLeast(0f).coerceAtMost(scrollState.maxValue.toFloat())
                scrollState.animateScrollTo(targetScroll.toInt())
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
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(16.dp)
                .windowInsetsPadding(WindowInsets.ime)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    })
                }
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
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

                        Box {
                            Text(
                                text = "Create Account",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            passwordFocusRequester.requestFocus()
                            scrollToField("password")
                        }
                    ),
                    isError = showDomainWarning,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("email" to coordinates.boundsInWindow().top)
                            emailAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            autofill?.apply {
                                if (focusState.isFocused && emailAutofillNode.boundingBox != null) {
                                    requestAutofillForNode(emailAutofillNode)
                                } else {
                                    cancelAutofillForNode(emailAutofillNode)
                                }
                            }
                        }
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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            confirmPasswordFocusRequester.requestFocus()
                            scrollToField("confirmPassword")
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
                                    Icons.Default.Visibility
                                else
                                    Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    isError = password.isNotEmpty() && password.length < 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("password" to coordinates.boundsInWindow().top)
                            passwordAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            autofill?.apply {
                                if (focusState.isFocused && passwordAutofillNode.boundingBox != null) {
                                    requestAutofillForNode(passwordAutofillNode)
                                } else {
                                    cancelAutofillForNode(passwordAutofillNode)
                                }
                            }
                        }
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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            firstNameFocusRequester.requestFocus()
                            scrollToField("firstName")
                        }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(confirmPasswordFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("confirmPassword" to coordinates.boundsInWindow().top)
                        }
                )

                if (!passwordsMatch) {
                    Text(
                        text = "Passwords do not match",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            lastNameFocusRequester.requestFocus()
                            scrollToField("lastName")
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(firstNameFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("firstName" to coordinates.boundsInWindow().top)
                            firstNameAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            autofill?.apply {
                                if (focusState.isFocused && firstNameAutofillNode.boundingBox != null) {
                                    requestAutofillForNode(firstNameAutofillNode)
                                } else {
                                    cancelAutofillForNode(firstNameAutofillNode)
                                }
                            }
                        }
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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            engineerNumberFocusRequester.requestFocus()
                            scrollToField("engineerNumber")
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(lastNameFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("lastName" to coordinates.boundsInWindow().top)
                            lastNameAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            autofill?.apply {
                                if (focusState.isFocused && lastNameAutofillNode.boundingBox != null) {
                                    requestAutofillForNode(lastNameAutofillNode)
                                } else {
                                    cancelAutofillForNode(lastNameAutofillNode)
                                }
                            }
                        }
                )


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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            phoneNumberFocusRequester.requestFocus()
                            scrollToField("phoneNumber")
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(engineerNumberFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("engineerNumber" to coordinates.boundsInWindow().top)
                        }
                )
                if (!engineerNumber.isNotEmpty() && engineerNumber.length < 5) {
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
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.clearFocus()
                            asmFocusRequester.requestFocus()
                            scrollToField("asm")
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(phoneNumberFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("phoneNumber" to coordinates.boundsInWindow().top)
                            phoneAutofillNode.boundingBox = coordinates.boundsInWindow()
                        }
                        .onFocusChanged { focusState ->
                            autofill?.apply {
                                if (focusState.isFocused && phoneAutofillNode.boundingBox != null) {
                                    requestAutofillForNode(phoneAutofillNode)
                                } else {
                                    cancelAutofillForNode(phoneAutofillNode)
                                }
                            }
                        }
                )

                // Country Dropdown


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
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(asmFocusRequester)
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("asm" to coordinates.boundsInWindow().top)
                        }
                )
                if (!asm.isNotEmpty() && !asm.contains("@")) {
                    Text(
                        text = "ASM must be a valid email address",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


                ExposedDropdownMenuBox(
                    expanded = countryExpanded,
                    onExpandedChange = { countryExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            fieldPositions = fieldPositions + ("country" to coordinates.boundsInWindow().top)
                        }
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
                        .height(60.dp), // Fixed height to prevent layout shift
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
                                    deactive = false
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