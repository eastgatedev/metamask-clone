package dev.eastgate.metamaskclone.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Wallet(
    val address: String,
    val name: String,
    val encryptedPrivateKey: String,
    val publicKey: String,
    val createdAt: String = LocalDateTime.now().toString(),
    val isImported: Boolean = false,
    val derivationPath: String? = null,
    val mnemonicId: String? = null
) {
    fun getShortAddress(): String {
        return if (address.length > 10) {
            "${address.substring(0, 6)}...${address.substring(address.length - 4)}"
        } else {
            address
        }
    }

    fun isValid(): Boolean {
        return address.startsWith("0x") && address.length == 42
    }
}

@Serializable
data class WalletBalance(
    val address: String,
    val nativeBalance: String,
    val nativeBalanceUSD: Double? = null,
    val tokens: List<TokenBalance> = emptyList()
)

@Serializable
data class TokenBalance(
    val contractAddress: String,
    val symbol: String,
    val name: String,
    val balance: String,
    val decimals: Int,
    val balanceUSD: Double? = null
)
