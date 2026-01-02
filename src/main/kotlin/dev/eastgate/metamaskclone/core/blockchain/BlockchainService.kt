package dev.eastgate.metamaskclone.core.blockchain

import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.core.storage.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.web3j.contracts.eip20.generated.ERC20
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.http.HttpService
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for blockchain operations using Web3j.
 * Provides balance fetching, gas price queries, and transaction sending.
 */
class BlockchainService private constructor(private val project: Project) {
    private val web3jInstances = ConcurrentHashMap<String, Web3j>()

    companion object {
        private val instances = ConcurrentHashMap<Project, BlockchainService>()

        fun getInstance(project: Project): BlockchainService {
            return instances.computeIfAbsent(project) { BlockchainService(it) }
        }

        const val DEFAULT_GAS_LIMIT = 21000L
        const val ERC20_GAS_LIMIT = 100000L
        const val DEFAULT_TIMEOUT_SECONDS = 60
        const val DEFAULT_POLL_INTERVAL_MS = 2000L
    }

    /**
     * Get or create Web3j instance for a network.
     * Instances are cached per network ID.
     */
    private fun getWeb3j(network: Network): Web3j {
        return web3jInstances.computeIfAbsent(network.id) {
            Web3j.build(HttpService(network.rpcUrl))
        }
    }

    /**
     * Validate Ethereum address format.
     */
    private fun isValidAddress(address: String): Boolean {
        if (!address.startsWith("0x")) return false
        if (address.length != 42) return false
        // Check all characters after 0x are valid hex
        return address.substring(2).all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
    }

    /**
     * Fetch native coin balance for an address.
     */
    suspend fun getBalance(
        address: String,
        network: Network
    ): BalanceResult =
        withContext(Dispatchers.IO) {
            try {
                val web3j = getWeb3j(network)
                val balanceResponse = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send()

                if (balanceResponse.hasError()) {
                    return@withContext BalanceResult.Error(balanceResponse.error.message ?: "Unknown error")
                }

                val balanceWei = balanceResponse.balance
                val balanceEther = Convert.fromWei(BigDecimal(balanceWei), Convert.Unit.ETHER)

                BalanceResult.Success(
                    balanceWei = balanceWei,
                    balanceFormatted = balanceEther.stripTrailingZeros().toPlainString(),
                    symbol = network.symbol
                )
            } catch (e: Exception) {
                BalanceResult.Error(e.message ?: "Failed to fetch balance")
            }
        }

    /**
     * Get current gas price from network.
     */
    suspend fun getGasPrice(network: Network): GasPriceResult =
        withContext(Dispatchers.IO) {
            try {
                val web3j = getWeb3j(network)
                val gasPriceResponse = web3j.ethGasPrice().send()

                if (gasPriceResponse.hasError()) {
                    return@withContext GasPriceResult.Error(gasPriceResponse.error.message ?: "Unknown error")
                }

                GasPriceResult.Success(gasPriceResponse.gasPrice)
            } catch (e: Exception) {
                GasPriceResult.Error(e.message ?: "Failed to fetch gas price")
            }
        }

    /**
     * Get transaction nonce for an address.
     */
    suspend fun getNonce(
        address: String,
        network: Network
    ): NonceResult =
        withContext(Dispatchers.IO) {
            try {
                val web3j = getWeb3j(network)
                val nonceResponse = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).send()

                if (nonceResponse.hasError()) {
                    return@withContext NonceResult.Error(nonceResponse.error.message ?: "Unknown error")
                }

                NonceResult.Success(nonceResponse.transactionCount)
            } catch (e: Exception) {
                NonceResult.Error(e.message ?: "Failed to fetch nonce")
            }
        }

    /**
     * Send native coin (ETH, BNB, MATIC, etc.) to an address.
     *
     * @param fromAddress The sender's address
     * @param toAddress The recipient's address
     * @param amount Amount to send in native coin units (e.g., ETH, BNB)
     * @param privateKey The sender's private key for signing
     * @param network The network to send on
     * @param gasLimit Gas limit (default: 21000 for simple transfers)
     * @param gasPrice Gas price in Wei (if null, fetched from network)
     */
    suspend fun sendNativeCoin(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        privateKey: String,
        network: Network,
        gasLimit: BigInteger = BigInteger.valueOf(DEFAULT_GAS_LIMIT),
        gasPrice: BigInteger? = null
    ): TransactionResult =
        withContext(Dispatchers.IO) {
            try {
                // Validate addresses before processing
                if (!isValidAddress(fromAddress)) {
                    return@withContext TransactionResult.Error("Invalid from address: $fromAddress")
                }
                if (!isValidAddress(toAddress)) {
                    return@withContext TransactionResult.Error("Invalid to address: $toAddress")
                }

                val web3j = getWeb3j(network)
                val credentials = Credentials.create(privateKey)

                // Verify the credentials match the from address
                if (!credentials.address.equals(fromAddress, ignoreCase = true)) {
                    return@withContext TransactionResult.Error("Private key does not match sender address")
                }

                // Get nonce
                val nonceResponse = web3j.ethGetTransactionCount(fromAddress, DefaultBlockParameterName.PENDING).send()
                if (nonceResponse.hasError()) {
                    return@withContext TransactionResult.Error("Failed to get nonce: ${nonceResponse.error.message}")
                }
                val nonce = nonceResponse.transactionCount

                // Get gas price if not provided
                val actualGasPrice = gasPrice ?: run {
                    val gasPriceResponse = web3j.ethGasPrice().send()
                    if (gasPriceResponse.hasError()) {
                        return@withContext TransactionResult.Error("Failed to get gas price: ${gasPriceResponse.error.message}")
                    }
                    gasPriceResponse.gasPrice
                }

                // Convert amount to Wei
                val amountWei = Convert.toWei(amount, Convert.Unit.ETHER).toBigInteger()

                // Create raw transaction
                val rawTransaction = RawTransaction.createEtherTransaction(
                    nonce,
                    actualGasPrice,
                    gasLimit,
                    toAddress,
                    amountWei
                )

                // Sign transaction with chain ID (EIP-155)
                val signedMessage = TransactionEncoder.signMessage(
                    rawTransaction,
                    network.chainId.toLong(),
                    credentials
                )
                val hexValue = Numeric.toHexString(signedMessage)

                // Send transaction
                val transactionResponse = web3j.ethSendRawTransaction(hexValue).send()

                if (transactionResponse.hasError()) {
                    return@withContext TransactionResult.Error(transactionResponse.error.message ?: "Transaction failed")
                }

                val txHash = transactionResponse.transactionHash
                val explorerUrl = network.blockExplorerUrl?.let { "$it/tx/$txHash" }

                TransactionResult.Success(
                    transactionHash = txHash,
                    explorerUrl = explorerUrl
                )
            } catch (e: Exception) {
                TransactionResult.Error(e.message ?: "Transaction failed")
            }
        }

    /**
     * Wait for transaction confirmation by polling for receipt.
     *
     * @param txHash Transaction hash to check
     * @param network Network to check on
     * @param maxWaitSeconds Maximum time to wait (default: 60 seconds)
     * @param pollIntervalMs Interval between polls (default: 2000ms)
     */
    suspend fun waitForConfirmation(
        txHash: String,
        network: Network,
        maxWaitSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
        pollIntervalMs: Long = DEFAULT_POLL_INTERVAL_MS
    ): ConfirmationResult =
        withContext(Dispatchers.IO) {
            try {
                val web3j = getWeb3j(network)
                val maxAttempts = (maxWaitSeconds * 1000 / pollIntervalMs).toInt()

                repeat(maxAttempts) {
                    val receiptResponse = web3j.ethGetTransactionReceipt(txHash).send()

                    if (receiptResponse.transactionReceipt.isPresent) {
                        val receipt = receiptResponse.transactionReceipt.get()
                        val success = receipt.status == "0x1"

                        return@withContext ConfirmationResult.Confirmed(
                            blockNumber = receipt.blockNumber,
                            gasUsed = receipt.gasUsed,
                            success = success
                        )
                    }

                    delay(pollIntervalMs)
                }

                ConfirmationResult.Timeout
            } catch (e: Exception) {
                ConfirmationResult.Error(e.message ?: "Failed to get confirmation")
            }
        }

    // ==================== ERC20 Token Methods ====================

    /**
     * Fetch ERC20 token metadata (symbol, name, decimals) from contract.
     *
     * @param contractAddress The token contract address
     * @param network The network where the token is deployed
     */
    suspend fun getTokenMetadata(
        contractAddress: String,
        network: Network
    ): TokenMetadataResult =
        withContext(Dispatchers.IO) {
            try {
                if (!isValidAddress(contractAddress)) {
                    return@withContext TokenMetadataResult.Error("Invalid contract address: $contractAddress")
                }

                val web3j = getWeb3j(network)
                // Use a dummy credentials for read-only operations
                val dummyCredentials = Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000001")
                val erc20 = ERC20.load(contractAddress, web3j, dummyCredentials, DefaultGasProvider())

                val symbol = erc20.symbol().send()
                val name = erc20.name().send()
                val decimals = erc20.decimals().send().toInt()

                TokenMetadataResult.Success(
                    symbol = symbol,
                    name = name,
                    decimals = decimals
                )
            } catch (e: Exception) {
                TokenMetadataResult.Error(e.message ?: "Failed to fetch token metadata")
            }
        }

    /**
     * Fetch ERC20 token balance for an address.
     *
     * @param contractAddress The token contract address
     * @param walletAddress The wallet address to check balance for
     * @param decimals Token decimals for formatting
     * @param network The network where the token is deployed
     */
    suspend fun getTokenBalance(
        contractAddress: String,
        walletAddress: String,
        decimals: Int,
        network: Network
    ): TokenBalanceResult =
        withContext(Dispatchers.IO) {
            try {
                if (!isValidAddress(contractAddress)) {
                    return@withContext TokenBalanceResult.Error("Invalid contract address: $contractAddress")
                }
                if (!isValidAddress(walletAddress)) {
                    return@withContext TokenBalanceResult.Error("Invalid wallet address: $walletAddress")
                }

                val web3j = getWeb3j(network)
                // Use a dummy credentials for read-only operations
                val dummyCredentials = Credentials.create("0x0000000000000000000000000000000000000000000000000000000000000001")
                val erc20 = ERC20.load(contractAddress, web3j, dummyCredentials, DefaultGasProvider())

                val balanceRaw = erc20.balanceOf(walletAddress).send()

                // Format balance with decimals
                val divisor = BigInteger.TEN.pow(decimals)
                val balanceFormatted = BigDecimal(balanceRaw)
                    .divide(BigDecimal(divisor))
                    .stripTrailingZeros()
                    .toPlainString()

                TokenBalanceResult.Success(
                    balanceRaw = balanceRaw,
                    balanceFormatted = balanceFormatted
                )
            } catch (e: Exception) {
                TokenBalanceResult.Error(e.message ?: "Failed to fetch token balance")
            }
        }

    /**
     * Send ERC20 tokens to an address.
     *
     * @param contractAddress The token contract address
     * @param toAddress The recipient's address
     * @param amount Amount to send in token units (e.g., 10.5 for 10.5 tokens)
     * @param decimals Token decimals for conversion
     * @param privateKey The sender's private key for signing
     * @param network The network to send on
     * @param gasLimit Gas limit (default: 100000 for ERC20 transfers)
     * @param gasPrice Gas price in Wei (if null, fetched from network)
     */
    suspend fun sendToken(
        contractAddress: String,
        toAddress: String,
        amount: BigDecimal,
        decimals: Int,
        privateKey: String,
        network: Network,
        gasLimit: BigInteger = BigInteger.valueOf(ERC20_GAS_LIMIT),
        gasPrice: BigInteger? = null
    ): TokenTransferResult =
        withContext(Dispatchers.IO) {
            try {
                if (!isValidAddress(contractAddress)) {
                    return@withContext TokenTransferResult.Error("Invalid contract address: $contractAddress")
                }
                if (!isValidAddress(toAddress)) {
                    return@withContext TokenTransferResult.Error("Invalid recipient address: $toAddress")
                }

                val web3j = getWeb3j(network)
                val credentials = Credentials.create(privateKey)

                // Get gas price if not provided
                val actualGasPrice = gasPrice ?: run {
                    val gasPriceResponse = web3j.ethGasPrice().send()
                    if (gasPriceResponse.hasError()) {
                        return@withContext TokenTransferResult.Error("Failed to get gas price: ${gasPriceResponse.error.message}")
                    }
                    gasPriceResponse.gasPrice
                }

                // Create gas provider with specified gas price and limit
                val gasProvider = StaticGasProvider(actualGasPrice, gasLimit)
                val erc20 = ERC20.load(contractAddress, web3j, credentials, gasProvider)

                // Convert amount to smallest unit (multiply by 10^decimals)
                val multiplier = BigInteger.TEN.pow(decimals)
                val amountInSmallestUnit = amount.multiply(BigDecimal(multiplier)).toBigInteger()

                // Execute the transfer
                val receipt = erc20.transfer(toAddress, amountInSmallestUnit).send()

                if (receipt.status != "0x1") {
                    return@withContext TokenTransferResult.Error("Transaction failed on chain")
                }

                val txHash = receipt.transactionHash
                val explorerUrl = network.blockExplorerUrl?.let { "$it/tx/$txHash" }

                TokenTransferResult.Success(
                    transactionHash = txHash,
                    explorerUrl = explorerUrl
                )
            } catch (e: Exception) {
                TokenTransferResult.Error(e.message ?: "Token transfer failed")
            }
        }

    /**
     * Invalidate cached Web3j instance for a network.
     * Useful when network RPC URL changes.
     */
    fun invalidateNetwork(networkId: String) {
        web3jInstances.remove(networkId)?.shutdown()
    }

    /**
     * Clean up all Web3j instances.
     */
    fun shutdown() {
        web3jInstances.values.forEach { it.shutdown() }
        web3jInstances.clear()
    }
}

// Result sealed classes for type-safe error handling

sealed class BalanceResult {
    data class Success(
        val balanceWei: BigInteger,
        val balanceFormatted: String,
        val symbol: String
    ) : BalanceResult()

    data class Error(val message: String) : BalanceResult()
}

sealed class GasPriceResult {
    data class Success(val gasPrice: BigInteger) : GasPriceResult()
    data class Error(val message: String) : GasPriceResult()
}

sealed class NonceResult {
    data class Success(val nonce: BigInteger) : NonceResult()
    data class Error(val message: String) : NonceResult()
}

sealed class TransactionResult {
    data class Success(
        val transactionHash: String,
        val explorerUrl: String?
    ) : TransactionResult()

    data class Error(val message: String) : TransactionResult()
}

sealed class ConfirmationResult {
    data class Confirmed(
        val blockNumber: BigInteger,
        val gasUsed: BigInteger,
        val success: Boolean
    ) : ConfirmationResult()

    object Timeout : ConfirmationResult()
    data class Error(val message: String) : ConfirmationResult()
}

// ERC20 Token Result Classes

sealed class TokenMetadataResult {
    data class Success(
        val symbol: String,
        val name: String,
        val decimals: Int
    ) : TokenMetadataResult()

    data class Error(val message: String) : TokenMetadataResult()
}

sealed class TokenBalanceResult {
    data class Success(
        val balanceRaw: BigInteger,
        val balanceFormatted: String
    ) : TokenBalanceResult()

    data class Error(val message: String) : TokenBalanceResult()
}

sealed class TokenTransferResult {
    data class Success(
        val transactionHash: String,
        val explorerUrl: String?
    ) : TokenTransferResult()

    data class Error(val message: String) : TokenTransferResult()
}
