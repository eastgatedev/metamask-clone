package dev.eastgate.metamaskclone.core.blockchain

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.Closeable
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class BitcoinAddress(
    val address: String,
    val amount: Double,
    val confirmations: Int,
    val txids: List<String>
)

data class BitcoinTransaction(
    val address: String,
    val category: String,
    val amount: Double,
    val txid: String,
    val time: Long,
    val timereceived: Long,
    val blockheight: Int?,
    val blockhash: String?,
    val fee: Double?,
    val confirmations: Int
)

/**
 * Bitcoin Core JSON-RPC client using OkHttp + Jackson.
 *
 * Usage:
 * ```
 * BitcoinRpcClient.ofRegtest().use { client ->
 *     val address = client.generateNewAddress()
 *     val balance = client.getBalance()
 * }
 * ```
 *
 * @param rpcUrl Full RPC URL with embedded credentials, e.g. "http://user:pass@127.0.0.1:18443"
 */
class BitcoinRpcClient(rpcUrl: String) : Closeable {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        /** Create client for local regtest node with default credentials. */
        fun ofRegtest() = BitcoinRpcClient("http://bitcoin:bitcoin@127.0.0.1:18443")
    }

    private val requestIdCounter = AtomicLong(0)
    private val mapper = ObjectMapper()

    private val baseUrl: String
    private val basicAuth: String

    init {
        val url = URL(rpcUrl)
        val userInfo = url.userInfo
            ?: throw IllegalArgumentException("RPC URL must contain credentials (http://user:pass@host:port)")
        val parts = userInfo.split(":", limit = 2)
        basicAuth = Credentials.basic(parts[0], parts.getOrElse(1) { "" })
        baseUrl = "${url.protocol}://${url.host}:${url.port}${url.path.ifEmpty { "/" }}"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // === Public API ===

    /** Generate a new Bitcoin address using the default address type. */
    fun generateNewAddress(): String {
        return rpcCall("getnewaddress").asText()
    }

    /** Get wallet balance in BTC. */
    fun getBalance(): Double {
        return rpcCall("getbalance").asDouble()
    }

    /** Get blockchain info (chain, blocks, headers, difficulty, etc.). */
    fun getBlockchainInfo(): JsonNode {
        return rpcCall("getblockchaininfo")
    }

    /** Get current block count. */
    fun getBlockCount(): Int {
        return rpcCall("getblockcount").asInt()
    }

    /**
     * Set the transaction fee rate per kB.
     * On regtest, fee estimation has no data, so this must be called before sending.
     * On mainnet, Bitcoin Core estimates fees automatically — avoid calling this.
     *
     * @param feePerKb Fee rate in BTC per kilobyte
     * @return true if the fee was successfully set
     */
    fun setTxFee(feePerKb: Double): Boolean {
        return rpcCall("settxfee", listOf(feePerKb)).asBoolean()
    }

    /**
     * List recent wallet transactions.
     *
     * @param count Number of transactions to return (default 10)
     * @return List of [BitcoinTransaction]
     */
    fun listTransactions(count: Int = 10): List<BitcoinTransaction> {
        val result = rpcCall("listtransactions", listOf("*", count))
        return result.map { tx ->
            BitcoinTransaction(
                address = tx["address"]?.asText() ?: "",
                category = tx["category"]?.asText() ?: "",
                amount = tx["amount"]?.asDouble() ?: 0.0,
                txid = tx["txid"]?.asText() ?: "",
                time = tx["time"]?.asLong() ?: 0L,
                timereceived = tx["timereceived"]?.asLong() ?: 0L,
                blockheight = tx["blockheight"]?.asInt(),
                blockhash = tx["blockhash"]?.asText(),
                fee = tx["fee"]?.asDouble(),
                confirmations = tx["confirmations"]?.asInt() ?: 0
            )
        }
    }

    /**
     * Get all addresses in the wallet.
     * Tries `getaddressesbylabel("")` first, falls back to `listreceivedbyaddress`.
     *
     * @return List of Bitcoin address strings
     */
    fun getAllAddresses(): List<String> {
        return try {
            val result = rpcCall("getaddressesbylabel", listOf(""))
            result.fieldNames().asSequence().toList()
        } catch (_: RuntimeException) {
            listReceivedByAddress().map { it.address }
        }
    }

    /**
     * List addresses that have received payments.
     *
     * @param minConf Minimum confirmations (default 0)
     * @return List of [BitcoinAddress] with amount, confirmations, and txids
     */
    fun listReceivedByAddress(minConf: Int = 0): List<BitcoinAddress> {
        val result = rpcCall("listreceivedbyaddress", listOf(minConf, true))
        return result.map { entry ->
            BitcoinAddress(
                address = entry["address"]?.asText() ?: "",
                amount = entry["amount"]?.asDouble() ?: 0.0,
                confirmations = entry["confirmations"]?.asInt() ?: 0,
                txids = entry["txids"]?.map { it.asText() } ?: emptyList()
            )
        }
    }

    /**
     * Send BTC to an address.
     *
     * @param address Recipient Bitcoin address
     * @param amount Amount in BTC
     * @return Transaction ID (txid)
     */
    fun sendToAddress(
        address: String,
        amount: Double
    ): String {
        return rpcCall("sendtoaddress", listOf(address, amount)).asText()
    }

    // === Closeable ===

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    // === Private ===

    private fun rpcCall(
        method: String,
        params: List<Any> = emptyList()
    ): JsonNode {
        val payload = mapper.createObjectNode().apply {
            put("jsonrpc", "1.0")
            put("id", requestIdCounter.incrementAndGet())
            put("method", method)
            set<JsonNode>("params", mapper.valueToTree(params))
        }

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", basicAuth)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw RuntimeException("Empty response from Bitcoin RPC")

            val json = mapper.readTree(body)

            val error = json["error"]
            if (error != null && !error.isNull) {
                val code = error["code"]?.asInt() ?: -1
                val message = error["message"]?.asText() ?: "Unknown error"
                throw RuntimeException("Bitcoin RPC error $code: $message")
            }

            return json["result"]
                ?: throw RuntimeException("Missing 'result' in RPC response")
        }
    }
}
