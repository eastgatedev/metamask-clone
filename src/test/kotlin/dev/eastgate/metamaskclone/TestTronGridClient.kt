package dev.eastgate.metamaskclone

import dev.eastgate.metamaskclone.core.blockchain.TronGridClient
import dev.eastgate.metamaskclone.core.blockchain.TronTransactionType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class TestTronGridClient {
    private lateinit var client: TronGridClient

    @BeforeEach
    fun setUp() {
        client = TronGridClient.ofShasta()
    }

    @AfterEach
    fun tearDown() {
        client.close()
    }

    @Test
    fun `getTrxTransactions returns transactions on Shasta`() {
        val txs = client.getTrxTransactions(WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one transaction")
        println("Shasta transactions: ${txs.size}")

        val tx = txs.first()
        assertEquals(TronTransactionType.TRX, tx.transactionType)
        assertTrue(tx.txID.isNotEmpty(), "txID should not be empty")
        assertNotNull(tx.blockNumber, "Block number should not be null for TRX")
        assertTrue(tx.blockNumber!! > 0, "Block number should be positive")
        assertTrue(tx.blockTimestamp > 0, "Block timestamp should be positive")
        assertTrue(tx.contractType.isNotEmpty(), "Contract type should not be empty")
        assertTrue(tx.contractResult!!.isNotEmpty(), "Contract result should not be empty")

        for ((i, t) in txs.withIndex()) {
            println("--- Transaction #${i + 1} ---")
            println("  txID:            ${t.txID}")
            println("  blockNumber:     ${t.blockNumber}")
            println("  blockTimestamp:   ${t.blockTimestamp}")
            println("  contractType:    ${t.contractType}")
            println("  contractResult:  ${t.contractResult}")
            println("  fee (SUN):       ${t.fee}")
            println("  ownerAddress:    ${t.ownerAddress}")
            println("  toAddress:       ${t.toAddress}")
            println("  amount (SUN):    ${t.amount}")
            println("  amount (TRX):    ${t.amountInTrx}")
            println("  netUsage:        ${t.netUsage}")
            println("  netFee:          ${t.netFee}")
            println("  energyFee:       ${t.energyFee}")
            println("  energyTotal:     ${t.energyUsageTotal}")
        }
    }

    @Test
    fun `getTrxTransactions only contains native TRX transfers`() {
        val txs = client.getTrxTransactions(WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one transaction")

        for (tx in txs) {
            assertTrue(
                tx.contractType != "TriggerSmartContract" && tx.contractType != "CreateSmartContract",
                "Should not contain smart contract transactions, found: ${tx.contractType}"
            )
            assertTrue(tx.amount!! > 0, "TransferContract should have positive amount")
            assertTrue(tx.ownerAddress.startsWith("T"), "Owner address should be Base58")
            assertTrue(tx.toAddress.startsWith("T"), "To address should be Base58")
        }
        println("All ${txs.size} transactions are native TRX transfers")
    }

    @Test
    fun `getTrxTransactions amountInTrx converts correctly`() {
        val txs = client.getTrxTransactions(WALLET_ADDRESS)
        val transfer = txs.first { it.contractType == "TransferContract" }

        val expectedTrx = java.math.BigDecimal(transfer.amount!!)
            .divide(java.math.BigDecimal(1_000_000))
        assertEquals(expectedTrx, transfer.amountInTrx)
        println("Amount: ${transfer.amount} SUN -> ${transfer.amountInTrx} TRX")
    }

    @Test
    fun `getTrxTransactions returns empty for invalid account address`() {
        val txs = client.getTrxTransactions("TAaaaaaaaaaaaaaaaaaaaaaaaaaaaajbMn")

        assertEquals(0, txs.size, "Invalid account should return empty list")
        println("Empty result verified for invalid account address")
    }

    // === TRC20 Transaction Tests ===

    @Test
    fun `getTrc20Transactions returns EGT transfers on Shasta`() {
        val txs = client.getTrc20Transactions(WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one TRC20 transaction")
        println("TRC20 transactions: ${txs.size}")

        val tx = txs.first()
        assertEquals(TronTransactionType.TRC20, tx.transactionType)
        assertTrue(tx.txID.isNotEmpty(), "Transaction ID should not be empty")
        assertTrue(tx.blockTimestamp > 0, "Block timestamp should be positive")
        assertTrue(tx.ownerAddress.startsWith("T"), "From address should be Base58")
        assertTrue(tx.toAddress.startsWith("T"), "To address should be Base58")
        assertTrue(tx.tokenName!!.isNotEmpty(), "Token name should not be empty")
        assertTrue(tx.tokenSymbol!!.isNotEmpty(), "Token symbol should not be empty")
        assertTrue(tx.tokenDecimal!! > 0, "Token decimal should be positive")

        for ((i, t) in txs.withIndex()) {
            println("--- TRC20 Transaction #${i + 1} ---")
            println("  txID:            ${t.txID}")
            println("  blockTimestamp:   ${t.blockTimestamp}")
            println("  from:            ${t.ownerAddress}")
            println("  to:              ${t.toAddress}")
            println("  value (raw):     ${t.value}")
            println("  value (token):   ${t.displayValue} ${t.tokenSymbol}")
            println("  type:            ${t.contractType}")
            println("  tokenName:       ${t.tokenName}")
            println("  tokenSymbol:     ${t.tokenSymbol}")
            println("  tokenAddress:    ${t.tokenAddress}")
            println("  tokenDecimal:    ${t.tokenDecimal}")
        }
    }

    @Test
    fun `getTrc20Transactions displayValue converts correctly`() {
        val txs = client.getTrc20Transactions(WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one TRC20 transaction")

        val tx = txs.first()
        val expected = java.math.BigDecimal(tx.value!!)
            .divide(java.math.BigDecimal(java.math.BigInteger.TEN.pow(tx.tokenDecimal!!)))
        assertEquals(expected, tx.displayValue)
        println("Raw: ${tx.value} -> ${tx.displayValue} ${tx.tokenSymbol}")
    }

    @Test
    fun `getTrc20Transactions from and to match wallet`() {
        val txs = client.getTrc20Transactions(WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one TRC20 transaction")

        for (tx in txs) {
            val matches = tx.ownerAddress == WALLET_ADDRESS || tx.toAddress == WALLET_ADDRESS
            assertTrue(matches, "Transaction ${tx.txID} should involve wallet address")
        }
        println("All ${txs.size} TRC20 transactions involve the queried wallet address")
    }

    @Test
    fun `getTrc20Transactions returns empty for invalid account address`() {
        val txs = client.getTrc20Transactions("TAaaaaaaaaaaaaaaaaaaaaaaaaaaaajbMn")

        assertEquals(0, txs.size, "Invalid account should return empty list")
        println("TRC20 empty result verified for invalid account address")
    }

    companion object {
        private const val WALLET_ADDRESS = "TVM3ECHPsVHjxf7a4p6tBv6NVWpUhLPJSW"
    }
}
