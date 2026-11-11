package com.uwonham.firebaselogin.utils

import android.util.Patterns

/**
 * Email normalisation and validation used by [com.uwonham.firebaselogin.FirebaseSignInDialog].
 *
 * A validator returns `null` when the email is acceptable, or a user-facing error message.
 */
object EmailValidation {

    /** Local part that is an employee/account number, e.g. `93604552` or `C9939333`. */
    private val ACCOUNT_NUMBER = Regex("^[A-Za-z]?\\d{6,}$")

    const val ACCOUNT_NUMBER_MESSAGE =
        "Please use your named email address (e.g. firstname.lastname@…), not your employee number"

    const val INVALID_EMAIL_MESSAGE = "Enter a valid email address"

    /** Trims leading/trailing whitespace and lowercases. */
    fun normalise(email: String): String = email.trim().lowercase()

    /** True if the part before '@' looks like an employee/account number. */
    fun isAccountNumber(email: String): Boolean =
        ACCOUNT_NUMBER.matches(normalise(email).substringBefore('@'))

    /** Default rule: must be a valid email and must not be an account-number address. */
    val default: (String) -> String? = { raw ->
        val email = normalise(raw)
        when {
            email.isEmpty() -> null
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> INVALID_EMAIL_MESSAGE
            isAccountNumber(email) -> ACCOUNT_NUMBER_MESSAGE
            else -> null
        }
    }

    /** Use to switch validation off entirely. */
    val none: (String) -> String? = { null }
}
