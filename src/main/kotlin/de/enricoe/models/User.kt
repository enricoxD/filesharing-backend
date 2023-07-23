package de.enricoe.models

import de.enricoe.api.responses.ForeignUserResponse
import de.enricoe.api.responses.UserResponse
import de.enricoe.database.MongoManager
import de.enricoe.security.Crypto
import de.enricoe.utils.getCurrentDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

enum class Role {
    ADMIN, STAFF, PREMIUM, NORMAL
}

@Serializable
data class User(
    @SerialName("_id") val id: String,
    var name: String,
    var email: String,
    var password: String,
    val createdAt: LocalDateTime,
    var lastSeen: LocalDateTime,
    var emailVerified: Boolean = false,
    var apiKey: String? = null,
    var role: Role = Role.NORMAL,
) {

    companion object {
        suspend fun register(credentials: UserRegistrationCredentials): User {
            val currentTime = getCurrentDate()
            val user = User(
                id = getFreeId(),
                credentials.username,
                credentials.email,
                Crypto.hashPassword(credentials.password),
                createdAt = currentTime,
                lastSeen = currentTime,
            )
            MongoManager.users.insertOne(user)
            return user
        }

        suspend fun getByEmail(email: String): User? {
            return MongoManager.users.findOne(User::email eq email)
        }

        suspend fun getById(id: String): User? {
            return MongoManager.users.findOne(User::id eq id)
        }

        suspend fun getByUsername(username: String): User? {
            return MongoManager.users.findOne(User::name eq username)
        }


        private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        private suspend fun getFreeId(): String {
            var id = buildString {
                repeat(10) {
                    append(charPool.random())
                }
            }
            if (MongoManager.users.findOne(User::id eq id) != null)
               id = getFreeId()
            return id
        }
    }

    fun asResponse() = UserResponse(id, name, email, createdAt, lastSeen, emailVerified, role)
    fun asForeignResponse() = ForeignUserResponse(id, name, createdAt, lastSeen, role)

}

@Serializable
data class UserRegistrationCredentials(
    val username: String,
    val password: String,
    val email: String
)