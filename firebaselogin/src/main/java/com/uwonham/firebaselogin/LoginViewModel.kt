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

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "LibraryLoginViewModel"
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

    init {
        Log.d(TAG, "init:${sampleString} ")
    }
fun setAuth(auth: com.google.firebase.auth.FirebaseAuth) {
    _state.update { it.copy(auth = auth) }
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
                    ?.addOnSuccessListener {
                        _state.update { it.copy(isLoading = false) }



                               if (_state.value.rememberCredentials != false){
                                   viewModelScope.launch {
                                       saveCredentials(
                                           activity,
                                           _state.value.email,
                                           _state.value.password
                                       )
                                       _signInResult.postValue( SignInResult.Success(user = it.user))
                                   }

                    }else {
                                   _signInResult.postValue(SignInResult.Success(user = it.user))
                               }
                    }
                    ?.addOnFailureListener {error->
                        _state.update { it.copy(isLoading = false, errorMessage = error.message) }

//
                    }


    }catch (e: Exception) {

        _state.update { it.copy(isLoading = false) }
        _signInResult.postValue(SignInResult.Error(e.message ?: "Unknown error"))
    }

            }    }

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
        Log.d(TAG, "checkForSavedCredentials:  $hasSavedCredentials")

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