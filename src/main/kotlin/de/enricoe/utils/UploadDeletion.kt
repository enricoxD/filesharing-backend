package de.enricoe.utils

import de.enricoe.database.MongoManager
import de.enricoe.models.Upload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import org.litote.kmongo.eq
import org.litote.kmongo.gte
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

object UploadDeletion {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init() {
        scope.launch {
            MongoManager.uploads.find(Upload::deleteAt gte getCurrentDate()).forEach { upload ->
                deleteUpload(upload)
            }

            while (true) {
                val lastDateToDelete = getCurrentDate().toJavaLocalDateTime().plusHours(3).toKotlinLocalDateTime()
                scope.launch {
                    MongoManager.uploads.find(Upload::deleteAt gte lastDateToDelete).forEach { upload ->
                        scope.launch {
                            val currentOffset = ZoneOffset.from(getCurrentDate().toJavaLocalDateTime())
                            val deleteAtOffset = ZoneOffset.from(upload.deleteAt.toJavaLocalDateTime())
                            val offsetDifference = deleteAtOffset.totalSeconds - currentOffset.totalSeconds
                            delay(offsetDifference.seconds)
                            deleteUpload(upload)
                        }
                    }
                }
                delay(3.hours)
            }
        }
    }

    private fun deleteUpload(upload: Upload) {
        MongoManager.uploads.deleteOne(Upload::id eq upload.id)
        upload.files.forEach { fileUpload ->
            val file = fileUpload.asFile()
            file.delete()
        }
    }
}