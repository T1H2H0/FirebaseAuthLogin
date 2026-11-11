package com.uwonham.firebaselogin.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidationTest {

    @Test
    fun normalise_trimsAndLowercases() {
        assertEquals("john.smith@beko.com", EmailValidation.normalise("  John.Smith@Beko.com \t"))
    }

    @Test
    fun accountNumberAddresses_areDetected() {
        assertTrue(EmailValidation.isAccountNumber("93604552@beko.com"))
        assertTrue(EmailValidation.isAccountNumber(" C9939333@beko.com "))
        assertTrue(EmailValidation.isAccountNumber("c9939333@beko.com"))
    }

    @Test
    fun namedAddresses_areAllowed() {
        assertFalse(EmailValidation.isAccountNumber("john.smith@beko.com"))
        assertFalse(EmailValidation.isAccountNumber("john.smith2@beko.com"))
        assertFalse(EmailValidation.isAccountNumber("ab123456@beko.com"))
    }
}
