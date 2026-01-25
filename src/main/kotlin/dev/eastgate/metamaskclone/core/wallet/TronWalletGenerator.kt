package dev.eastgate.metamaskclone.core.wallet

import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Wallet
import org.bouncycastle.util.encoders.Hex
import org.tron.common.crypto.ECKey
import org.tron.common.utils.Base58
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64

/**
 * Generator for TRON wallets.
 * Uses the same secp256k1 curve as EVM, but with different address encoding (Base58Check with 0x41 prefix).
 */
class TronWalletGenerator {
    companion object {
        const val TRON_MAINNET_PREFIX: Byte = 0x41.toByte()
    }

    private val secureRandom = SecureRandom()

    /**
     * Generates a new TRON wallet with a random private key.
     */
    fun generateNewWallet(
        name: String,
        password: String
    ): Wallet {
        val ecKey = ECKey(secureRandom)
        return createWalletFromECKey(ecKey, name, password, isImported = false)
    }

    /**
     * Imports a TRON wallet from an existing private key.
     * @param privateKey Hex-encoded private key (with or without 0x prefix)
     */
    fun importWalletFromPrivateKey(
        privateKey: String,
        name: String,
        password: String
    ): Wallet {
        val cleanedKey = privateKey.removePrefix("0x").trim()

        if (!isValidPrivateKey(cleanedKey)) {
            throw IllegalArgumentException("Invalid private key format. Expected 64 hex characters.")
        }

        val privateKeyBytes = Hex.decode(cleanedKey)
        val ecKey = ECKey.fromPrivate(privateKeyBytes)
        return createWalletFromECKey(ecKey, name, password, isImported = true)
    }

    private fun createWalletFromECKey(
        ecKey: ECKey,
        name: String,
        password: String,
        isImported: Boolean
    ): Wallet {
        val addressBytes = ecKey.address
        val tronAddress = encodeToBase58Check(addressBytes)
        val privateKeyHex = Hex.toHexString(ecKey.privKeyBytes)
        val publicKeyHex = Hex.toHexString(ecKey.pubKey)
        val encryptedPrivateKey = encryptPrivateKey(privateKeyHex, password)

        return Wallet(
            address = tronAddress,
            name = name,
            encryptedPrivateKey = encryptedPrivateKey,
            publicKey = publicKeyHex,
            createdAt = LocalDateTime.now().toString(),
            isImported = isImported,
            blockchainType = BlockchainType.TRON
        )
    }

    /**
     * Decrypts and returns the private key for a wallet.
     */
    fun decryptPrivateKey(
        encryptedPrivateKey: String,
        password: String
    ): String {
        return decryptString(encryptedPrivateKey, password)
    }

    /**
     * Encodes address bytes to Base58Check format (TRON mainnet format).
     * Format: Base58(addressBytes + checksum)
     * where checksum = first 4 bytes of SHA256(SHA256(addressBytes))
     */
    private fun encodeToBase58Check(addressBytes: ByteArray): String {
        val hash0 = sha256(addressBytes)
        val hash1 = sha256(hash0)
        val checksum = hash1.copyOfRange(0, 4)
        val addressWithChecksum = addressBytes + checksum
        return Base58.encode(addressWithChecksum)
    }

    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    private fun encryptPrivateKey(
        privateKey: String,
        password: String
    ): String {
        return encryptString(privateKey, password)
    }

    // Reuse same encryption logic as WalletGenerator for consistency
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

    /**
     * Generates a default wallet name for a new TRON wallet.
     */
    fun generateWalletName(index: Int): String {
        return "TRON Wallet ${index + 1}"
    }
}
