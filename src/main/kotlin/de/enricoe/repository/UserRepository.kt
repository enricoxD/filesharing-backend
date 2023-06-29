package de.enricoe.repository

import de.enricoe.database.MongoManager
import de.enricoe.models.User
import de.enricoe.models.UserRegistrationCredentials
import de.enricoe.security.Crypto
import de.enricoe.security.Jwt
import de.enricoe.utils.Response
import io.ktor.server.auth.*
import org.litote.kmongo.eq
import org.litote.kmongo.updateOne

object UserRepository {

    suspend fun register(credentials: UserRegistrationCredentials): Response<Any> {
        if (isEmailInUse(credentials.email)) return Response.Error(message = "Email already in use")
        if (isUsernameInUse(credentials.username)) return Response.Error(message = "Username already in use")
        /* TODO
        val passwordValidation = Crypto.validatePassword(credentials.password)
        if (!passwordValidation.isValid) {
            return Response.Error(exception = passwordValidation, message = "Password missmatch")
        }*/
        val user = User.register(credentials)
        user.authToken = Jwt.makeToken(user.id)
        return Response.Success(data = user, "Successfully created user '${credentials.username}'")
    }

    suspend fun login(credentials: UserPasswordCredential): Response<Any> {
        val user = User.getByUsername(credentials.name) ?: User.getByEmail(credentials.name) ?: return Response.Error(message = "Username or Password does not exist")
        val cryptoResult = Crypto.verifyPassword(credentials.password, user.password)
        if (!cryptoResult.verified) return Response.Error(message = "Username or Password does not exist")
        user.authToken = Jwt.makeToken(user.id)
        return Response.Success(data = user, "Successfully logged in")
    }

    suspend fun updateUsername(user: User, newUsername: String): Response<Any> {
        if (User.getByUsername(newUsername) != null) return Response.Error(message = "Username already in use")
        user.username = newUsername
        MongoManager.users.updateOne(User::id eq user.id, user)
        return Response.Success(data = user, "Successfully changed name to '$newUsername'")
    }

    suspend fun updateEmail(user: User, newEmail: String): Response<Any> {
        if (User.getByEmail(newEmail) != null) return Response.Error(message = "Email already in use")
        user.email = newEmail
        MongoManager.users.updateOne(User::id eq user.id, user)
        return Response.Success(data = user, "Successfully changed email to '$newEmail'")
    }

    suspend fun updatePassword(user: User, newPassword: String, currentPassword: String): Response<Any> {
        /* TODO
        val passwordValidation = Crypto.validatePassword(newPassword)
        if (!passwordValidation.isValid) {
            return Response.Error(exception = passwordValidation, message = "Password missmatch")
        }*/
        if (!Crypto.verifyPassword(currentPassword, user.password).verified) {
            return Response.Error(message = "Password missmatch")
        }
        user.password = Crypto.hashPassword(newPassword)
        MongoManager.users.updateOne(User::id eq user.id, user)
        return Response.Success(data = user, "Successfully changed password")
    }

    private suspend fun isEmailInUse(email: String): Boolean {
        return User.getByEmail(email) != null
    }

    private suspend fun isUsernameInUse(username: String): Boolean {
        return User.getByUsername(username) != null
    }
}