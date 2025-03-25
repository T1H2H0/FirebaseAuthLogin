package com.uwonham.firebaselogin.utils

import androidx.activity.result.IntentSenderRequest
import com.google.firebase.auth.FirebaseUser

sealed class SignInResult {
    data class Success(val user: FirebaseUser? = null) : SignInResult()
    data class Error(val message: String, val exception: Exception? = null) : SignInResult()
    object Loading : SignInResult()
}