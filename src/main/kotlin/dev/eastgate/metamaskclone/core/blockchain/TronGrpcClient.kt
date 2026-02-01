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
import java.io.Closeable
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

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
}
