package com.uwonham.firebaselogin

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.uwonham.firebaselogin.utils.EmailValidation
import com.uwonham.firebaselogin.utils.SignInResult
import com.uwonham.firebaselogin.utils.SignInState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "LibraryLoginViewModel"

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("AuthPrefs")
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SignInState())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    /** Kept for backwards compatibility. Note: LiveData re-delivers the last value to new observers. */
    private val _signInResult = MutableLiveData<SignInResult>()
    val signInResult: LiveData<SignInResult> get() = _signInResult

    /** One-shot sign-in events — each result is delivered once. Used by the dialog. */
    private val _events = Channel<SignInResult>(Channel.BUFFERED)
    val events: Flow<SignInResult> = _events.receiveAsFlow()

    private val credentialManager = CredentialManager.create(context)

    private var emailValidator: (String) -> String? = EmailValidation.default

    fun setAuth(auth: FirebaseAuth) {
        _state.update { it.copy(auth = auth) }
    }

    fun setEmailValidator(validator: (String) -> String?) {
        emailValidator = validator
    }

    fun updateEmail(email: String) {
        // Don't trim here — trimming on every keystroke fights the cursor. Clear any shown error.
        _state.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    /** Called when the email field loses focus: tidy the value and show any validation error. */
    fun onEmailFocusLost() {
        val email = EmailValidation.normalise(_state.value.email)
        _state.update { it.copy(email = email, emailError = emailValidator(email)) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password, errorMessage = null) }
    }

    fun setRememberCredentials(rememberCredentials: Boolean) {
        _state.update { it.copy(rememberCredentials = rememberCredentials) }
    }

    fun signIn(activity: Activity) {
        val email = EmailValidation.normalise(_state.value.email)
        val password = _state.value.password
        val emailError = emailValidator(email)

        _state.update { it.copy(email = email, emailError = emailError, errorMessage = null) }
        if (emailError != null) return

        val auth = _state.value.auth
        if (auth == null) {
            publish(SignInResult.Error("Sign-in is not configured (FirebaseAuth not set)"))
            _state.update { it.copy(errorMessage = "Sign-in is not available. Please try again.") }
            return
        }

        _state.update { it.copy(isLoading = true) }
        publish(SignInResult.Loading)

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                viewModelScope.launch {
                    if (_state.value.rememberCredentials) {
                        saveCredentials(activity, email, password)
                    }
                    _state.update { it.copy(isLoading = false) }
                    if (user != null) {
                        publish(SignInResult.Success(user = user))
                    } else {
                        val message = "Sign-in failed. Please try again."
                        _state.update { it.copy(errorMessage = message) }
                        publish(SignInResult.Error(message))
                    }
                }
            }
            .addOnFailureListener { error ->
                val message = friendlyMessage(error)
                Log.w(TAG, "signIn failed", error)
                _state.update { it.copy(isLoading = false, errorMessage = message) }
                publish(SignInResult.Error(message, error))
            }
    }

    private fun publish(result: SignInResult) {
        _signInResult.postValue(result)
        _events.trySend(result)
    }

    private fun friendlyMessage(error: Exception): String = when (error) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password"
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait and try again."
        is FirebaseNetworkException -> "No connection. Check your network and try again."
        else -> error.message ?: "Sign-in failed. Please try again."
    }

    suspend fun saveCredentials(activity: Activity, username: String, password: String) {
        try {
            val request = CreatePasswordRequest(id = username, password = password)
            credentialManager.createCredential(request = request, context = activity)

            sharedPreferences
                .edit()
                .putBoolean("has_saved_credentials", true)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving credentials: $e")
        }
    }

    fun loadSavedCredentials(activity: Activity) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(GetPasswordOption())
                    .build()

                val credential = credentialManager.getCredential(
                    request = request,
                    context = activity
                ).credential

                if (credential is PasswordCredential) {
                    _state.update {
                        it.copy(
                            email = EmailValidation.normalise(credential.id),
                            password = credential.password,
                            emailError = null,
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
    }
}
