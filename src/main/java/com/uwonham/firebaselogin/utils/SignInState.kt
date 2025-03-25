package com.uwonham.firebaselogin.utils

import com.google.firebase.auth.FirebaseAuth

data class SignInState(
    val auth: FirebaseAuth? = null,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val useSavedCredentials: Boolean = false,
    val rememberCredentials: Boolean = false
)