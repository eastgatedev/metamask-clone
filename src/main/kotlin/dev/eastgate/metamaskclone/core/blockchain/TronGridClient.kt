package dev.eastgate.metamaskclone.core.blockchain

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tron.common.utils.Base58
import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private val SUN_PER_TRX = BigDecimal(BigInteger.TEN.pow(6))

enum class TronTransactionType { TRX, TRC20 }

data class TronTransaction(
    val transactionType: TronTransactionType,
    val txID: String,
    val blockTimestamp: Long,
    val ownerAddress: String,
    val toAddress: String,
    val contractType: String,
    // Native TRX fields (null for TRC20)
    val blockNumber: Long? = null,
    val contractResult: String? = null,
    val fee: Long? = null,
    val amount: Long? = null,
    val netUsage: Long? = null,
    val netFee: Long? = null,
    val energyUsage: Long? = null,
    val energyFee: Long? = null,
    val energyUsageTotal: Long? = null,
    // TRC20 fields (null for native TRX)
    val value: String? = null,
    val tokenName: String? = null,
    val tokenSymbol: String? = null,
    val tokenAddress: String? = null,
    val tokenDecimal: Int? = null
) {
    val amountInTrx: BigDecimal?
        get() = amount?.let { BigDecimal(it).divide(SUN_PER_TRX) }

    val displayValue: BigDecimal?
        get() = if (value != null && tokenDecimal != null) {
            BigDecimal(value).divide(BigDecimal(BigInteger.TEN.pow(tokenDecimal)))
        } else {
            null
        }
}

/**
 * TronGrid REST API client for fetching transaction history.
 *
 * Usage:
 * ```
 * TronGridClient.ofShasta().use { client ->
 *     val txs = client.getTrxTransactions("TVM3ECHPsVHjxf7a4p6tBv6NVWpUhLPJSW")
 * }
 * ```
 *
 * @param baseUrl TronGrid API base URL
 */
class TronGridClient(
    private val baseUrl: String = SHASTA_URL
) : Closeable {
    companion object {
        private const val SHASTA_URL = "https://api.shasta.trongrid.io"
        private const val MAINNET_URL = "https://api.trongrid.io"

        fun ofShasta() = TronGridClient(SHASTA_URL)
        fun ofMainnet() = TronGridClient(MAINNET_URL)
    }

    private val mapper = ObjectMapper()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch TRX transactions for a wallet address.
     *
     * @param address Tron wallet address (Base58 format, e.g. "T...")
     * @return List of [TronTransaction]
     */
    fun getTrxTransactions(address: String): List<TronTransaction> {
        val url = "$baseUrl/v1/accounts/$address/transactions"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw RuntimeException("Empty response from TronGrid API")

            val json = mapper.readTree(body)

            val success = json["success"]?.asBoolean() ?: false
            if (!success) {
                val error = json["error"]?.asText() ?: ""
                if (error.contains("account", ignoreCase = true)) {
                    return emptyList()
                }
                throw RuntimeException("TronGrid API error: $error")
            }

            val dataArray = json["data"] ?: return emptyList()

            return dataArray.mapNotNull { tx ->
                val contract = tx["raw_data"]?.get("contract")?.get(0) ?: return@mapNotNull null
                val type = contract["type"]?.asText() ?: ""
                if (type == "TriggerSmartContract" || type == "CreateSmartContract") return@mapNotNull null
                val param = contract["parameter"]?.get("value") ?: return@mapNotNull null
                val ret = tx["ret"]?.get(0)

                TronTransaction(
                    transactionType = TronTransactionType.TRX,
                    txID = tx["txID"]?.asText() ?: "",
                    blockNumber = tx["blockNumber"]?.asLong() ?: 0L,
                    blockTimestamp = tx["block_timestamp"]?.asLong() ?: 0L,
                    contractType = contract["type"]?.asText() ?: "",
                    contractResult = ret?.get("contractRet")?.asText() ?: "",
                    fee = ret?.get("fee")?.asLong() ?: 0L,
                    ownerAddress = hexToBase58Check(param["owner_address"]?.asText() ?: ""),
                    toAddress = hexToBase58Check(param["to_address"]?.asText() ?: ""),
                    amount = param["amount"]?.asLong() ?: 0L,
                    netUsage = tx["net_usage"]?.asLong() ?: 0L,
                    netFee = tx["net_fee"]?.asLong() ?: 0L,
                    energyUsage = tx["energy_usage"]?.asLong() ?: 0L,
                    energyFee = tx["energy_fee"]?.asLong() ?: 0L,
                    energyUsageTotal = tx["energy_usage_total"]?.asLong() ?: 0L
                )
            }
        }
    }

    /**
     * Fetch TRC20 token transactions for a wallet address.
     *
     * @param address Tron wallet address (Base58 format, e.g. "T...")
     * @return List of [TronTransaction] with transactionType = TRC20
     */
    fun getTrc20Transactions(address: String): List<TronTransaction> {
        val url = "$baseUrl/v1/accounts/$address/transactions/trc20"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw RuntimeException("Empty response from TronGrid API")

            val json = mapper.readTree(body)

            val success = json["success"]?.asBoolean() ?: false
            if (!success) {
                val error = json["error"]?.asText() ?: ""
                if (error.contains("account", ignoreCase = true)) {
                    return emptyList()
                }
                throw RuntimeException("TronGrid API error: $error")
            }

            val dataArray = json["data"] ?: return emptyList()

            return dataArray.map { tx ->
                val tokenInfo = tx["token_info"]

                TronTransaction(
                    transactionType = TronTransactionType.TRC20,
                    txID = tx["transaction_id"]?.asText() ?: "",
                    blockTimestamp = tx["block_timestamp"]?.asLong() ?: 0L,
                    ownerAddress = tx["from"]?.asText() ?: "",
                    toAddress = tx["to"]?.asText() ?: "",
                    contractType = tx["type"]?.asText() ?: "",
                    value = tx["value"]?.asText() ?: "0",
                    tokenName = tokenInfo?.get("name")?.asText() ?: "",
                    tokenSymbol = tokenInfo?.get("symbol")?.asText() ?: "",
                    tokenAddress = tokenInfo?.get("address")?.asText() ?: "",
                    tokenDecimal = tokenInfo?.get("decimals")?.asInt() ?: 18
                )
            }
        }
    }

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    private fun hexToBase58Check(hex: String): String {
        if (hex.isEmpty()) return ""
        val bytes = hexStringToBytes(hex)
        val hash0 = sha256(bytes)
        val hash1 = sha256(hash0)
        val checksum = hash1.copyOfRange(0, 4)
        return Base58.encode(bytes + checksum)
    }

    private fun hexStringToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }

    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }
}
