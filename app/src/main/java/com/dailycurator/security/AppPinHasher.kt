package com.dailycurator.security

import java.security.MessageDigest
import java.security.SecureRandom

object AppPinHasher {

    private val random = SecureRandom()

    fun randomSaltHex(byteCount: Int = 16): String {
        val bytes = ByteArray(byteCount)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(salt: String, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = (salt + "\u0000" + pin).toByteArray(Charsets.UTF_8)
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
