package dev.eastgate.metamaskclone.core.storage

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 16

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun encrypt(
        data: String,
        password: String
    ): String {
        val salt = generateSalt()
        val iv = generateIV()
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

        val encryptedData = cipher.doFinal(data.toByteArray())

        // Combine salt + iv + encrypted data
        val combined = ByteArray(salt.size + iv.size + encryptedData.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(encryptedData, 0, combined, salt.size + iv.size, encryptedData.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    fun decrypt(
        encryptedData: String,
        password: String
    ): String {
        val combined = Base64.getDecoder().decode(encryptedData)

        // Extract salt, iv, and encrypted data
        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val iv = combined.sliceArray(SALT_LENGTH until SALT_LENGTH + IV_LENGTH)
        val encrypted = combined.sliceArray(SALT_LENGTH + IV_LENGTH until combined.size)

        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val decryptedData = cipher.doFinal(encrypted)
        return String(decryptedData)
    }

    private fun deriveKey(
        password: String,
        salt: ByteArray
    ): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val key = factory.generateSecret(spec).encoded
        return SecretKeySpec(key, KEY_ALGORITHM)
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    fun hashPassword(password: String): String {
        val salt = generateSalt()
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded

        // Combine salt + hash
        val combined = ByteArray(salt.size + hash.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(hash, 0, combined, salt.size, hash.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    fun verifyPassword(
        password: String,
        hashedPassword: String
    ): Boolean {
        val combined = Base64.getDecoder().decode(hashedPassword)
        val salt = combined.sliceArray(0 until SALT_LENGTH)
        val originalHash = combined.sliceArray(SALT_LENGTH until combined.size)

        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val newHash = factory.generateSecret(spec).encoded

        return originalHash.contentEquals(newHash)
    }
}
