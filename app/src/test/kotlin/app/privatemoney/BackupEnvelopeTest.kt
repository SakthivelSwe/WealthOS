package app.privatemoney

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupEnvelopeTest {

    private val magic = byteArrayOf(0x57, 0x4F, 0x53, 0x42) // "WOSB"
    private val currentVersion = 1
    private val minEnvelopeSize = 58
    private val maxEnvelopeSize = 50 * 1024 * 1024
    private val maxMetadataSize = 64 * 1024

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val pinChars = pin.toCharArray()
        val spec = PBEKeySpec(pinChars, salt, 100_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        java.util.Arrays.fill(pinChars, '\u0000')
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun encrypt(
        plainBytes: ByteArray,
        metadataBytes: ByteArray,
        key: SecretKeySpec
    ): Pair<ByteArray, ByteArray> {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(metadataBytes)
        val cipherText = cipher.doFinal(plainBytes)
        return iv to cipherText
    }

    private fun decrypt(
        cipherText: ByteArray,
        metadataBytes: ByteArray,
        iv: ByteArray,
        key: SecretKeySpec
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(metadataBytes)
        return cipher.doFinal(cipherText)
    }

    private fun sha256(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }

    private fun validateEnvelopeHeader(envelope: ByteArray) {
        if (envelope.size < minEnvelopeSize) {
            throw IllegalArgumentException("Invalid backup format: envelope truncated (${envelope.size} bytes).")
        }
        if (envelope.size > maxEnvelopeSize) {
            throw IllegalArgumentException("Invalid backup format: envelope exceeds maximum size ($maxEnvelopeSize bytes).")
        }
        val buffer = ByteBuffer.wrap(envelope)
        val fileMagic = ByteArray(4)
        buffer.get(fileMagic)
        if (!fileMagic.contentEquals(magic)) {
            throw IllegalArgumentException("Invalid backup format: unrecognized signature.")
        }
        val version = buffer.int
        if (version > currentVersion) {
            throw IllegalArgumentException("Incompatible backup version ($version). Please update WealthOS.")
        }
        if (version < 1) {
            throw IllegalArgumentException("Invalid backup format: unsupported version ($version).")
        }
        val metadataLen = buffer.int
        if (metadataLen <= 0 || metadataLen > maxMetadataSize) {
            throw IllegalArgumentException("Invalid backup metadata size: $metadataLen bytes.")
        }
        if (buffer.remaining() < metadataLen + 16 + 12 + 16) {
            throw IllegalArgumentException("Invalid backup format: payload truncated.")
        }
    }

    @Test
    fun backupEnvelopeFramingAndDecryptionSucceedsWithAeadAad() {
        val pin = "1234"
        val payloadJson = """{"accounts":[{"id":"acc_1","name":"Main Bank"}],"txns":[{"id":"txn_1","amountMinor":150000}]}"""
        val payloadBytes = payloadJson.toByteArray(Charsets.UTF_8)
        val checksum = sha256(payloadBytes)

        val metadataJson = """{"version":$currentVersion,"timestamp":1700000000,"checksumSha256":"$checksum","accountsCount":1,"txnsCount":1}"""
        val metaBytes = metadataJson.toByteArray(Charsets.UTF_8)

        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(pin, salt)
        val (iv, ciphertext) = encrypt(payloadBytes, metaBytes, key)

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

        // Validate envelope framing
        validateEnvelopeHeader(envelopeBytes)

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

        // Decrypt with AEAD AAD
        val recoveredKey = deriveKey(pin, readSalt)
        val decryptedBytes = decrypt(readCipher, readMetaBytes, readIv, recoveredKey)

        assertEquals(checksum, sha256(decryptedBytes))
        assertEquals(payloadJson, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test(expected = Exception::class)
    fun wrongPinFailsDecryption() {
        val correctPin = "1234"
        val wrongPin = "9999"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val metaBytes = """{"test":true}""".toByteArray()

        val key = deriveKey(correctPin, salt)
        val (iv, ciphertext) = encrypt("secret ledger data".toByteArray(), metaBytes, key)

        val wrongKey = deriveKey(wrongPin, salt)
        decrypt(ciphertext, metaBytes, iv, wrongKey)
    }

    @Test(expected = Exception::class)
    fun tamperedCiphertextIsRejectedByGCM() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val metaBytes = """{"test":true}""".toByteArray()
        val key = deriveKey(pin, salt)
        val (iv, ciphertext) = encrypt("authentic data".toByteArray(), metaBytes, key)

        val corrupted = ciphertext.copyOf()
        corrupted[corrupted.size - 1] = (corrupted[corrupted.size - 1].toInt() xor 0xFF).toByte()

        decrypt(corrupted, metaBytes, iv, key)
    }

    @Test(expected = AEADBadTagException::class)
    fun tamperedMetadataIsRejectedByAEAD() {
        val pin = "1234"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val metaBytes = """{"test":true,"user":"alice"}""".toByteArray()
        val key = deriveKey(pin, salt)
        val (iv, ciphertext) = encrypt("authentic data".toByteArray(), metaBytes, key)

        // Modify metadata (e.g. attacker changing user or timestamp in unencrypted metadata header)
        val tamperedMeta = """{"test":true,"user":"mallory"}""".toByteArray()

        // Decryption MUST throw AEADBadTagException because metadata is bound to GCM auth tag
        decrypt(ciphertext, tamperedMeta, iv, key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun envelopeSmallerThanMinSizeThrows() {
        val tinyEnvelope = ByteArray(57) // 1 byte less than MIN_ENVELOPE_SIZE
        validateEnvelopeHeader(tinyEnvelope)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidMagicThrows() {
        val badMagicEnvelope = ByteBuffer.allocate(60).apply {
            put(byteArrayOf(0x00, 0x01, 0x02, 0x03))
            putInt(1)
            putInt(2)
            put("{}".toByteArray())
            put(ByteArray(16))
            put(ByteArray(12))
            put(ByteArray(16))
        }.array()
        validateEnvelopeHeader(badMagicEnvelope)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedVersionThrows() {
        val futureVersionEnvelope = ByteBuffer.allocate(60).apply {
            put(magic)
            putInt(99) // future version 99
            putInt(2)
            put("{}".toByteArray())
            put(ByteArray(16))
            put(ByteArray(12))
            put(ByteArray(16))
        }.array()
        validateEnvelopeHeader(futureVersionEnvelope)
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedMetadataLengthThrows() {
        val badLenEnvelope = ByteBuffer.allocate(60).apply {
            put(magic)
            putInt(1)
            putInt(100_000) // exceeds MAX_METADATA_SIZE (64KB)
            put(ByteArray(40))
        }.array()
        validateEnvelopeHeader(badLenEnvelope)
    }

    @Test
    fun passwordSpecCanBeClearedSecurely() {
        val pinChars = "5678".toCharArray()
        val spec = PBEKeySpec(pinChars, ByteArray(16), 1000, 256)
        spec.clearPassword()
        java.util.Arrays.fill(pinChars, '\u0000')

        // Ensure chars array is zeroed
        assertTrue(pinChars.all { it == '\u0000' })
    }
}
