package de.enricoe.security

import at.favre.lib.crypto.bcrypt.BCrypt

class PasswordValidationResponse(
    private val minChars: Boolean,
    private val lowercase: Boolean,
    private val uppercase: Boolean,
    private val digit: Boolean,
    private val noSpace: Boolean,

    val isValid: Boolean = minChars && lowercase && uppercase && digit && noSpace
) {
    val exception = if (isValid) null
    else buildString {
        append("Your password needs to:<br/>")
        append("<ul>")
        if (!minChars) append("<li>be at least 8 characters long.</li>")
        if (!lowercase || !uppercase) append("<li>include both lower and upper case characters.</li>")
        if (!digit) append("<li>include at least one number.</li>")
        if (!noSpace) append("<li>not contain any spaces.</li>")
        append("</ul>")
    }
}

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