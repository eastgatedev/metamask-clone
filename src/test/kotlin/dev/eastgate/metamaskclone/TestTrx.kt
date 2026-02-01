package dev.eastgate.metamaskclone

import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.tron.api.GrpcAPI
import org.tron.api.WalletGrpc
import org.tron.common.crypto.ECKey
import org.tron.common.crypto.Sha256Sm3Hash
import org.tron.common.utils.Base58
import org.tron.protos.Protocol
import org.tron.protos.contract.BalanceContract
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * TestTrx demonstrates TRX balance queries using gRPC protocol on the Shasta testnet.
 *
 * Key Concepts:
 * 1. TRON uses gRPC for node communication (unlike EVM's JSON-RPC)
 * 2. Balance is returned in SUN (1 TRX = 1,000,000 SUN)
 * 3. Shasta is TRON's public testnet for development
 *
 * gRPC Endpoints:
 * - Shasta Full Node: grpc.shasta.trongrid.io:50051
 * - Shasta Solidity Node: grpc.shasta.trongrid.io:50052
 *
 * This test is marked @Disabled as it requires network access (integration test).
 */
@Disabled("Integration test - requires network access to Shasta testnet")
class TestTrx {
    companion object {
        // Shasta testnet gRPC endpoints
        const val SHASTA_GRPC_ENDPOINT = "grpc.shasta.trongrid.io"
        const val SHASTA_GRPC_PORT = 50051

        // SUN to TRX conversion factor (1 TRX = 1,000,000 SUN)
        const val SUN_PER_TRX = 1_000_000L

        // Test addresses on Shasta testnet
        // Using a known testnet address
        const val TEST_ADDRESS = "<your-wallet-address>"
        const val TEST_PRIVATE_KEY = "<your-key>"

        // Recipient address for transfer test
        const val RECIPIENT_ADDRESS = "TByuNoiAoxapDqJeT2PtrxSCgh2oaiGHxq"

        // Alternative test addresses that may have balance on Shasta
        val ALTERNATIVE_TEST_ADDRESSES = listOf(
            "TG3XXyExBkPp9nzdajDZsozEu4BkaSJozs", // USDT contract address
            "TYsbWxNnyTgsZaTFaue9hqpxkU3Fkco94a", // Common test address
            "TVj7RNVHy6thbM7BWdSe9G6gXwKhjhdNZS" // Another test address
        )
    }

    @Test
    fun `test get TRX balance from Shasta testnet using gRPC`() {
        println("=".repeat(80))
        println("TEST: Query TRX Balance from Shasta Testnet (gRPC)")
        println("=".repeat(80))
        println()

        println("Configuration:")
        println("  gRPC Endpoint: $SHASTA_GRPC_ENDPOINT:$SHASTA_GRPC_PORT")
        println("  Test Address: $TEST_ADDRESS")
        println()

        // Create gRPC channel to Shasta full node
        val channel = ManagedChannelBuilder
            .forAddress(SHASTA_GRPC_ENDPOINT, SHASTA_GRPC_PORT)
            .usePlaintext() // Shasta uses plaintext (no TLS)
            .build()

        println("Created gRPC channel to Shasta testnet")
        println()

        try {
            // Create blocking stub for synchronous calls
            val walletStub = WalletGrpc.newBlockingStub(channel)

            // Decode Base58Check address to bytes
            val addressBytes = decodeBase58Check(TEST_ADDRESS)
            println("Decoded address bytes: ${addressBytes.joinToString("") { "%02x".format(it) }}")
            println()

            // Create Account request with address
            val accountRequest = Protocol.Account.newBuilder()
                .setAddress(ByteString.copyFrom(addressBytes))
                .build()

            // Query account information
            println("Querying account information...")
            val account = walletStub.getAccount(accountRequest)

            // Extract balance in SUN
            val balanceInSun = account.balance
            println("Balance in SUN: $balanceInSun")

            // Convert SUN to TRX
            val balanceInTrx = BigDecimal(balanceInSun)
                .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
            println("Balance in TRX: $balanceInTrx TRX")
            println()

            // Display additional account information if available
            println("Account Details:")
            if (account.address.isEmpty) {
                println("  Account not found or not activated on Shasta testnet")
            } else {
                println("  Address (hex): ${account.address.toByteArray().joinToString("") { "%02x".format(it) }}")
                println("  Create Time: ${account.createTime}")
                println("  Latest Operation Time: ${account.latestOprationTime}")
            }
            println()

            // Check for frozen balance (staked TRX)
            if (account.frozenCount > 0) {
                println("Frozen Balance (Staked TRX):")
                account.frozenList.forEach { frozen ->
                    val frozenTrx = BigDecimal(frozen.frozenBalance)
                        .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
                    println("  Amount: $frozenTrx TRX, Expire Time: ${frozen.expireTime}")
                }
                println()
            }

            println("=".repeat(80))
            println("SUCCESS: TRX balance query completed!")
            println("=".repeat(80))
        } catch (e: Exception) {
            println("Error querying account: ${e.message}")
            e.printStackTrace()

            // If the account doesn't exist, try alternative addresses
            println()
            println("Trying alternative test addresses...")
            tryAlternativeAddresses(channel)
        } finally {
            // Gracefully shutdown the channel
            channel.shutdown()
            channel.awaitTermination(5, TimeUnit.SECONDS)
            println("gRPC channel closed")
        }
    }

    @Test
    fun `test get TRX balance for multiple addresses`() {
        println("=".repeat(80))
        println("TEST: Query TRX Balance for Multiple Addresses")
        println("=".repeat(80))
        println()

        val channel = ManagedChannelBuilder
            .forAddress(SHASTA_GRPC_ENDPOINT, SHASTA_GRPC_PORT)
            .usePlaintext()
            .build()

        try {
            val walletStub = WalletGrpc.newBlockingStub(channel)

            ALTERNATIVE_TEST_ADDRESSES.forEachIndexed { index, address ->
                println("Address ${index + 1}: $address")
                try {
                    val addressBytes = decodeBase58Check(address)
                    val accountRequest = Protocol.Account.newBuilder()
                        .setAddress(ByteString.copyFrom(addressBytes))
                        .build()

                    val account = walletStub.getAccount(accountRequest)
                    val balanceInSun = account.balance
                    val balanceInTrx = BigDecimal(balanceInSun)
                        .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
                    println("  Balance: $balanceInTrx TRX ($balanceInSun SUN)")
                } catch (e: Exception) {
                    println("  Error: ${e.message}")
                }
                println()
            }

            println("=".repeat(80))
            println("Multi-address query completed!")
            println("=".repeat(80))
        } finally {
            channel.shutdown()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `test Shasta testnet connectivity`() {
        println("=".repeat(80))
        println("TEST: Verify Shasta Testnet Connectivity (gRPC)")
        println("=".repeat(80))
        println()

        println("Connecting to Shasta testnet...")
        println("  Endpoint: $SHASTA_GRPC_ENDPOINT:$SHASTA_GRPC_PORT")
        println()

        val channel = ManagedChannelBuilder
            .forAddress(SHASTA_GRPC_ENDPOINT, SHASTA_GRPC_PORT)
            .usePlaintext()
            .build()

        try {
            val walletStub = WalletGrpc.newBlockingStub(channel)

            // Get the latest block to verify connectivity
            val nowBlock = walletStub.getNowBlock(
                org.tron.api.GrpcAPI.EmptyMessage.getDefaultInstance()
            )
            val blockNumber = nowBlock.blockHeader.rawData.number
            val timestamp = nowBlock.blockHeader.rawData.timestamp

            println("Connection successful!")
            println()
            println("Latest Block Information:")
            println("  Block Number: $blockNumber")
            println("  Timestamp: $timestamp")
            println("  Transactions: ${nowBlock.transactionsCount}")
            println()

            // Display block header details
            println("Block Header:")
            val witnessAddress = nowBlock.blockHeader.rawData.witnessAddress.toByteArray()
            println("  Witness Address: ${encodeToBase58Check(witnessAddress)}")
            println()

            println("=".repeat(80))
            println("SUCCESS: Shasta testnet connection verified!")
            println("=".repeat(80))
        } catch (e: Exception) {
            println("Connection failed: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            channel.shutdown()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `test get account resources bandwidth and energy`() {
        println("=".repeat(80))
        println("TEST: Query Account Resources (Bandwidth & Energy)")
        println("=".repeat(80))
        println()

        val channel = ManagedChannelBuilder
            .forAddress(SHASTA_GRPC_ENDPOINT, SHASTA_GRPC_PORT)
            .usePlaintext()
            .build()

        try {
            val walletStub = WalletGrpc.newBlockingStub(channel)

            println("Querying account resources for: $TEST_ADDRESS")
            println()

            val addressBytes = decodeBase58Check(TEST_ADDRESS)
            val accountRequest = Protocol.Account.newBuilder()
                .setAddress(ByteString.copyFrom(addressBytes))
                .build()

            val accountResource = walletStub.getAccountResource(accountRequest)

            println("Bandwidth:")
            println("  Free Net Used: ${accountResource.freeNetUsed}")
            println("  Free Net Limit: ${accountResource.freeNetLimit}")
            println("  Net Used: ${accountResource.netUsed}")
            println("  Net Limit: ${accountResource.netLimit}")
            println()

            println("Energy:")
            println("  Energy Used: ${accountResource.energyUsed}")
            println("  Energy Limit: ${accountResource.energyLimit}")
            println()

            println("=".repeat(80))
            println("SUCCESS: Account resource query completed!")
            println("=".repeat(80))
        } catch (e: Exception) {
            println("Error querying resources: ${e.message}")
            e.printStackTrace()
        } finally {
            channel.shutdown()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `test send 0_1 TRX from TEST_ADDRESS to recipient`() {
        println("=".repeat(80))
        println("TEST: Send 0.1 TRX on Shasta Testnet")
        println("=".repeat(80))
        println()

        // Amount to send: 0.1 TRX = 100,000 SUN
        val amountInSun = (0.1 * SUN_PER_TRX).toLong()

        println("Transfer Details:")

        println("  From: $TEST_ADDRESS")
        println("  To: $RECIPIENT_ADDRESS")
        println("  Amount: 0.1 TRX ($amountInSun SUN)")
        println()

        val channel = ManagedChannelBuilder
            .forAddress(SHASTA_GRPC_ENDPOINT, SHASTA_GRPC_PORT)
            .usePlaintext()
            .build()

        try {
            val walletStub = WalletGrpc.newBlockingStub(channel)

            // Decode addresses
            val ownerAddressBytes = decodeBase58Check(TEST_ADDRESS)
            val toAddressBytes = decodeBase58Check(RECIPIENT_ADDRESS)

            // Check sender balance before transfer
            println("Checking sender balance before transfer...")
            val senderAccount = walletStub.getAccount(
                Protocol.Account.newBuilder()
                    .setAddress(ByteString.copyFrom(ownerAddressBytes))
                    .build()
            )
            val balanceBefore = senderAccount.balance
            val balanceBeforeTrx = BigDecimal(balanceBefore)
                .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
            println("  Sender balance: $balanceBeforeTrx TRX ($balanceBefore SUN)")

            if (balanceBefore < amountInSun) {
                println("ERROR: Insufficient balance for transfer!")
                println("Get testnet TRX from Shasta faucet: https://shasta.tronex.io/")
                return
            }
            println()

            // Step 1: Create TransferContract
            println("Step 1: Creating TransferContract...")
            val transferContract = BalanceContract.TransferContract.newBuilder()
                .setOwnerAddress(ByteString.copyFrom(ownerAddressBytes))
                .setToAddress(ByteString.copyFrom(toAddressBytes))
                .setAmount(amountInSun)
                .build()
            println("  TransferContract created")

            // Step 2: Create transaction using gRPC
            println("Step 2: Creating transaction via gRPC...")
            val transactionExtension = walletStub.createTransaction2(transferContract)

            if (!transactionExtension.result.result) {
                println("ERROR: Failed to create transaction: ${transactionExtension.result.message.toStringUtf8()}")
                return
            }

            var transaction = transactionExtension.transaction
            println("  Transaction created successfully")
            println("  TxID: ${getTransactionId(transaction)}")
            println()

            // Step 3: Sign the transaction
            println("Step 3: Signing transaction...")
            val privateKeyBytes = hexStringToByteArray(TEST_PRIVATE_KEY)
            val ecKey = ECKey.fromPrivate(privateKeyBytes)

            // Hash the transaction raw data
            val rawDataBytes = transaction.rawData.toByteArray()
            val hash = Sha256Sm3Hash.hash(rawDataBytes)

            // Sign the hash
            val signature = ecKey.sign(hash)
            val signatureBytes = signature.toByteArray()

            // Add signature to transaction
            transaction = transaction.toBuilder()
                .addSignature(ByteString.copyFrom(signatureBytes))
                .build()
            println("  Transaction signed successfully")
            println()

            // Step 4: Broadcast the transaction
            println("Step 4: Broadcasting transaction...")
            val broadcastResult = walletStub.broadcastTransaction(transaction)

            println("  Broadcast result: ${broadcastResult.result}")
            if (!broadcastResult.result) {
                println("  Error code: ${broadcastResult.code}")
                println("  Error message: ${broadcastResult.message.toStringUtf8()}")
            } else {
                println("  Transaction broadcasted successfully!")
                println()

                // Wait for confirmation
                println("Waiting for confirmation (10 seconds)...")
                Thread.sleep(10000)

                // Check transaction status
                val txId = getTransactionId(transaction)
                println("Checking transaction status...")
                val txInfo = walletStub.getTransactionInfoById(
                    GrpcAPI.BytesMessage.newBuilder()
                        .setValue(ByteString.copyFrom(hexStringToByteArray(txId)))
                        .build()
                )

                if (txInfo.id.isEmpty) {
                    println("  Transaction not yet confirmed, may need more time")
                } else {
                    println("  Transaction confirmed in block: ${txInfo.blockNumber}")
                    println("  Fee: ${txInfo.fee} SUN")
                    val resultStr = if (txInfo.result == Protocol.TransactionInfo.code.SUCESS) "SUCCESS" else "FAILED"
                    println("  Result: $resultStr")
                }

                // Check balances after transfer
                println()
                println("Checking balances after transfer...")
                val senderAfter = walletStub.getAccount(
                    Protocol.Account.newBuilder()
                        .setAddress(ByteString.copyFrom(ownerAddressBytes))
                        .build()
                )
                val recipientAfter = walletStub.getAccount(
                    Protocol.Account.newBuilder()
                        .setAddress(ByteString.copyFrom(toAddressBytes))
                        .build()
                )

                val senderBalanceAfter = BigDecimal(senderAfter.balance)
                    .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
                val recipientBalanceAfter = BigDecimal(recipientAfter.balance)
                    .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)

                println("  Sender balance after: $senderBalanceAfter TRX")
                println("  Recipient balance after: $recipientBalanceAfter TRX")
            }

            println()
            println("=".repeat(80))
            println("Transfer test completed!")
            println("=".repeat(80))
        } catch (e: Exception) {
            println("Error during transfer: ${e.message}")
            e.printStackTrace()
        } finally {
            channel.shutdown()
            channel.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /**
     * Get transaction ID (hash of raw data)
     */
    private fun getTransactionId(transaction: Protocol.Transaction): String {
        val rawDataBytes = transaction.rawData.toByteArray()
        val hash = Sha256Sm3Hash.hash(rawDataBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Convert hex string to byte array
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }

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
     * Compute SHA-256 hash
     */
    private fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * Helper function to try alternative addresses when the primary test address fails
     */
    private fun tryAlternativeAddresses(channel: io.grpc.ManagedChannel) {
        val walletStub = WalletGrpc.newBlockingStub(channel)

        ALTERNATIVE_TEST_ADDRESSES.forEach { address ->
            try {
                println("Trying address: $address")
                val addressBytes = decodeBase58Check(address)
                val accountRequest = Protocol.Account.newBuilder()
                    .setAddress(ByteString.copyFrom(addressBytes))
                    .build()

                val account = walletStub.getAccount(accountRequest)
                val balanceInSun = account.balance
                val balanceInTrx = BigDecimal(balanceInSun)
                    .divide(BigDecimal(SUN_PER_TRX), 6, RoundingMode.HALF_UP)
                println("  Balance: $balanceInTrx TRX ($balanceInSun SUN)")
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
            }
        }
    }
}
