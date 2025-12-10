package dev.eastgate.metamaskclone.models

import kotlinx.serialization.Serializable

@Serializable
data class Token(
    val id: String,
    val contractAddress: String,
    val symbol: String,
    val name: String,
    val decimals: Int = 18,
    val balance: String = "0",
    val networkId: String
) {
    fun getShortAddress(): String {
        return if (contractAddress.length > 13) {
            "${contractAddress.take(6)}...${contractAddress.takeLast(4)}"
        } else {
            contractAddress
        }
    }

    fun getFormattedBalance(): String {
        return try {
            val balanceValue = balance.toBigDecimalOrNull() ?: return "0"
            if (balanceValue == java.math.BigDecimal.ZERO) {
                "0"
            } else {
                balanceValue.stripTrailingZeros().toPlainString()
            }
        } catch (e: Exception) {
            balance
        }
    }

    companion object {
        fun generateId(): String = "TOKEN_${System.currentTimeMillis()}"
    }
}
