package de.enricoe.utils

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
}