package dev.eastgate.metamaskclone.core.blockchain

import com.intellij.openapi.project.Project
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.tron.common.utils.Base58
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
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for blockchain operations using Web3j and TronGrpcClient.
 * Provides balance fetching, gas price queries, and transaction sending
 * for both EVM and TRON networks.
 */
class BlockchainService private constructor(private val project: Project) {
    private val networkManager = NetworkManager.getInstance(project)
    private val web3jInstances = ConcurrentHashMap<String, Web3j>()
    private val tronClients = ConcurrentHashMap<String, TronGrpcClient>()
    private val bitcoinClients = ConcurrentHashMap<String, BitcoinRpcClient>()
    private val evmScanClients = ConcurrentHashMap<String, EvmScanClient>()
    private val tronGridClients = ConcurrentHashMap<String, TronGridClient>()

    companion object {
        private val instances = ConcurrentHashMap<Project, BlockchainService>()

        fun getInstance(project: Project): BlockchainService {
            return instances.computeIfAbsent(project) { BlockchainService(it) }
        }

        const val DEFAULT_GAS_LIMIT = 21000L
        const val ERC20_GAS_LIMIT = 100000L
        const val DEFAULT_TIMEOUT_SECONDS = 60
        const val DEFAULT_POLL_INTERVAL_MS = 2000L
        const val ETHERSCAN_API_KEY = "PNQJ9UZ4PECH7Q31TGDD21DQB3QYK6A5SR"
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
     * Get or create TronGrpcClient instance for a TRON network.
     * Instances are cached per network ID.
     * Only supports predefined TRON networks (mainnet and shasta).
     */
    private fun getTronClient(network: Network): TronGrpcClient {
        return tronClients.computeIfAbsent(network.id) {
            when (network.id) {
                "TRON_MAINNET" -> TronGrpcClient.ofMainnet()
                "TRON_SHASTA" -> TronGrpcClient.ofShasta()
                else -> throw IllegalStateException("Unknown TRON network: ${network.id}")
            }
        }
    }

    /**
     * Get or create BitcoinRpcClient instance for a Bitcoin network.
     * Uses the effective RPC URL from NetworkManager (user-configured override or predefined default).
     * Instances are cached per network ID.
     */
    private fun getBitcoinClient(network: Network): BitcoinRpcClient {
        return bitcoinClients.computeIfAbsent(network.id) {
            BitcoinRpcClient(networkManager.getEffectiveRpcUrl(network))
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
     * Validate TRON address format with full Base58Check validation.
     * TRON addresses are Base58Check encoded, 34 characters, starting with 'T'.
     */
    fun isValidTronAddress(address: String): Boolean {
        // Quick format check
        if (!address.startsWith("T") || address.length != 34) {
            return false
        }

        // Full Base58Check validation with checksum verification
        return try {
            val decoded = Base58.decode(address)
            if (decoded.size < 4) {
                return false
            }

            // Split into address bytes and checksum
            val addressBytes = decoded.copyOfRange(0, decoded.size - 4)
            val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)

            // Verify checksum = first 4 bytes of SHA256(SHA256(addressBytes))
            val digest = MessageDigest.getInstance("SHA-256")
            val hash1 = digest.digest(addressBytes)
            val hash2 = digest.digest(hash1)
            val expectedChecksum = hash2.copyOfRange(0, 4)

            checksum.contentEquals(expectedChecksum)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch native coin balance for an address.
     * Supports both EVM (ETH, BNB, etc.) and TRON networks.
     */
    suspend fun getBalance(
        address: String,
        network: Network
    ): BalanceResult =
        withContext(Dispatchers.IO) {
            // Handle TRON networks using TronGrpcClient
            if (network.blockchainType == BlockchainType.TRON) {
                return@withContext try {
                    val client = getTronClient(network)
                    val balanceTrx = client.getBalance(address)
                    // Convert TRX to SUN for consistency (1 TRX = 1,000,000 SUN)
                    val balanceSun = balanceTrx.multiply(BigDecimal(TronGrpcClient.SUN_PER_TRX)).toBigInteger()
                    BalanceResult.Success(
                        balanceWei = balanceSun, // Using balanceWei field for SUN
                        balanceFormatted = balanceTrx.stripTrailingZeros().toPlainString(),
                        symbol = network.symbol
                    )
                } catch (e: Exception) {
                    BalanceResult.Error(e.message ?: "Failed to fetch TRX balance")
                }
            }

            // Handle Bitcoin networks using BitcoinRpcClient
            if (network.blockchainType == BlockchainType.BITCOIN) {
                return@withContext try {
                    val client = getBitcoinClient(network)
                    val balanceBtc = client.getBalance()
                    // Convert BTC to satoshis (1 BTC = 100,000,000 satoshis)
                    val balanceSatoshis = BigDecimal.valueOf(balanceBtc)
                        .multiply(BigDecimal(100_000_000L))
                        .toBigInteger()
                    BalanceResult.Success(
                        balanceWei = balanceSatoshis, // Using balanceWei field for satoshis
                        balanceFormatted = BigDecimal.valueOf(balanceBtc).stripTrailingZeros().toPlainString(),
                        symbol = network.symbol
                    )
                } catch (e: Exception) {
                    BalanceResult.Error(e.message ?: "Failed to fetch BTC balance")
                }
            }

            // Handle EVM networks using Web3j
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
     * Send TRX (TRON native coin) to an address.
     *
     * @param fromAddress The sender's TRON address (Base58Check, starts with T)
     * @param toAddress The recipient's TRON address (Base58Check, starts with T)
     * @param amount Amount to send in TRX units
     * @param privateKey The sender's private key (64-character hex string without 0x prefix)
     * @param network The TRON network to send on
     */
    suspend fun sendTronNativeCoin(
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        privateKey: String,
        network: Network
    ): TransactionResult =
        withContext(Dispatchers.IO) {
            try {
                // Validate TRON addresses
                if (!isValidTronAddress(fromAddress)) {
                    return@withContext TransactionResult.Error("Invalid TRON from address: $fromAddress")
                }
                if (!isValidTronAddress(toAddress)) {
                    return@withContext TransactionResult.Error("Invalid TRON to address: $toAddress")
                }

                val client = getTronClient(network)

                // Strip 0x prefix if present (TronGrpcClient expects raw hex)
                val cleanPrivateKey = if (privateKey.startsWith("0x")) {
                    privateKey.substring(2)
                } else {
                    privateKey
                }

                val txId = client.sendTrx(fromAddress, toAddress, cleanPrivateKey, amount)

                // TRON uses different explorer URL format with hash fragment
                val explorerUrl = network.blockExplorerUrl?.let { "$it/#/transaction/$txId" }

                TransactionResult.Success(
                    transactionHash = txId,
                    explorerUrl = explorerUrl
                )
            } catch (e: Exception) {
                TransactionResult.Error(e.message ?: "TRX transfer failed")
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
     * Fetch TRC20 token metadata (symbol, name, decimals) from contract.
     *
     * @param contractAddress The TRC20 contract address (Base58Check format)
     * @param network The TRON network where the token is deployed
     */
    suspend fun getTrc20TokenMetadata(
        contractAddress: String,
        network: Network
    ): TokenMetadataResult =
        withContext(Dispatchers.IO) {
            try {
                if (!isValidTronAddress(contractAddress)) {
                    return@withContext TokenMetadataResult.Error("Invalid TRC20 contract address: $contractAddress")
                }

                val client = getTronClient(network)
                val tokenInfo = client.getTrc20TokenInfo(contractAddress)

                TokenMetadataResult.Success(
                    symbol = tokenInfo.symbol,
                    name = tokenInfo.name,
                    decimals = tokenInfo.decimals
                )
            } catch (e: Exception) {
                TokenMetadataResult.Error(e.message ?: "Failed to fetch TRC20 token metadata")
            }
        }

    /**
     * Fetch ERC20/TRC20 token balance for an address.
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
            // Handle TRC20 tokens on TRON networks
            if (network.blockchainType == BlockchainType.TRON) {
                return@withContext try {
                    if (!isValidTronAddress(contractAddress)) {
                        return@withContext TokenBalanceResult.Error("Invalid TRC20 contract address: $contractAddress")
                    }
                    if (!isValidTronAddress(walletAddress)) {
                        return@withContext TokenBalanceResult.Error("Invalid wallet address: $walletAddress")
                    }

                    val client = getTronClient(network)
                    val balance = client.getTrc20Balance(walletAddress, contractAddress)

                    // Convert to raw units for consistency with ERC20
                    val balanceRaw = balance.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()

                    TokenBalanceResult.Success(
                        balanceRaw = balanceRaw,
                        balanceFormatted = balance.stripTrailingZeros().toPlainString()
                    )
                } catch (e: Exception) {
                    TokenBalanceResult.Error(e.message ?: "Failed to fetch TRC20 balance")
                }
            }

            // Handle ERC20 tokens on EVM networks
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
     * Send TRC20 tokens to an address.
     *
     * @param contractAddress The TRC20 contract address
     * @param fromAddress The sender's TRON address
     * @param toAddress The recipient's TRON address
     * @param amount Amount to send in token units (e.g., 10.5 for 10.5 tokens)
     * @param privateKey The sender's private key (hex string)
     * @param network The TRON network to send on
     * @param feeLimit Maximum TRX to burn for energy (in SUN, default: 50 TRX = 50,000,000 SUN)
     */
    suspend fun sendTrc20Token(
        contractAddress: String,
        fromAddress: String,
        toAddress: String,
        amount: BigDecimal,
        privateKey: String,
        network: Network,
        // 50 TRX in SUN
        feeLimit: Long = 50_000_000L
    ): TokenTransferResult =
        withContext(Dispatchers.IO) {
            try {
                // Validate TRON addresses
                if (!isValidTronAddress(contractAddress)) {
                    return@withContext TokenTransferResult.Error("Invalid TRC20 contract address: $contractAddress")
                }
                if (!isValidTronAddress(fromAddress)) {
                    return@withContext TokenTransferResult.Error("Invalid from address: $fromAddress")
                }
                if (!isValidTronAddress(toAddress)) {
                    return@withContext TokenTransferResult.Error("Invalid to address: $toAddress")
                }

                val client = getTronClient(network)

                // Strip 0x prefix if present
                val cleanPrivateKey = if (privateKey.startsWith("0x")) {
                    privateKey.substring(2)
                } else {
                    privateKey
                }

                val txId = client.sendTrc20(
                    fromAddress = fromAddress,
                    toAddress = toAddress,
                    contractAddress = contractAddress,
                    privateKey = cleanPrivateKey,
                    amount = amount,
                    feeLimit = feeLimit
                )

                // TRON uses different explorer URL format
                val explorerUrl = network.blockExplorerUrl?.let { "$it/#/transaction/$txId" }

                TokenTransferResult.Success(
                    transactionHash = txId,
                    explorerUrl = explorerUrl
                )
            } catch (e: Exception) {
                TokenTransferResult.Error(e.message ?: "TRC20 transfer failed")
            }
        }

    // ==================== Bitcoin Methods ====================

    /**
     * Send BTC to an address via Bitcoin Core.
     * On regtest, automatically sets a fee rate before sending.
     *
     * @param toAddress Recipient Bitcoin address
     * @param amount Amount in BTC
     * @param network The Bitcoin network
     */
    suspend fun sendBitcoin(
        toAddress: String,
        amount: Double,
        network: Network
    ): TransactionResult =
        withContext(Dispatchers.IO) {
            try {
                val client = getBitcoinClient(network)

                // On regtest, fee estimation has no data — set a manual fee
                if (network.id == "BTC_REGTEST") {
                    client.setTxFee(0.00001)
                }

                val txid = client.sendToAddress(toAddress, amount)
                val explorerUrl = network.blockExplorerUrl?.takeIf { it.isNotEmpty() }
                    ?.let { "$it/tx/$txid" }

                TransactionResult.Success(
                    transactionHash = txid,
                    explorerUrl = explorerUrl
                )
            } catch (e: Exception) {
                TransactionResult.Error(e.message ?: "Bitcoin send failed")
            }
        }

    /**
     * Get all receiving addresses from Bitcoin Core wallet.
     */
    suspend fun getBitcoinAddresses(network: Network): BitcoinAddressesResult =
        withContext(Dispatchers.IO) {
            try {
                val client = getBitcoinClient(network)
                val addresses = client.getAllAddresses()
                BitcoinAddressesResult.Success(addresses)
            } catch (e: Exception) {
                BitcoinAddressesResult.Error(e.message ?: "Failed to fetch Bitcoin addresses")
            }
        }

    /**
     * Generate a new receiving address in Bitcoin Core wallet.
     */
    suspend fun generateBitcoinAddress(network: Network): BitcoinAddressResult =
        withContext(Dispatchers.IO) {
            try {
                val client = getBitcoinClient(network)
                val address = client.generateNewAddress()
                BitcoinAddressResult.Success(address)
            } catch (e: Exception) {
                BitcoinAddressResult.Error(e.message ?: "Failed to generate Bitcoin address")
            }
        }

    /**
     * Get recent transactions from Bitcoin Core wallet.
     *
     * @param network The Bitcoin network
     * @param count Number of transactions to return
     */
    suspend fun getBitcoinTransactions(
        network: Network,
        count: Int = 20
    ): BitcoinTransactionsResult =
        withContext(Dispatchers.IO) {
            try {
                val client = getBitcoinClient(network)
                val transactions = client.listTransactions(count)
                BitcoinTransactionsResult.Success(transactions)
            } catch (e: Exception) {
                BitcoinTransactionsResult.Error(e.message ?: "Failed to fetch Bitcoin transactions")
            }
        }

    // ==================== Transaction History Methods ====================

    /**
     * Fetch EVM transaction history (native + ERC20) for a wallet.
     *
     * @param network The EVM network
     * @param walletAddress The wallet address to query
     * @param tokens List of tokens to fetch ERC20 transactions for
     */
    suspend fun getEvmTransactions(
        network: Network,
        walletAddress: String,
        tokens: List<Token>
    ): EvmTransactionsResult =
        withContext(Dispatchers.IO) {
            try {
                // Etherscan V2 free API key only supports Ethereum Mainnet and Sepolia
                val supportedChainIds = setOf(1L, 11155111L)
                val chainId = network.chainId.toLong()
                if (chainId !in supportedChainIds) {
                    return@withContext EvmTransactionsResult.Error(
                        "Transaction history is not available for ${network.name}. " +
                            "Etherscan API only supports Ethereum Mainnet and Sepolia."
                    )
                }

                val client = evmScanClients.computeIfAbsent("etherscan") {
                    EvmScanClient(ETHERSCAN_API_KEY)
                }

                val allTransactions = mutableListOf<EvmTransaction>()

                // Fetch native transactions
                val nativeTxs = client.getNativeTransactions(
                    chainId = chainId,
                    walletAddress = walletAddress,
                    offset = 20
                )
                allTransactions.addAll(nativeTxs)

                // Fetch ERC20 transactions for each token
                val networkTokens = tokens.filter { it.networkId == network.id }
                for (token in networkTokens) {
                    val erc20Txs = client.getErc20Transactions(
                        chainId = chainId,
                        walletAddress = walletAddress,
                        tokenAddress = token.contractAddress,
                        offset = 20
                    )
                    allTransactions.addAll(erc20Txs)
                }

                // Sort by timestamp descending
                val sorted = allTransactions.sortedByDescending { it.timeStamp }
                EvmTransactionsResult.Success(sorted)
            } catch (e: Exception) {
                EvmTransactionsResult.Error(e.message ?: "Failed to fetch EVM transactions")
            }
        }

    /**
     * Fetch TRON transaction history (TRX + TRC20) for a wallet.
     *
     * @param network The TRON network
     * @param walletAddress The wallet address to query
     */
    suspend fun getTronTransactions(
        network: Network,
        walletAddress: String
    ): TronTransactionsResult =
        withContext(Dispatchers.IO) {
            try {
                val client = tronGridClients.computeIfAbsent(network.id) {
                    when (network.id) {
                        "TRON_MAINNET" -> TronGridClient.ofMainnet()
                        "TRON_SHASTA" -> TronGridClient.ofShasta()
                        else -> throw IllegalStateException("Unknown TRON network: ${network.id}")
                    }
                }

                val allTransactions = mutableListOf<TronTransaction>()

                // Fetch TRX transactions
                val trxTxs = client.getTrxTransactions(walletAddress)
                allTransactions.addAll(trxTxs)

                // Fetch TRC20 transactions
                val trc20Txs = client.getTrc20Transactions(walletAddress)
                allTransactions.addAll(trc20Txs)

                // Sort by blockTimestamp descending
                val sorted = allTransactions.sortedByDescending { it.blockTimestamp }
                TronTransactionsResult.Success(sorted)
            } catch (e: Exception) {
                TronTransactionsResult.Error(e.message ?: "Failed to fetch TRON transactions")
            }
        }

    /**
     * Invalidate cached client instance for a network.
     * Useful when network RPC URL changes.
     * Handles Web3j (EVM), TronGrpcClient (TRON), and BitcoinRpcClient (Bitcoin) instances.
     */
    fun invalidateNetwork(networkId: String) {
        web3jInstances.remove(networkId)?.shutdown()
        tronClients.remove(networkId)?.close()
        bitcoinClients.remove(networkId)?.close()
        evmScanClients.remove(networkId)?.close()
        tronGridClients.remove(networkId)?.close()
    }

    /**
     * Clean up all client instances (Web3j, TronGrpcClient, and BitcoinRpcClient).
     */
    fun shutdown() {
        web3jInstances.values.forEach { it.shutdown() }
        web3jInstances.clear()
        tronClients.values.forEach { it.close() }
        tronClients.clear()
        bitcoinClients.values.forEach { it.close() }
        bitcoinClients.clear()
        evmScanClients.values.forEach { it.close() }
        evmScanClients.clear()
        tronGridClients.values.forEach { it.close() }
        tronGridClients.clear()
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

// Bitcoin Result Classes

sealed class BitcoinAddressesResult {
    data class Success(val addresses: List<String>) : BitcoinAddressesResult()
    data class Error(val message: String) : BitcoinAddressesResult()
}

sealed class BitcoinAddressResult {
    data class Success(val address: String) : BitcoinAddressResult()
    data class Error(val message: String) : BitcoinAddressResult()
}

sealed class BitcoinTransactionsResult {
    data class Success(val transactions: List<BitcoinTransaction>) : BitcoinTransactionsResult()
    data class Error(val message: String) : BitcoinTransactionsResult()
}

// EVM/TRON Transaction History Result Classes

sealed class EvmTransactionsResult {
    data class Success(val transactions: List<EvmTransaction>) : EvmTransactionsResult()
    data class Error(val message: String) : EvmTransactionsResult()
}

sealed class TronTransactionsResult {
    data class Success(val transactions: List<TronTransaction>) : TronTransactionsResult()
    data class Error(val message: String) : TronTransactionsResult()
}
