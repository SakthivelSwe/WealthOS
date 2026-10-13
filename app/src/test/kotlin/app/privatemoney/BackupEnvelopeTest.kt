package app.privatemoney

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupEnvelopeTest {

    private val magic = byteArrayOf(0x57, 0x4F, 0x53, 0x42) // "WOSB"
    private val currentVersion = 1

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 100_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(plainBytes: ByteArray, key: SecretKeySpec): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val cipherText = cipher.doFinal(plainBytes)
        return iv to cipherText
    }

    private fun decrypt(cipherText: ByteArray, iv: ByteArray, key: SecretKeySpec): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText)
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    @Test
    fun backupEnvelopeFramingAndDecryptionSucceeds() {
        val pin = "1234"
        val payloadJson = """{"accounts":[{"id":"acc_1","name":"Main Bank"}],"txns":[{"id":"txn_1","amountMinor":150000}]}"""
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val checksum = sha256(payloadBytes)

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pin, salt)
        val (iv, ciphertext) = encrypt(payloadBytes, key)

        val metadataJson = """{"version":$currentVersion,"timestamp":1700000000,"checksumSha256":"$checksum","accountsCount":1,"txnsCount":1}"""
        val metaBytes = metadataJson.toByteArray(Charsets.UTF_8)

        // Assemble binary envelope matching CloudSyncService
        val totalSize = 4 + 4 + 4 + metaBytes.size + 16 + 12 + ciphertext.size
        val buffer = ByteBuffer.allocate(totalSize).apply {
            put(magic)
            putInt(currentVersion)
            putInt(metaBytes.size)
            put(metaBytes)
            put(salt)
            put(iv)
            put(ciphertext)
        }
        val envelopeBytes = buffer.array()

        // Unpack envelope
        val unpack = ByteBuffer.wrap(envelopeBytes)
        val readMagic = ByteArray(4).also { unpack.get(it) }
        assertTrue(readMagic.contentEquals(magic))

        val version = unpack.int
        assertEquals(1, version)

        val metaLen = unpack.int
        val readMetaBytes = ByteArray(metaLen).also { unpack.get(it) }
        val readMetaString = String(readMetaBytes, Charsets.UTF_8)
        assertTrue(readMetaString.contains(checksum))

        val readSalt = ByteArray(16).also { unpack.get(it) }
        val readIv = ByteArray(12).also { unpack.get(it) }
        val readCipher = ByteArray(unpack.remaining()).also { unpack.get(it) }

        // Decrypt
        val recoveredKey = deriveKey(pin, readSalt)
        val decryptedBytes = decrypt(readCipher, readIv, recoveredKey)

        assertEquals(checksum, sha256(decryptedBytes))
        assertEquals(payloadJson, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun wrongPinFailsDecryption() {
        val correctPin = "1234"
        val wrongPin = "9999"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val key = deriveKey(correctPin, salt)
        val (iv, ciphertext) = encrypt("secret ledger data".toByteArray(), key)

        val wrongKey = deriveKey(wrongPin, salt)
        decrypt(ciphertext, iv, wrongKey)
    }

    @Test(expected = Exception::class)
    fun tamperedCiphertextIsRejectedByGCM() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pin, salt)
        val (iv, ciphertext) = encrypt("authentic data".toByteArray(), key)

        val corrupted = ciphertext.copyOf()
        corrupted[corrupted.size - 1] = (corrupted[corrupted.size - 1].toInt() xor 0xFF).toByte()

        decrypt(corrupted, iv, key)
    }
}
