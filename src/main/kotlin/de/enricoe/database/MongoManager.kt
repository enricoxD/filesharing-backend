package de.enricoe.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoCollection
import de.enricoe.models.Upload
import de.enricoe.models.User
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import java.util.logging.Logger

object MongoManager {
    /*private val client = KMongo.createClient(settings = MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .credential(
                MongoCredential.createCredential(
                    System.getenv("MONGODB_USER"),
                    System.getenv("MONGODB_DATABASE"),
                    System.getenv("MONGODB_PASSWORD").toCharArray()
                ))
            .applyToClusterSettings { it.hosts(listOf(ServerAddress(System.getenv("MONGODB_HOST"), System.getenv("MONGODB_PORT").toIntOrNull() ?: 27017))) }
            .build()
        )
        val database = client.getDatabase(System.getenv("MONGODB_DATABASE"))*/

    //val client = KMongo.createClient("mongodb://root:root@mongodb/admin")
    val client = KMongo.createClient(
        MongoClientSettings.builder()
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyConnectionString(ConnectionString("mongodb://root:root@mongodb/admin")).build()
    )

    val database = client.getDatabase("filesharing")

    fun init() {
        runCatching {
            System.setProperty(
                "org.litote.mongo.test.mapping.service",
                "org.litote.kmongo.serialization.SerializationClassMappingTypeService"
            )
        }
    }


    fun disconnect() = runCatching { client.close() }

    val users: MongoCollection<User>
    val uploads: MongoCollection<Upload>

    init {
        runCatching {
            database.createCollection("users")
            database.createCollection("uploads")
        }
        users = database.getCollection<User>("users")
        uploads = database.getCollection<Upload>("uploads")
    }
}