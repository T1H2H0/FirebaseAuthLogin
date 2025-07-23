package com.uwonham.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uwonham.firebaselogin.utils.SignInResult
import com.uwonham.firebaselogin.utils.SignInState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseUser

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
    val engineerNumber: String = "",
    val country:String = "GB",
    val name: String = "",
    val photo: String = "",
    val phonenumber:String = "",
    val asm: String = "",
    val role: String = "NEWUSER",
    val deActivated: Boolean? = false,
    val deactive: Boolean? = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("AuthPrefs")
    private val sharedPreferences: SharedPreferences,
    var sampleString: String
): ViewModel() {
    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()
    private val _signInResult = MutableLiveData<SignInResult>()
    val signInResult: LiveData<SignInResult> get() = _signInResult
    private val credentialManager = CredentialManager.create(context)
    private val firestore = FirebaseFirestore.getInstance()

    // Add allowed domain property
    private var allowedEmailDomain: String = ""

    init {
        Log.d(TAG, "init:${sampleString} ")
    }

    fun setAuth(auth: com.google.firebase.auth.FirebaseAuth) {
        _state.update { it.copy(auth = auth) }
    }

    fun setAllowedEmailDomain(domain: String) {
        allowedEmailDomain = domain
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
        viewModelScope.launch() {
            try {
                _signInResult.postValue(SignInResult.Loading)

                _state.value.auth?.signInWithEmailAndPassword(_state.value.email, _state.value.password)
                    ?.addOnSuccessListener { authResult ->
                        _state.update { it.copy(isLoading = false) }

                        // Check if user should be created based on email domain
                        val user = authResult.user
                        if (user != null && shouldCreateUser(user.email)) {
                            viewModelScope.launch {
                                createUserIfNotExists(user)
                            }
                        }

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
                    ?.addOnFailureListener { error ->
                        _state.update { it.copy(isLoading = false, errorMessage = error.message) }
                    }

            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _signInResult.postValue(SignInResult.Error(e.message ?: "Unknown error"))
            }
        }
    }

    private fun shouldCreateUser(email: String?): Boolean {
        return email != null &&
                allowedEmailDomain.isNotEmpty() &&
                email.endsWith(allowedEmailDomain)
    }

    private suspend fun createUserIfNotExists(user: FirebaseUser) {
        try {
            val userEmail = user.email ?: return

            // Ensure we're authenticated before accessing Firestore
            if (_state.value.auth?.currentUser == null) {
                Log.w(TAG, "User not authenticated, cannot create Firestore document")
                return
            }

            val userDocRef = firestore.collection("Users").document(userEmail)

            // Check if user document already exists
            val docSnapshot = userDocRef.get().await()

            if (!docSnapshot.exists()) {
                // Create new user document
                val userData = UserData(
                    email = userEmail,
                    deActivated =false,
                    engineerNumber = "", // You can set this based on your logic
                    name = user.displayName ?: "", // Get from Firebase Auth if available
                    photo = generateAutoPhoto(userEmail), // Auto-generate if empty
                    asm = "", // Set based on your requirements
                    role = "newuser"
                )

                userDocRef.set(userData).await()
                Log.d(TAG, "User document created for: $userEmail")
            } else {
                Log.d(TAG, "User document already exists for: $userEmail")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating user document: ${e.message}")
            // Additional error handling for permission issues
            if (e.message?.contains("PERMISSION_DENIED") == true) {
                Log.e(TAG, "Permission denied - check Firebase security rules")
            }
        }
    }

    private fun generateAutoPhoto(email: String): String {
        // Auto-generate photo URL if empty
        // You can implement your own logic here, for example:
        // - Use a default avatar service like Gravatar
        // - Generate initials-based avatar
        // - Use a placeholder image

        val emailHash = email.hashCode().toString()
        return "https://via.placeholder.com/150x150.png?text=${email.first().uppercase()}"
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

    fun sendPasswordResetEmail(activity: Activity) {
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