package de.enricoe.repository

import de.enricoe.api.requests.UserUpdateData
import de.enricoe.database.MongoManager
import de.enricoe.models.User
import de.enricoe.models.UserRegistrationCredentials
import de.enricoe.security.Crypto
import de.enricoe.security.Jwt
import de.enricoe.utils.Response
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.litote.kmongo.eq
import org.litote.kmongo.setTo
import org.litote.kmongo.updateOne
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object UserRepository {

    suspend fun register(call: ApplicationCall, credentials: UserRegistrationCredentials): Response<Any> {
        if (isEmailInUse(credentials.email)) return Response.Error(exception = "Email already in use")
        if (isUsernameInUse(credentials.username)) return Response.Error(exception = "Username already in use")
        /* TODO
        val passwordValidation = Crypto.validatePassword(credentials.password)
        if (!passwordValidation.isValid) {
            return Response.Error(exception = passwordValidation, message = "Password missmatch")
        }*/
        val user = User.register(credentials)
        Jwt.appendCookies(call, Jwt.makeToken(user.id))
        return Response.Success(data = user, "Successfully created user '${credentials.username}'")
    }

    suspend fun login(call: ApplicationCall, credentials: UserPasswordCredential): Response<Any> {
        val user = User.getByUsername(credentials.name) ?: User.getByEmail(credentials.name) ?: return Response.Error(exception = "Username or Password does not exist")
        val cryptoResult = Crypto.verifyPassword(credentials.password, user.password)
        if (!cryptoResult.verified) return Response.Error(exception = "Username or Password does not exist")
        Jwt.appendCookies(call, Jwt.makeToken(user.id))
        return Response.Success(data = user, "Successfully logged in")
    }

    val allowedAvatarFileExtensions = setOf("png", "jpg", "jpeg", "webp")
    suspend fun updateUser(userId: String, multiPartData: MultiPartData): Response<Any> {
        val user = User.getById(userId) ?: run {
            return Response.Error(HttpStatusCode.BadRequest, exception = "The requesting User doesn't exist?")
        }
        val updateData = UserUpdateData()
        var exception: String? = null

        multiPartData.forEachPart { part ->
            if (exception != null) return@forEachPart
            when (part) {
                is PartData.FormItem -> {
                    val name = part.name
                    val value = part.value

                    when (name) {
                        "name" -> updateData.name = value
                        "email" -> updateData.email = value
                        "newPassword" -> updateData.newPassword = value
                        "currentPassword" -> updateData.currentPassword = value
                    }
                }

                is PartData.FileItem -> {
                    if (part.name != "avatar") return@forEachPart
                    val fileExtension = part.originalFileName?.split('.')?.last()
                    if (fileExtension !in allowedAvatarFileExtensions) {
                        exception = "This file type is not allowed as an avatar."
                        return@forEachPart
                    }
                    val directory = File("/avatars/").also { if (!it.exists()) it.mkdirs() }
                    directory.listFiles { file -> file.nameWithoutExtension == userId }?.forEach { it.delete() }
                    val targetFile = File(directory, "$userId.$fileExtension")
                    Files.copy(
                        ByteArrayInputStream(part.streamProvider().readBytes()),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }

                else -> {}
            }
            part.dispose()
        }

        if (updateData.email != null && isEmailInUse(updateData.email!!)) {
            return Response.Error(HttpStatusCode.Conflict, exception = "Email already in use")
        }
        if (updateData.name != null && isUsernameInUse(updateData.name!!)) {
            return Response.Error(HttpStatusCode.Conflict, exception = "Username already in use")
        }

        if (updateData.currentPassword == null || !Crypto.verifyPassword(updateData.currentPassword!!, user.password).verified) {
            return Response.Error(HttpStatusCode.Unauthorized, exception = "The provided password is not valid.")
        }

        if (exception != null) {
            return Response.Error(HttpStatusCode.NotAcceptable, exception = exception)
        }

        val updatedData = buildList {
            if (updateData.email != null) add(User::email setTo updateData.email!!)
            if (updateData.name != null) add(User::name setTo updateData.name!!)
            if (updateData.newPassword != null) add(User::password setTo Crypto.hashPassword(updateData.newPassword!!))
        }.toTypedArray()

        MongoManager.users.updateOne(User::id eq userId, *updatedData)
        return Response.Success()
    }


    suspend fun getAvatar(userId: String?): File {
        val directory = File("/avatars/").also { if (!it.exists()) it.mkdirs() }
        return directory.listFiles { file -> file.nameWithoutExtension == userId }?.singleOrNull()
            ?: File(directory, "unknown.webp")
    }

    private suspend fun isEmailInUse(email: String): Boolean {
        return User.getByEmail(email) != null
    }

    private suspend fun isUsernameInUse(username: String): Boolean {
        return User.getByUsername(username) != null
    }
}