package dev.eastgate.metamaskclone.core.blockchain

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

private val WEI_PER_ETH = BigDecimal(BigInteger.TEN.pow(18))

enum class EvmTransactionType { NATIVE, ERC20 }

data class EvmTransaction(
    val transactionType: EvmTransactionType,
    val blockNumber: String,
    val timeStamp: Long,
    val hash: String,
    val blockHash: String,
    val from: String,
    val to: String,
    val value: String,
    val isError: String = "",
    val txreceipt_status: String = "",
    val contractAddress: String = "",
    val tokenName: String = "",
    val tokenSymbol: String = "",
    val tokenDecimal: String = "",
    val gas: String = "",
    val gasPrice: String = "",
    val gasUsed: String = "",
    val methodId: String = "",
    val functionName: String = ""
) {
    val displayValue: BigDecimal
        get() = when (transactionType) {
            EvmTransactionType.NATIVE -> BigDecimal(value).divide(WEI_PER_ETH)
            EvmTransactionType.ERC20 -> {
                val decimals = tokenDecimal.ifEmpty { "18" }.toInt()
                BigDecimal(value).divide(BigDecimal(BigInteger.TEN.pow(decimals)))
            }
        }
}

/**
 * Etherscan V2 API client for fetching transaction history.
 *
 * Usage:
 * ```
 * EvmScanClient("YOUR_API_KEY").use { client ->
 *     val txs = client.getNativeTransactions(chainId = 1, walletAddress = "0x...")
 *     val erc20 = client.getErc20Transactions(chainId = 1, walletAddress = "0x...", tokenAddress = "0x...")
 * }
 * ```
 *
 * @param apiKey Etherscan API key
 */
class EvmScanClient(
    private val apiKey: String,
    private val baseUrl: String = BASE_URL
) : Closeable {
    companion object {
        private const val BASE_URL = "https://api.etherscan.io/v2/api"
    }

    private val mapper = ObjectMapper()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch native coin (ETH/MATIC/BNB etc.) transactions for a wallet address.
     *
     * @param chainId EVM chain ID (1 = Ethereum mainnet, 137 = Polygon, etc.)
     * @param walletAddress Wallet address to query
     * @param page Page number for pagination (default 1)
     * @param offset Number of results per page (default 10)
     * @param sort Sort order: "asc" or "desc" (default "desc")
     * @return List of [EvmTransaction] with transactionType = NATIVE
     */
    fun getNativeTransactions(
        chainId: Long,
        walletAddress: String,
        page: Int = 1,
        offset: Int = 10,
        sort: String = "desc"
    ): List<EvmTransaction> {
        val url = "$baseUrl?module=account&action=txlist" +
            "&chainid=$chainId" +
            "&address=$walletAddress" +
            "&startblock=0" +
            "&endblock=99999999" +
            "&page=$page" +
            "&offset=$offset" +
            "&sort=$sort" +
            "&apikey=$apiKey"

        return executeRequest(url) { tx ->
            EvmTransaction(
                transactionType = EvmTransactionType.NATIVE,
                blockNumber = tx["blockNumber"]?.asText() ?: "",
                timeStamp = tx["timeStamp"]?.asLong() ?: 0L,
                hash = tx["hash"]?.asText() ?: "",
                blockHash = tx["blockHash"]?.asText() ?: "",
                from = tx["from"]?.asText() ?: "",
                to = tx["to"]?.asText() ?: "",
                value = tx["value"]?.asText() ?: "0",
                isError = tx["isError"]?.asText() ?: "0",
                txreceipt_status = tx["txreceipt_status"]?.asText() ?: "",
                contractAddress = tx["contractAddress"]?.asText() ?: "",
                methodId = tx["methodId"]?.asText() ?: "",
                functionName = tx["functionName"]?.asText() ?: ""
            )
        }
    }

    /**
     * Fetch ERC-20 token transfer events for a wallet address.
     *
     * @param chainId EVM chain ID (1 = Ethereum mainnet, 11155111 = Sepolia, etc.)
     * @param walletAddress Wallet address to query
     * @param tokenAddress ERC-20 token contract address
     * @param page Page number for pagination (default 1)
     * @param offset Number of results per page (default 10)
     * @param sort Sort order: "asc" or "desc" (default "desc")
     * @return List of [EvmTransaction] with transactionType = ERC20
     */
    fun getErc20Transactions(
        chainId: Long,
        walletAddress: String,
        tokenAddress: String? = null,
        page: Int = 1,
        offset: Int = 10,
        sort: String = "desc"
    ): List<EvmTransaction> {
        val contractParam = if (tokenAddress != null) "&contractaddress=$tokenAddress" else ""
        val url = "$baseUrl?module=account&action=tokentx" +
            "&chainid=$chainId" +
            "&address=$walletAddress" +
            contractParam +
            "&startblock=0" +
            "&endblock=99999999" +
            "&page=$page" +
            "&offset=$offset" +
            "&sort=$sort" +
            "&apikey=$apiKey"

        return executeRequest(url) { tx ->
            EvmTransaction(
                transactionType = EvmTransactionType.ERC20,
                blockNumber = tx["blockNumber"]?.asText() ?: "",
                timeStamp = tx["timeStamp"]?.asLong() ?: 0L,
                hash = tx["hash"]?.asText() ?: "",
                blockHash = tx["blockHash"]?.asText() ?: "",
                from = tx["from"]?.asText() ?: "",
                to = tx["to"]?.asText() ?: "",
                value = tx["value"]?.asText() ?: "0",
                contractAddress = tx["contractAddress"]?.asText() ?: "",
                tokenName = tx["tokenName"]?.asText() ?: "",
                tokenSymbol = tx["tokenSymbol"]?.asText() ?: "",
                tokenDecimal = tx["tokenDecimal"]?.asText() ?: "18",
                gas = tx["gas"]?.asText() ?: "0",
                gasPrice = tx["gasPrice"]?.asText() ?: "0",
                gasUsed = tx["gasUsed"]?.asText() ?: "0",
                methodId = tx["methodId"]?.asText() ?: "",
                functionName = tx["functionName"]?.asText() ?: ""
            )
        }
    }

    override fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    private fun executeRequest(
        url: String,
        mapTransaction: (com.fasterxml.jackson.databind.JsonNode) -> EvmTransaction
    ): List<EvmTransaction> {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string()
                ?: throw RuntimeException("Empty response from Etherscan API")

            val json = mapper.readTree(body)

            val status = json["status"]?.asText() ?: ""
            if (status != "1") {
                val message = json["message"]?.asText() ?: "Unknown error"
                if (message.contains("No transactions found", ignoreCase = true)) {
                    return emptyList()
                }
                val result = json["result"]?.asText() ?: ""
                throw RuntimeException("Etherscan API error: $message ($result)")
            }

            val resultArray = json["result"] ?: return emptyList()

            return resultArray.map(mapTransaction)
        }
    }
}
