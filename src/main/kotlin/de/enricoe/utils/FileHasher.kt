package de.enricoe.utils

import kotlinx.datetime.LocalDateTime
import java.io.ByteArrayInputStream
import java.security.MessageDigest

object FileHasher {

    fun hash(inputStream: ByteArrayInputStream): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(inputStream.readBytes())
        inputStream.close()
        val hashBytes = md.digest()

        return buildString {
            hashBytes.forEach { byte ->
                append(String.format("%02x", byte))
            }
        }
    }

    fun hashWithDate(inputStream: ByteArrayInputStream, date: LocalDateTime): String {
        val hash = hash(inputStream)

        return buildString {
            append(date.year)
            append(date.monthNumber.toString().padStart(2, '0'))
            append(date.dayOfMonth.toString().padStart(2, '0'))
            val minutes = date.minute + date.hour * 60
            val seconds = date.second + minutes * 60
            append(seconds.toString().padStart(5, '0'))
            append(hash)
        }
    }
}