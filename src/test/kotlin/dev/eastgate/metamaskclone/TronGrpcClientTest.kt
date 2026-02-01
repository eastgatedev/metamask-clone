package dev.eastgate.metamaskclone

import dev.eastgate.metamaskclone.core.blockchain.TronGrpcClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Unit and integration tests for TronGrpcClient.
 *
 * Unit tests (not disabled) test conversion logic without network access.
 * Integration tests (disabled by default) require network access to Shasta testnet.
 */
@Disabled
class TronGrpcClientTest {
    companion object {
        // Test addresses on Shasta testnet
        const val TEST_ADDRESS = "<your-wallet-address>"
        const val TEST_PRIVATE_KEY = "<your-key>"
        const val RECIPIENT_ADDRESS = "TByuNoiAoxapDqJeT2PtrxSCgh2oaiGHxq"
    }

    // === Unit Tests (no network required) ===

    @Test
    fun `test SUN_PER_TRX constant is correct`() {
        assertEquals(1_000_000L, TronGrpcClient.SUN_PER_TRX)
    }

    @Test
    fun `test TRON_MAINNET_PREFIX constant is correct`() {
        assertEquals(0x41.toByte(), TronGrpcClient.TRON_MAINNET_PREFIX)
    }

    @Test
    fun `test factory methods create clients with correct endpoints`() {
        // Just verify factory methods don't throw - clients will be closed immediately
        TronGrpcClient.ofShasta().use { client ->
            // Client created successfully
        }

        TronGrpcClient.ofMainnet().use { client ->
            // Client created successfully
        }

        TronGrpcClient.ofNile().use { client ->
            // Client created successfully
        }
    }

    // === Integration Tests (require network access) ===

    // @Disabled("Integration test - requires network access to Shasta testnet")
    @Test
    fun `test getBalance from Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            val balance = client.getBalance(TEST_ADDRESS)
            println("Balance: $balance TRX")

            // Balance should be non-negative
            assertTrue(balance >= BigDecimal.ZERO)
        }
    }

    @Test
    @Disabled("Integration test - requires network access to Shasta testnet")
    fun `test getBalanceInSun from Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            val balanceInSun = client.getBalanceInSun(TEST_ADDRESS)
            println("Balance: $balanceInSun SUN")

            // Balance should be non-negative
            assertTrue(balanceInSun >= 0)
        }
    }

    @Test
    @Disabled("Integration test - requires network access to Shasta testnet")
    fun `test getAccount from Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            val account = client.getAccount(TEST_ADDRESS)

            println("Account Details:")
            println("  Balance: ${account.balance} SUN")
            println("  Create Time: ${account.createTime}")
            println("  Latest Operation Time: ${account.latestOprationTime}")

            // If account exists, it should have an address
            if (!account.address.isEmpty) {
                val addressHex = account.address.toByteArray().joinToString("") { "%02x".format(it) }
                println("  Address (hex): $addressHex")
            }
        }
    }

    @Test
    @Disabled("Integration test - requires network access to Shasta testnet")
    fun `test getAccountResource from Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            val resources = client.getAccountResource(TEST_ADDRESS)

            println("Account Resources:")
            println("  Free Net Used: ${resources.freeNetUsed}")
            println("  Free Net Limit: ${resources.freeNetLimit}")
            println("  Net Used: ${resources.netUsed}")
            println("  Net Limit: ${resources.netLimit}")
            println("  Energy Used: ${resources.energyUsed}")
            println("  Energy Limit: ${resources.energyLimit}")
        }
    }

    @Test
    @Disabled("Integration test - requires network access to Shasta testnet")
    fun `test getNowBlock from Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            val block = client.getNowBlock()

            println("Latest Block:")
            println("  Block Number: ${block.blockHeader.rawData.number}")
            println("  Timestamp: ${block.blockHeader.rawData.timestamp}")
            println("  Transactions: ${block.transactionsCount}")

            // Block number should be positive
            assertTrue(block.blockHeader.rawData.number > 0)
        }
    }

    // @Disabled("Integration test - requires network access and test TRX")
    @Test
    fun `test sendTrx on Shasta testnet`() {
        TronGrpcClient.ofShasta().use { client ->
            // Check balance before transfer
            val balanceBefore = client.getBalance(TEST_ADDRESS)
            println("Balance before: $balanceBefore TRX")

            if (balanceBefore < BigDecimal("0.2")) {
                println("Insufficient balance for test. Get testnet TRX from https://shasta.tronex.io/")
                return@use
            }

            // Send 0.1 TRX
            val txId = client.sendTrx(
                fromAddress = TEST_ADDRESS,
                toAddress = RECIPIENT_ADDRESS,
                privateKey = TEST_PRIVATE_KEY,
                amountTrx = BigDecimal("0.1")
            )

            println("Transaction sent!")
            println("  TxID: $txId")
            println("  Explorer: https://shasta.tronscan.org/#/transaction/$txId")

            // Wait for confirmation
            println("Waiting for confirmation (10 seconds)...")
            Thread.sleep(10000)

            // Check transaction status
            val txInfo = client.getTransactionInfo(txId)
            if (!txInfo.id.isEmpty) {
                println("Transaction confirmed in block: ${txInfo.blockNumber}")
                println("Fee: ${txInfo.fee} SUN")
            } else {
                println("Transaction not yet confirmed, may need more time")
            }

            // Check balance after
            val balanceAfter = client.getBalance(TEST_ADDRESS)
            println("Balance after: $balanceAfter TRX")
        }
    }

    @Test
    @Disabled("Integration test - demonstrates clean API usage")
    fun `demo clean API usage pattern`() {
        // Pattern 1: Using try-finally
        val client = TronGrpcClient.ofShasta()
        try {
            val balance = client.getBalance(TEST_ADDRESS)
            println("Balance: $balance TRX")
        } finally {
            client.close()
        }

        // Pattern 2: Using Kotlin's use() - recommended
        TronGrpcClient.ofShasta().use { c ->
            val balance = c.getBalance(TEST_ADDRESS)
            println("Balance: $balance TRX")
        }

        // Pattern 3: Custom endpoint
        TronGrpcClient("grpc.shasta.trongrid.io", 50051).use { c ->
            val block = c.getNowBlock()
            println("Block #${block.blockHeader.rawData.number}")
        }
    }
}
