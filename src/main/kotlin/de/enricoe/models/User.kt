package de.enricoe.models

import de.enricoe.database.MongoManager
import de.enricoe.security.Crypto
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import java.nio.file.Path

@Serializable
data class User(
    @SerialName("_id") val id: String,
    var username: String,
    var email: String,
    var password: String,
    val createdAt: LocalDateTime,
    var lastSeen: LocalDateTime,
    var avatar: Path? = null,
    var authToken: String? = null,
    var emailVerified: Boolean = false,
    var apiKey: String? = null
) {

    companion object {
        suspend fun register(credentials: UserRegistrationCredentials): User {
            println(1)
            val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            println(2)
            val user = User(
                id = getFreeId(),
                credentials.username,
                credentials.email,
                Crypto.hashPassword(credentials.password),
                createdAt = currentTime,
                lastSeen = currentTime,
            )
            println(3)
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
            return MongoManager.users.findOne(User::username eq username)
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
}

@Serializable
data class UserRegistrationCredentials(
    val username: String,
    val password: String,
    val email: String
)