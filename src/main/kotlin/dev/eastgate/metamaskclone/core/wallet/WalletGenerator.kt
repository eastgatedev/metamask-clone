package dev.eastgate.metamaskclone.core.wallet

import dev.eastgate.metamaskclone.models.Wallet
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.security.SecureRandom
import java.util.Base64

class WalletGenerator {
    private val secureRandom = SecureRandom()

    fun generateNewWallet(
        name: String,
        password: String
    ): Wallet {
        val ecKeyPair = Keys.createEcKeyPair(secureRandom)
        val credentials = Credentials.create(ecKeyPair)

        val address = credentials.address
        val privateKey = credentials.ecKeyPair.privateKey.toString(16)
        val publicKey = credentials.ecKeyPair.publicKey.toString(16)

        val encryptedPrivateKey = encryptPrivateKey(privateKey, password)

        return Wallet(
            address = address,
            name = name,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKey = publicKey,
            isImported = false
        )
    }

    fun importWalletFromPrivateKey(
        privateKey: String,
        name: String,
        password: String
    ): Wallet {
        val cleanedKey = privateKey.removePrefix("0x").trim()

        if (!isValidPrivateKey(cleanedKey)) {
            throw IllegalArgumentException("Invalid private key format")
        }

        val credentials = Credentials.create(cleanedKey)

        val address = credentials.address
        val publicKey = credentials.ecKeyPair.publicKey.toString(16)
        val encryptedPrivateKey = encryptPrivateKey(cleanedKey, password)

        return Wallet(
            address = address,
            name = name,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKey = publicKey,
            isImported = true
        )
    }

    fun decryptPrivateKey(
        encryptedPrivateKey: String,
        password: String
    ): String {
        return decryptString(encryptedPrivateKey, password)
    }

    private fun encryptPrivateKey(
        privateKey: String,
        password: String
    ): String {
        return encryptString(privateKey, password)
    }

    private fun encryptString(
        input: String,
        password: String
    ): String {
        val keyBytes = password.toByteArray()
        val inputBytes = input.toByteArray()
        val result = ByteArray(inputBytes.size)

        for (i in inputBytes.indices) {
            result[i] = (inputBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }

        return Base64.getEncoder().encodeToString(result)
    }

    private fun decryptString(
        encryptedData: String,
        password: String
    ): String {
        val keyBytes = password.toByteArray()
        val encryptedBytes = Base64.getDecoder().decode(encryptedData)
        val result = ByteArray(encryptedBytes.size)

        for (i in encryptedBytes.indices) {
            result[i] = (encryptedBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }

        return String(result)
    }

    private fun isValidPrivateKey(privateKey: String): Boolean {
        return try {
            privateKey.matches(Regex("[0-9a-fA-F]{64}"))
        } catch (_: Exception) {
            false
        }
    }

    fun generateWalletName(index: Int): String {
        return "Wallet ${index + 1}"
    }
}
