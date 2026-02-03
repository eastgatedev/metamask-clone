package dev.eastgate.metamaskclone.core.blockchain

import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.tron.api.GrpcAPI
import org.tron.api.WalletGrpc
import org.tron.common.crypto.ECKey
import org.tron.common.crypto.Sha256Sm3Hash
import org.tron.common.utils.Base58
import org.tron.protos.Protocol
import org.tron.protos.contract.BalanceContract
import org.tron.protos.contract.SmartContractOuterClass
import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * TRC20 token information.
 */
data class Trc20TokenInfo(
    val contractAddress: String,
    val name: String,
    val symbol: String,
    val decimals: Int
)

/**
 * TRON blockchain client using gRPC protocol.
 *
 * This client provides a clean API for interacting with the TRON blockchain,
 * similar to how Web3j works for EVM chains. The gRPC channel is created once
 * in the constructor and reused for all operations.
 *
 * Usage:
 * ```
 * val client = TronGrpcClient("grpc.shasta.trongrid.io", 50051)
 * try {
 *     val balance = client.getBalance("T...")
 *     val txId = client.sendTrx("T...", "T...", "privateKey", BigDecimal("1.5"))
 * } finally {
 *     client.close()
 * }
 * ```
 *
 * Or with Kotlin's use() for automatic resource management:
 * ```
 * TronGrpcClient.ofShasta().use { client ->
 *     val balance = client.getBalance("T...")
 * }
 * ```
 *
 * @param grpcEndpoint The gRPC endpoint hostname (e.g., "grpc.shasta.trongrid.io")
 * @param grpcPort The gRPC port (typically 50051)
 */
class TronGrpcClient(
    private val grpcEndpoint: String,
    private val grpcPort: Int
) : Closeable {
    companion object {
        /** Conversion factor: 1 TRX = 1,000,000 SUN */
        const val SUN_PER_TRX = 1_000_000L

        /** TRON mainnet address prefix (0x41) */
        const val TRON_MAINNET_PREFIX: Byte = 0x41

        // Convenience factory methods for common networks

        /** Create client for Shasta testnet */
        fun ofShasta() = TronGrpcClient("grpc.shasta.trongrid.io", 50051)

        /** Create client for TRON mainnet */
        fun ofMainnet() = TronGrpcClient("grpc.trongrid.io", 50051)

        /** Create client for Nile testnet */
        fun ofNile() = TronGrpcClient("grpc.nile.trongrid.io", 50051)
    }

    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress(grpcEndpoint, grpcPort)
        .usePlaintext() // TRON gRPC uses plaintext (no TLS)
        .build()

    private val walletStub: WalletGrpc.WalletBlockingStub =
        WalletGrpc.newBlockingStub(channel)

    // === Balance Operations ===

    /**
     * Get TRX balance for an address in TRX units.
     *
     * @param address Base58Check encoded TRON address (starts with 'T')
     * @return Balance in TRX as BigDecimal with 6 decimal places
     * @throws IllegalArgumentException if address is invalid
     * @throws RuntimeException if gRPC call fails
     */
    fun getBalance(address: String): BigDecimal {
        val balanceInSun = getBalanceInSun(address)
        return sunToTrx(balanceInSun)
    }

    /**
     * Get TRX balance for an address in SUN units.
     *
     * @param address Base58Check encoded TRON address (starts with 'T')
     * @return Balance in SUN (1 TRX = 1,000,000 SUN)
     * @throws IllegalArgumentException if address is invalid
     * @throws RuntimeException if gRPC call fails
     */
    fun getBalanceInSun(address: String): Long {
        val addressBytes = decodeBase58Check(address)
        val account = walletStub.getAccount(
            Protocol.Account.newBuilder()
                .setAddress(ByteString.copyFrom(addressBytes))
                .build()
        )
        return account.balance
    }

    // === Transfer Operations ===

    /**
     * Send TRX from one address to another.
     *
     * @param fromAddress Sender's Base58Check address
     * @param toAddress Recipient's Base58Check address
     * @param privateKey Sender's private key (64-character hex string without 0x prefix)
     * @param amountTrx Amount to send in TRX
     * @return Transaction ID (64-character hex string)
     * @throws IllegalArgumentException if addresses are invalid
     * @throws RuntimeException if transaction creation, signing, or broadcast fails
     */
    fun sendTrx(
        fromAddress: String,
        toAddress: String,
        privateKey: String,
        amountTrx: BigDecimal
    ): String {
        val ownerAddressBytes = decodeBase58Check(fromAddress)
        val toAddressBytes = decodeBase58Check(toAddress)
        val amountInSun = trxToSun(amountTrx)

        // Step 1: Create TransferContract
        val transferContract = BalanceContract.TransferContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddressBytes))
            .setToAddress(ByteString.copyFrom(toAddressBytes))
            .setAmount(amountInSun)
            .build()

        // Step 2: Create transaction via gRPC
        val txExtension = walletStub.createTransaction2(transferContract)
        if (!txExtension.result.result) {
            throw RuntimeException("Failed to create transaction: ${txExtension.result.message.toStringUtf8()}")
        }

        // Step 3: Sign the transaction
        val privateKeyBytes = hexStringToByteArray(privateKey)
        val ecKey = ECKey.fromPrivate(privateKeyBytes)

        val rawDataBytes = txExtension.transaction.rawData.toByteArray()
        val hash = Sha256Sm3Hash.hash(rawDataBytes)
        val signature = ecKey.sign(hash)

        val signedTransaction = txExtension.transaction.toBuilder()
            .addSignature(ByteString.copyFrom(signature.toByteArray()))
            .build()

        // Step 4: Broadcast the transaction
        val broadcastResult = walletStub.broadcastTransaction(signedTransaction)
        if (!broadcastResult.result) {
            throw RuntimeException("Broadcast failed: ${broadcastResult.code} - ${broadcastResult.message.toStringUtf8()}")
        }

        return getTransactionId(signedTransaction)
    }

    // === Account Info ===

    /**
     * Get full account details for an address.
     *
     * @param address Base58Check encoded TRON address
     * @return Protocol.Account containing balance, create time, frozen balance, etc.
     */
    fun getAccount(address: String): Protocol.Account {
        val addressBytes = decodeBase58Check(address)
        return walletStub.getAccount(
            Protocol.Account.newBuilder()
                .setAddress(ByteString.copyFrom(addressBytes))
                .build()
        )
    }

    /**
     * Get account resources (bandwidth and energy) for an address.
     *
     * @param address Base58Check encoded TRON address
     * @return AccountResourceMessage containing bandwidth and energy details
     */
    fun getAccountResource(address: String): GrpcAPI.AccountResourceMessage {
        val addressBytes = decodeBase58Check(address)
        return walletStub.getAccountResource(
            Protocol.Account.newBuilder()
                .setAddress(ByteString.copyFrom(addressBytes))
                .build()
        )
    }

    // === TRC20 Token Operations ===

    /**
     * Get TRC20 token information (name, symbol, decimals).
     *
     * @param contractAddress Base58Check encoded TRC20 contract address
     * @return Token info including name, symbol, and decimals
     * @throws RuntimeException if contract call fails
     */
    fun getTrc20TokenInfo(contractAddress: String): Trc20TokenInfo {
        val contractBytes = decodeBase58Check(contractAddress)

        val name = decodeAbiString(callConstantContract(contractBytes, selectorName))
        val symbol = decodeAbiString(callConstantContract(contractBytes, selectorSymbol))
        val decimals = decodeAbiUint(callConstantContract(contractBytes, selectorDecimals)).toInt()

        return Trc20TokenInfo(
            contractAddress = contractAddress,
            name = name,
            symbol = symbol,
            decimals = decimals
        )
    }

    /**
     * Get TRC20 token balance for an address.
     * Automatically fetches decimals from the contract.
     *
     * @param ownerAddress Base58Check encoded owner address
     * @param contractAddress Base58Check encoded TRC20 contract address
     * @return Token balance as BigDecimal (with proper decimal places)
     * @throws RuntimeException if contract call fails
     */
    fun getTrc20Balance(
        ownerAddress: String,
        contractAddress: String
    ): BigDecimal {
        val contractBytes = decodeBase58Check(contractAddress)

        // Get decimals from contract
        val decimals = decodeAbiUint(callConstantContract(contractBytes, selectorDecimals)).toInt()

        // Get raw balance
        val rawBalance = getTrc20BalanceRaw(ownerAddress, contractAddress)

        // Convert to decimal representation
        return BigDecimal(rawBalance)
            .divide(BigDecimal.TEN.pow(decimals), decimals, RoundingMode.HALF_UP)
    }

    /**
     * Get TRC20 token balance in raw units (no decimal conversion).
     *
     * @param ownerAddress Base58Check encoded owner address
     * @param contractAddress Base58Check encoded TRC20 contract address
     * @return Raw token balance (before decimal conversion)
     * @throws RuntimeException if contract call fails
     */
    fun getTrc20BalanceRaw(
        ownerAddress: String,
        contractAddress: String
    ): BigInteger {
        val ownerBytes = decodeBase58Check(ownerAddress)
        val contractBytes = decodeBase58Check(contractAddress)

        val data = encodeBalanceOfCall(ownerBytes)
        val result = callConstantContract(contractBytes, data)

        return decodeAbiUint(result)
    }

    /**
     * Send TRC20 tokens from one address to another.
     *
     * @param fromAddress Sender's Base58Check address
     * @param toAddress Recipient's Base58Check address
     * @param contractAddress TRC20 contract address
     * @param privateKey Sender's private key (64-character hex string)
     * @param amount Amount to send (in token units, e.g., 1.5 for 1.5 tokens)
     * @param feeLimit Maximum TRX to burn for energy (default: 50 TRX)
     * @return Transaction ID (64-character hex string)
     * @throws RuntimeException if transaction creation, signing, or broadcast fails
     */
    fun sendTrc20(
        fromAddress: String,
        toAddress: String,
        contractAddress: String,
        privateKey: String,
        amount: BigDecimal,
        feeLimit: Long = 50_000_000L // 50 TRX in SUN
    ): String {
        val ownerAddressBytes = decodeBase58Check(fromAddress)
        val toAddressBytes = decodeBase58Check(toAddress)
        val contractBytes = decodeBase58Check(contractAddress)

        // Step 1: Get token decimals from contract
        val decimals = decodeAbiUint(callConstantContract(contractBytes, selectorDecimals)).toInt()

        // Step 2: Convert amount to raw units (amount × 10^decimals)
        val rawAmount = amount.multiply(BigDecimal.TEN.pow(decimals)).toBigInteger()

        // Step 3: Encode transfer(address,uint256) call data
        val data = encodeTransferCall(toAddressBytes, rawAmount)

        // Step 4: Create TriggerSmartContract for write operation
        val triggerContract = SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(ownerAddressBytes))
            .setContractAddress(ByteString.copyFrom(contractBytes))
            .setData(ByteString.copyFrom(data))
            .build()

        // Step 5: Trigger the smart contract (returns transaction to sign)
        val txExtension = walletStub.triggerContract(triggerContract)
        if (!txExtension.result.result) {
            throw RuntimeException("Failed to create TRC20 transfer: ${txExtension.result.message.toStringUtf8()}")
        }

        // Step 6: Set fee_limit on the transaction (required for smart contract calls)
        val rawDataWithFeeLimit = txExtension.transaction.rawData.toBuilder()
            .setFeeLimit(feeLimit)
            .build()

        val transactionWithFeeLimit = txExtension.transaction.toBuilder()
            .setRawData(rawDataWithFeeLimit)
            .build()

        // Step 7: Sign the transaction
        val privateKeyBytes = hexStringToByteArray(privateKey)
        val ecKey = ECKey.fromPrivate(privateKeyBytes)

        val rawDataBytes = transactionWithFeeLimit.rawData.toByteArray()
        val hash = Sha256Sm3Hash.hash(rawDataBytes)
        val signature = ecKey.sign(hash)

        val signedTransaction = transactionWithFeeLimit.toBuilder()
            .addSignature(ByteString.copyFrom(signature.toByteArray()))
            .build()

        // Step 8: Broadcast the transaction
        val broadcastResult = walletStub.broadcastTransaction(signedTransaction)
        if (!broadcastResult.result) {
            throw RuntimeException("Broadcast failed: ${broadcastResult.code} - ${broadcastResult.message.toStringUtf8()}")
        }

        return getTransactionId(signedTransaction)
    }

    // === Block Info ===

    /**
     * Get the current/latest block from the network.
     *
     * @return Protocol.Block containing block number, timestamp, transactions, etc.
     */
    fun getNowBlock(): Protocol.Block {
        return walletStub.getNowBlock(GrpcAPI.EmptyMessage.getDefaultInstance())
    }

    /**
     * Get transaction info by transaction ID.
     *
     * @param txId Transaction ID (64-character hex string)
     * @return Protocol.TransactionInfo containing block number, fee, result, etc.
     */
    fun getTransactionInfo(txId: String): Protocol.TransactionInfo {
        return walletStub.getTransactionInfoById(
            GrpcAPI.BytesMessage.newBuilder()
                .setValue(ByteString.copyFrom(hexStringToByteArray(txId)))
                .build()
        )
    }

    // === Closeable ===

    /**
     * Gracefully shutdown the gRPC channel.
     * Should be called when the client is no longer needed.
     */
    override fun close() {
        channel.shutdown()
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            channel.shutdownNow()
        }
    }

    // === Private Utilities ===

    /**
     * Decode Base58Check encoded TRON address to raw bytes.
     * Base58Check format: Base58(address_bytes + checksum)
     * where checksum = first 4 bytes of SHA256(SHA256(address_bytes))
     */
    private fun decodeBase58Check(base58Address: String): ByteArray {
        val decoded = Base58.decode(base58Address)
        if (decoded.size < 4) {
            throw IllegalArgumentException("Invalid Base58Check address: too short")
        }

        // Split into address bytes and checksum
        val addressBytes = decoded.copyOfRange(0, decoded.size - 4)
        val checksum = decoded.copyOfRange(decoded.size - 4, decoded.size)

        // Verify checksum
        val expectedChecksum = sha256(sha256(addressBytes)).copyOfRange(0, 4)
        if (!checksum.contentEquals(expectedChecksum)) {
            throw IllegalArgumentException("Invalid Base58Check checksum")
        }

        return addressBytes
    }

    /**
     * Encode raw address bytes to Base58Check format.
     */
    private fun encodeToBase58Check(addressBytes: ByteArray): String {
        val hash0 = sha256(addressBytes)
        val hash1 = sha256(hash0)
        val checksum = hash1.copyOfRange(0, 4)
        val addressWithChecksum = addressBytes + checksum
        return Base58.encode(addressWithChecksum)
    }

    /**
     * Compute SHA-256 hash.
     */
    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * Convert hex string to byte array.
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = if (hex.startsWith("0x")) hex.substring(2) else hex
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = (
                (Character.digit(cleanHex[i], 16) shl 4) +
                    Character.digit(cleanHex[i + 1], 16)
            ).toByte()
        }
        return data
    }

    /**
     * Get transaction ID (hash of raw data).
     */
    private fun getTransactionId(transaction: Protocol.Transaction): String {
        val rawDataBytes = transaction.rawData.toByteArray()
        val hash = Sha256Sm3Hash.hash(rawDataBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert SUN to TRX.
     */
    private fun sunToTrx(sun: Long): BigDecimal {
        return BigDecimal(sun)
            .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
    }

    /**
     * Convert TRX to SUN.
     */
    private fun trxToSun(trx: BigDecimal): Long {
        return trx.multiply(BigDecimal(SUN_PER_TRX)).toLong()
    }

    // === TRC20 Private Helpers ===

    // ABI function selectors (Keccak-256 first 4 bytes)
    private val selectorBalanceOf = hexStringToByteArray("70a08231") // balanceOf(address)
    private val selectorDecimals = hexStringToByteArray("313ce567") // decimals()
    private val selectorSymbol = hexStringToByteArray("95d89b41") // symbol()
    private val selectorName = hexStringToByteArray("06fdde03") // name()
    private val selectorTransfer = hexStringToByteArray("a9059cbb") // transfer(address,uint256)

    /**
     * Call a constant (read-only) contract method using TriggerSmartContract.
     *
     * @param contractAddressBytes Raw contract address bytes (21 bytes with 0x41 prefix)
     * @param data Function selector + encoded parameters
     * @return Contract response data
     */
    private fun callConstantContract(
        contractAddressBytes: ByteArray,
        data: ByteArray
    ): ByteArray {
        val triggerContract = SmartContractOuterClass.TriggerSmartContract.newBuilder()
            .setOwnerAddress(ByteString.copyFrom(contractAddressBytes)) // Use contract as owner for read-only calls
            .setContractAddress(ByteString.copyFrom(contractAddressBytes))
            .setData(ByteString.copyFrom(data))
            .build()

        val response = walletStub.triggerConstantContract(triggerContract)

        if (!response.result.result) {
            throw RuntimeException("Contract call failed: ${response.result.message.toStringUtf8()}")
        }

        // Get the result from constant_result field
        if (response.constantResultCount == 0) {
            throw RuntimeException("No result from contract call")
        }

        return response.getConstantResult(0).toByteArray()
    }

    /**
     * Encode balanceOf(address) call data.
     * Format: selector (4 bytes) + address padded to 32 bytes
     *
     * Note: TRON addresses are 21 bytes (with 0x41 prefix), but in ABI encoding
     * we use the last 20 bytes (without prefix) padded to 32 bytes.
     */
    private fun encodeBalanceOfCall(ownerAddressBytes: ByteArray): ByteArray {
        // Remove the 0x41 prefix from TRON address to get 20-byte address
        val address20 = if (ownerAddressBytes.size == 21 && ownerAddressBytes[0] == TRON_MAINNET_PREFIX) {
            ownerAddressBytes.copyOfRange(1, 21)
        } else {
            ownerAddressBytes
        }

        // Pad to 32 bytes (left-padded with zeros)
        val paddedAddress = ByteArray(32)
        address20.copyInto(paddedAddress, 32 - address20.size)

        // Combine selector + padded address
        return selectorBalanceOf + paddedAddress
    }

    /**
     * Encode transfer(address,uint256) call data.
     * Format: selector (4 bytes) + recipient address (32 bytes) + amount (32 bytes)
     *
     * | Offset | Size     | Content                                        |
     * |--------|----------|------------------------------------------------|
     * | 0-3    | 4 bytes  | Function selector: a9059cbb                    |
     * | 4-35   | 32 bytes | Recipient address (20 bytes, left-padded)      |
     * | 36-67  | 32 bytes | Amount (uint256, big-endian)                   |
     */
    private fun encodeTransferCall(
        toAddressBytes: ByteArray,
        amount: BigInteger
    ): ByteArray {
        // Remove the 0x41 prefix from TRON address to get 20-byte address
        val address20 = if (toAddressBytes.size == 21 && toAddressBytes[0] == TRON_MAINNET_PREFIX) {
            toAddressBytes.copyOfRange(1, 21)
        } else {
            toAddressBytes
        }

        // Pad address to 32 bytes (left-padded with zeros)
        val paddedAddress = ByteArray(32)
        address20.copyInto(paddedAddress, 32 - address20.size)

        // Encode amount as 32-byte big-endian
        val amountBytes = amount.toByteArray()
        val paddedAmount = ByteArray(32)
        // Handle potential sign byte from BigInteger
        val amountStartIndex = if (amountBytes.size > 32) 1 else 0
        val effectiveLength = amountBytes.size - amountStartIndex
        amountBytes.copyInto(paddedAmount, 32 - effectiveLength, amountStartIndex, amountBytes.size)

        // Combine selector + padded address + padded amount
        return selectorTransfer + paddedAddress + paddedAmount
    }

    /**
     * Decode ABI-encoded string from contract response.
     * Format: offset (32 bytes) + length (32 bytes) + data (padded to 32 bytes)
     */
    private fun decodeAbiString(data: ByteArray): String {
        if (data.size < 64) {
            return ""
        }

        // Read offset (should be 0x20 = 32 for simple string)
        val offset = BigInteger(1, data.copyOfRange(0, 32)).toInt()

        // Read length at offset
        val length = BigInteger(1, data.copyOfRange(offset, offset + 32)).toInt()

        // Read string data
        if (offset + 32 + length > data.size) {
            return ""
        }

        return String(data.copyOfRange(offset + 32, offset + 32 + length), Charsets.UTF_8)
    }

    /**
     * Decode ABI-encoded uint8/uint256 from contract response.
     * Format: 32 bytes, big-endian
     */
    private fun decodeAbiUint(data: ByteArray): BigInteger {
        if (data.isEmpty()) {
            return BigInteger.ZERO
        }

        // Take first 32 bytes (or all if less)
        val uintData = if (data.size >= 32) {
            data.copyOfRange(0, 32)
        } else {
            // Pad left with zeros if less than 32 bytes
            val padded = ByteArray(32)
            data.copyInto(padded, 32 - data.size)
            padded
        }

        return BigInteger(1, uintData)
    }
}
