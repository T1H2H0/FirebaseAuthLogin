package com.uwonham.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.net.toUri
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException

import com.uwonham.firebaselogin.utils.SignInResult
import com.uwonham.firebaselogin.utils.SignInState
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "LibraryLoginViewModel"

data class UserData(
    val email: String = "",
    val password: String = "", // ADD PASSWORD FIELD
    val engineernumber: String = "",
    val country: String = "GB",
    val name: String = "",
    val photo: String = "",
    val phonenumber: String = "",
    val asm: String = "",
    val role: String = "NEWUSER",
    val deActivated: Boolean? = false,
    val deactive: Boolean? = false,
    val msgtoken:String? = "",
)

@HiltViewModel
class FirebaseLoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("AuthPrefs")
    private val sharedPreferences: SharedPreferences,
     sampleString: String
): ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()
    private val _signInResult = MutableLiveData<SignInResult>()
    val signInResult: LiveData<SignInResult> get() = _signInResult
    private val credentialManager = CredentialManager.create(context)
    private val firestore = FirebaseFirestore.getInstance()

    // Add allowed domain property
    private var allowedEmailDomain: String = ""

    // Add create account result
    private val _createAccountResult = MutableLiveData<SignInResult>()
    val createAccountResult: LiveData<SignInResult> get() = _createAccountResult

    init {
        Log.d(TAG, "init:${sampleString} ")
    }

    fun setAuth(auth: com.google.firebase.auth.FirebaseAuth) {
        Log.d(TAG, "setAuth: ${auth.firebaseAuthSettings}")
        _state.update { it.copy(auth = auth) }
    }

    fun setAllowedEmailDomain(domain: String) {
        allowedEmailDomain = domain
    }

    fun checkAccountExists(email: String) {
        viewModelScope.launch {
            try {
                // Simply attempt to sign in with a dummy password
                _state.value.auth?.signInWithEmailAndPassword(email, "dummyPassword")?.await()

                // If we reach here, somehow the dummy password worked (very unlikely)
                // Sign out immediately and consider account exists
                _state.value.auth?.signOut()

                _state.update {
                    it.copy(
                        accountExists = false,
                        isLoading = true,
                        errorMessage = null
                    )
                }

            } catch (e: FirebaseAuthException) {
                // Handle FirebaseAuthException with error codes
                when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> {
                        // Account doesn't exist
                        Log.d(TAG, "Account does not exist for email: $email")
                        _state.update {
                            it.copy(
                                accountExists = false,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    "ERROR_WRONG_PASSWORD" -> {
                        _state.update {
                        it.copy(
                            accountExists = true,
                            isLoading = false,
                            errorMessage = null
                        )
                    }}
                    "ERROR_INVALID_CREDENTIAL" -> {
                        // Account exists but wrong password (expected)
                        Log.d(TAG, "Account exists for email: $email")
                        _state.update {
                            it.copy(
                                accountExists = true,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                    "ERROR_TOO_MANY_REQUESTS" -> {
                        // Rate limited - assume account exists to be safe
                        Log.w(TAG, "Too many requests, assuming account exists")
                        _state.update {
                            it.copy(
                                accountExists = true,
                                errorMessage = "Too many attempts. Please try again later."
                            )
                        }
                    }
                    else -> {
                        // Other errors - assume account doesn't exist
                        Log.e(TAG, "Unexpected error checking account: ${e.errorCode} - ${e.message}")
                        _state.update {
                            it.copy(
                                accountExists = false,
                                isLoading = false,
                                errorMessage = "Unable to verify account: ${e.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Catch any other exceptions
                Log.e(TAG, "Error checking account existence", e)
                _state.update {
                    it.copy(
                        accountExists = false,
                        errorMessage = "Error checking account: ${e.message}"
                    )
                }
            }
        }
    }

    // Alternative using the newer error code approach
//    fun checkAccountExistsWithErrorCode(email: String) {
//        viewModelScope.launch {
//            try {
//                _state.value.auth?.signInWithEmailAndPassword(email, "dummyPassword")?.await()
//
//                // If successful, sign out and mark as existing
//                _state.value.auth?.signOut()
//                _state.update {
//                    it.copy(accountExists = true, errorMessage = null)
//                }
//
//            } catch (e: Exception) {
//                // Check error message content since error codes can vary
//                val errorMessage = e.message?.lowercase() ?: ""
//
//                when {
//                    errorMessage.contains("user not found") ||
//                            errorMessage.contains("no user record") ||
//                            errorMessage.contains("user_not_found") -> {
//                        // Account doesn't exist
//                        Log.d(TAG, "Account does not exist for email: $email")
//                        _state.update {
//                            it.copy(accountExists = false, errorMessage = null)
//                        }
//                    }
//                    errorMessage.contains("wrong password") ||
//                            errorMessage.contains("invalid credential") ||
//                            errorMessage.contains("password is invalid") -> {
//                        // Account exists but wrong password
//                        Log.d(TAG, "Account exists for email: $email")
//                        _state.update {
//                            it.copy(accountExists = true, errorMessage = null)
//                        }
//                    }
//                    errorMessage.contains("too many requests") -> {
//                        // Rate limited
//                        _state.update {
//                            it.copy(
//                                accountExists = true,
//                                errorMessage = "Too many attempts. Please try again later."
//                            )
//                        }
//                    }
//                    else -> {
//                        Log.e(TAG, "Unexpected error: ${e.message}")
//                        _state.update {
//                            it.copy(
//                                accountExists = false,
//                                errorMessage = "Unable to verify account"
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }


    // FIXED: Create account function with proper error handling and validation
    fun createAccount( userData: UserData) {
        Log.d(TAG, "createAccount: Starting account creation for ${userData.email}")

        // Validate inputs
        if (userData.email.isEmpty()) {
            _createAccountResult.postValue(SignInResult.Error("Email cannot be empty"))
            return
        }

        if (userData.password.isEmpty()) {
            _createAccountResult.postValue(SignInResult.Error("Password cannot be empty"))
            return
        }

        if (userData.password.length < 6) {
            _createAccountResult.postValue(SignInResult.Error("Password must be at least 6 characters"))
            return
        }

        if (!shouldCreateUser(userData.email)) {
            _createAccountResult.postValue(SignInResult.Error("Email domain not allowed for account creation"))
            return
        }

        // Check if auth is initialized
        if (_state.value.auth == null) {
            _createAccountResult.postValue(SignInResult.Error("Firebase Auth not initialized"))
            return
        }

        _state.update { it.copy(isLoading = true, errorMessage = null) }
        _createAccountResult.postValue(SignInResult.Loading)

        viewModelScope.launch {
            try {
                Log.d(TAG, "createAccount: Creating Firebase Auth user")

                // Create Firebase Auth user with proper await
                val authResult = _state.value.auth!!.createUserWithEmailAndPassword(
                    userData.email,
                    userData.password
                ).await()

                val user = authResult.user
                if (user != null) {
                    Log.d(TAG, "createAccount: Firebase Auth user created successfully")
                    sendEmailVerification(user)
                    // Update display name if provided
                    if (userData.name.isNotEmpty()) {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(userData.name)
                            .setPhotoUri(generateAutoPhoto(userData.email).toUri())
                            .build()

                        try {
                            user.updateProfile(profileUpdates).await()
                            Log.d(TAG, "User profile updated successfully")

                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to update user profile: ${e.message}")
                            // Don't fail the entire operation for profile update failure
                        }
                    }

                    // Create Firestore user document
                    try {
                        createFirestoreUser(user, userData)
                        Log.d(TAG, "createAccount: Account creation completed successfully")
                        _state.update { it.copy(isLoading = false) }
                        _createAccountResult.postValue(SignInResult.Success(user = user))
                    } catch (e: Exception) {
                        Log.e(TAG, "createAccount: Failed to create Firestore document", e)
                        _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                        _createAccountResult.postValue(SignInResult.Error("Account created but profile setup failed: ${e.message}"))
                    }
                } else {
                    Log.e(TAG, "createAccount: User is null after creation")
                    _state.update { it.copy(isLoading = false) }
                    _createAccountResult.postValue(SignInResult.Error("Account creation failed - user is null"))
                }

            } catch (e: Exception) {
                Log.e(TAG, "createAccount: Exception during account creation", e)
                _state.update { it.copy(isLoading = false, errorMessage = e.message) }
                _createAccountResult.postValue(SignInResult.Error(e.message ?: "Account creation failed"))
            }
        }
    }

     fun sendEmailVerification(user: FirebaseUser) {
        if (!user.isEmailVerified) {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Log.d(TAG, "sendEmailVerification: Verification email sent to ${user.email}")
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage ="Verification email sent. Please check your inbox and verify your email address before logging in. This can take up to 10 minutes."
                        )
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "sendEmailVerification: Failed to send verification email", e)
                    _state.update { it.copy(isLoading = false, errorMessage = "Failed to send verification email: ${e.message}") }
                }
        } else {
            Log.d(TAG, "sendEmailVerification: Email already verified for ${user.email}")
        }
    }

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password) }
    }

    fun setRememberCredentials(rememberCredentials: Boolean) {
        _state.update { it.copy(rememberCredentials = rememberCredentials) }
    }

    fun signIn(activity: Activity) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                _signInResult.postValue(SignInResult.Loading)

                _state.value.auth?.signInWithEmailAndPassword(_state.value.email, _state.value.password)
                    ?.addOnSuccessListener { authResult ->
                        _state.update { it.copy(isLoading = false) }

                        // Check if user should be created based on email domain
                        val user = authResult.user
                        if (user?.isEmailVerified == false) {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = "User not verified please check your email.\nThis can take up to 10 minutes for you to receive the verification email."
                                )
                            }
                        }
//                        if (user != null && shouldCreateUser(user.email)) {
//                            viewModelScope.launch {
//                                createUserIfNotExists(user)
//                            }
//                        }
else{
                        if (_state.value.rememberCredentials != false) {
                            viewModelScope.launch {
                                saveCredentials(
                                    activity,
                                    _state.value.email,
                                    _state.value.password
                                )
                                _signInResult.postValue(SignInResult.Success(user = user))
                            }
                        } else {
                            _signInResult.postValue(SignInResult.Success(user = user))
                        }
                    }
                    }
                    ?.addOnFailureListener { error ->
                        _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                        _signInResult.postValue(SignInResult.Error(error.message ?: "Sign in failed"))
                    }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _signInResult.postValue(SignInResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun shouldCreateUser(email: String?): Boolean {
        val result = email != null &&
                allowedEmailDomain.isNotEmpty() &&
                email.endsWith(allowedEmailDomain)
        Log.d(TAG, "shouldCreateUser: email=$email, domain=$allowedEmailDomain, result=$result")
        return result
    }

    private suspend fun createFirestoreUser(user: FirebaseUser, userData: UserData) {
        try {
            val userEmail = user.email ?: throw Exception("User email is null")

            Log.d(TAG, "createFirestoreUser: Creating document for $userEmail")

            // Ensure we're authenticated before accessing Firestore
            if (_state.value.auth?.currentUser == null) {
                throw Exception("User not authenticated, cannot create Firestore document")
            }
            val fcmToken = getFcmTokenWithAwait()
            val userDocRef = firestore.collection("Users").document(userEmail)

            // Create user document with provided data (excluding password for security)
            val finalUserData = userData.copy(
                email = userEmail,
                password = "", // Don't store password in Firestore
                photo = if (userData.photo.isEmpty()) generateAutoPhoto(userEmail) else userData.photo,
                msgtoken = fcmToken,

            )

            userDocRef.set(finalUserData).await()
            Log.d(TAG, "createFirestoreUser: User document created successfully for: $userEmail")

        } catch (e: Exception) {
            Log.e(TAG, "createFirestoreUser: Error creating user document", e)
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "Permission denied - check Firebase security rules")
                throw Exception("Permission denied - please check your Firebase security rules")
            }
            throw e
        }
    }
    private suspend fun getFcmTokenWithAwait(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Fetching FCM registration token failed", e)
            ""
        }
    }
//    private suspend fun createUserIfNotExists(user: FirebaseUser) {
//        try {
//            val userEmail = user.email ?: return
//
//            Log.d(TAG, "createUserIfNotExists: Checking if user exists: $userEmail")
//
//            // Ensure we're authenticated before accessing Firestore
//            if (_state.value.auth?.currentUser == null) {
//                Log.w(TAG, "User not authenticated, cannot create Firestore document")
//                return
//            }
//
//            val userDocRef = firestore.collection("Users").document(userEmail)
//
//            // Check if user document already exists
//            val docSnapshot = userDocRef.get().await()
//
//            if (!docSnapshot.exists()) {
//                Log.d(TAG, "createUserIfNotExists: Creating new user document")
//                // Create new user document with updated structure
//                val userData = UserData(
//                    email = userEmail,
//                    password = "", // Don't store password
//                    engineernumber = "", // You can set this based on your logic
//                    country = "GB",
//                    name = user.displayName ?: "", // Get from Firebase Auth if available
//                    photo = user.photoUrl.toString(), // Auto-generate if empty
//                    phonenumber = "",
//                    asm = "", // Set based on your requirements
//                    role = "NEWUSER",
//                    deActivated = false,
//                    deactive = false
//                )
//
//                userDocRef.set(userData).await()
//                Log.d(TAG, "createUserIfNotExists: User document created for: $userEmail")
//            } else {
//                Log.d(TAG, "createUserIfNotExists: User document already exists for: $userEmail")
//            }
//
//        } catch (e: Exception) {
//            Log.e(TAG, "createUserIfNotExists: Error creating user document", e)
//            // Additional error handling for permission issues
//            if (e.message?.contains("PERMISSION_DENIED") == true) {
//                Log.e(TAG, "Permission denied - check Firebase security rules")
//            }
//        }
//    }

    private fun generateAutoPhoto(email: String): String {

            if (email.isEmpty()) {
                return "https://api.dicebear.com/7.x/initials/svg?seed=unknown&size=150"
            }

            // Use email as seed for consistent avatars
        val emailSplit = email.split("_",".",ignoreCase=true,limit = 2)
            val seed =emailSplit.first().first().toString() + emailSplit.last().first().toString()
            return "https://api.dicebear.com/7.x/initials/svg?seed=$seed&size=150"
        }

    suspend fun saveCredentials(activity: Activity, username: String, password: String) {
        try {
            val request = CreatePasswordRequest(
                id = username,
                password = password
            )

            credentialManager.createCredential(
                request = request,
                context = activity
            )

            // Mark that we have saved credentials
            sharedPreferences
                .edit()
                .putBoolean("has_saved_credentials", true)
                .apply()
            Log.d(
                TAG,
                "saveCredentials: ${
                    sharedPreferences.getBoolean(
                        "has_saved_credentials",
                        false
                    )
                }"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error saving credentials: $e")
        }
    }

    fun loadSavedCredentials(activity: Activity) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val getPasswordOption = GetPasswordOption()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(getPasswordOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential

                if (credential is androidx.credentials.PasswordCredential) {
                    _state.update {
                        it.copy(
                            email = credential.id,
                            password = credential.password,
                            isLoading = false
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading saved credentials: $e")
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not retrieve saved credentials"
                    )
                }
            }
        }
    }

    fun checkForSavedCredentials() {
        val hasSavedCredentials = sharedPreferences.getBoolean("has_saved_credentials", false)
        _state.update { it.copy(useSavedCredentials = hasSavedCredentials) }
        Log.d(TAG, "checkForSavedCredentials: $hasSavedCredentials")
    }

    fun sendPasswordResetEmail() {
        // Clear previous error
        _state.update { it.copy(errorMessage = null, isLoading = true) }

        _state.value.auth?.sendPasswordResetEmail(state.value.email)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Password reset email sent successfully
                    _state.update {
                        it.copy(
                            errorMessage = "Password reset email sent. Check your inbox.",
                            isLoading = false
                        )
                    }
                } else {
                    // If sending email fails
                    _state.update {
                        it.copy(
                            errorMessage = task.exception?.message ?: "Failed to send reset email",
                            isLoading = false
                        )
                    }
                }
            }
    }
}