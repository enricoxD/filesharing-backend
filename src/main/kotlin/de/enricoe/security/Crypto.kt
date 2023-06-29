package de.enricoe.security

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordValidationResponse(
    val minChars: Boolean,
    val lowercase: Boolean,
    val uppercase: Boolean,
    val digit: Boolean,
    val noSpace: Boolean,

    val isValid: Boolean = minChars && lowercase && uppercase && digit && noSpace
)

object Crypto {
    private val Hasher = BCrypt.with(BCrypt.Version.VERSION_2X)
    private val Verifier = BCrypt.verifyer(BCrypt.Version.VERSION_2X)

    fun hashPassword(password: String): String {
        return Hasher.hashToString(12, password.toCharArray())
    }

    fun verifyPassword(password: String, storedHash: String): BCrypt.Result {
        return Verifier.verify(password.toCharArray(), storedHash)
    }

    fun validatePassword(password: String): PasswordValidationResponse {
        val minChars = password.length >= 8
        val lowercase = password.any { it.isLowerCase() }
        val uppercase = password.any { it.isUpperCase() }
        val digit = password.any { it.isDigit() }
        val noSpace = !password.contains(' ')
        return PasswordValidationResponse(minChars, lowercase, uppercase, digit, noSpace)
    }
}