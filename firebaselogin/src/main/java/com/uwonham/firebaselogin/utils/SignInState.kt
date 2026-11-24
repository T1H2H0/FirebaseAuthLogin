package com.uwonham.firebaselogin.utils

import com.google.firebase.auth.FirebaseAuth

data class SignInState(
    val auth: FirebaseAuth? = null,
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Non-error notices, e.g. "Verification email sent". Not cleared by account checks. */
    val infoMessage: String? = null,
    val useSavedCredentials: Boolean = false,
    val rememberCredentials: Boolean = true,
    val accountExists: Boolean = false,
    /**
     * True only when Firebase said for certain whether [accountExists]. With email enumeration
     * protection on (the default for projects since 2023) a wrong password and an unknown email
     * give the same error, so the answer stays unknown and "Create account" stays available.
     */
    val accountCheckConclusive: Boolean = false,
)