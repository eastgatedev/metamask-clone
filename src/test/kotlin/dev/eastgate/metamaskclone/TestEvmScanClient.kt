package dev.eastgate.metamaskclone

import dev.eastgate.metamaskclone.core.blockchain.EvmScanClient
import dev.eastgate.metamaskclone.core.blockchain.EvmTransactionType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
class TestEvmScanClient {
    private lateinit var client: EvmScanClient

    @BeforeEach
    fun setUp() {
        client = EvmScanClient(apiKey = API_KEY)
    }

    @AfterEach
    fun tearDown() {
        client.close()
    }

    // === Native Transaction Tests ===

    @Test
    fun `getNativeTransactions returns transactions for Sepolia testnet`() {
        val txs = client.getNativeTransactions(chainId = SEPOLIA_CHAIN_ID, walletAddress = WALLET_ADDRESS)

        assertTrue(txs.isNotEmpty(), "Should have at least one transaction")
        println("Sepolia transactions: ${txs.size}")

        val tx = txs.first()
        assertEquals(EvmTransactionType.NATIVE, tx.transactionType)
        assertTrue(tx.hash.startsWith("0x"), "Hash should start with 0x")
        assertTrue(tx.from.isNotEmpty(), "From should not be empty")
        assertTrue(tx.to.isNotEmpty(), "To should not be empty")
        assertTrue(tx.blockNumber.isNotEmpty(), "Block number should not be empty")
        assertTrue(tx.timeStamp > 0, "Timestamp should be positive")

        for ((i, t) in txs.withIndex()) {
            println("--- Transaction #${i + 1} ---")
            println("  type:         ${t.transactionType}")
            println("  blockNumber:  ${t.blockNumber}")
            println("  timeStamp:    ${t.timeStamp}")
            println("  hash:         ${t.hash}")
            println("  from:         ${t.from}")
            println("  to:           ${t.to}")
            println("  value (wei):  ${t.value}")
            println("  value (ETH):  ${t.displayValue}")
            println("  isError:      ${t.isError}")
            println("  methodId:     ${t.methodId}")
            println("  functionName: ${t.functionName}")
        }
    }

    @Test
    fun `getNativeTransactions respects pagination parameters`() {
        val txs = client.getNativeTransactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = WALLET_ADDRESS,
            page = 1,
            offset = 3,
            sort = "asc"
        )

        assertTrue(txs.size <= 3, "Should return at most 3 transactions")
        println("Paginated result count: ${txs.size}")

        if (txs.size >= 2) {
            assertTrue(
                txs[0].timeStamp <= txs[1].timeStamp,
                "Ascending sort: first tx should be older or same"
            )
        }
    }

    @Test
    fun `getNativeTransactions returns empty for address with no transactions`() {
        val txs = client.getNativeTransactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = "0xdead000000000000000000000000000000099999"
        )

        assertEquals(0, txs.size, "Dead address should have no native transactions")
        println("Empty result verified for zero-transaction address")
    }

    @Test
    fun `getNativeTransactions from and to addresses match wallet`() {
        val txs = client.getNativeTransactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = WALLET_ADDRESS,
            offset = 5
        )

        assertTrue(txs.isNotEmpty(), "Should have at least one transaction")

        val lowerWallet = WALLET_ADDRESS.lowercase()
        for (tx in txs) {
            val matches = tx.from.lowercase() == lowerWallet || tx.to.lowercase() == lowerWallet
            assertTrue(matches, "Transaction ${tx.hash} should involve wallet address")
        }
        println("All ${txs.size} transactions involve the queried wallet address")
    }

    // === ERC-20 Token Transaction Tests ===

    @Test
    fun `getErc20Transactions returns MUSDC transfers on Sepolia`() {
        val txs = client.getErc20Transactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = WALLET_ADDRESS,
            tokenAddress = MUSDC_TOKEN_ADDRESS
        )

        assertTrue(txs.isNotEmpty(), "Should have at least one ERC-20 transaction")
        println("MUSDC transactions: ${txs.size}")

        val tx = txs.first()
        assertEquals(EvmTransactionType.ERC20, tx.transactionType)
        assertTrue(tx.hash.startsWith("0x"), "Hash should start with 0x")
        assertEquals("MyUSDC", tx.tokenName)
        assertEquals("MUSDC", tx.tokenSymbol)
        assertEquals("6", tx.tokenDecimal)
        assertEquals(MUSDC_TOKEN_ADDRESS.lowercase(), tx.contractAddress.lowercase())

        for ((i, t) in txs.withIndex()) {
            println("--- ERC-20 Transaction #${i + 1} ---")
            println("  type:            ${t.transactionType}")
            println("  blockNumber:     ${t.blockNumber}")
            println("  timeStamp:       ${t.timeStamp}")
            println("  hash:            ${t.hash}")
            println("  from:            ${t.from}")
            println("  to:              ${t.to}")
            println("  value (raw):     ${t.value}")
            println("  value (MUSDC):   ${t.displayValue}")
            println("  tokenName:       ${t.tokenName}")
            println("  tokenSymbol:     ${t.tokenSymbol}")
            println("  tokenDecimal:    ${t.tokenDecimal}")
            println("  methodId:        ${t.methodId}")
            println("  functionName:    ${t.functionName}")
        }
    }

    @Test
    fun `getErc20Transactions correctly converts token value`() {
        val txs = client.getErc20Transactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = WALLET_ADDRESS,
            tokenAddress = MUSDC_TOKEN_ADDRESS
        )

        assertTrue(txs.isNotEmpty(), "Should have at least one ERC-20 transaction")

        val tx = txs.first()
        val expectedValue = java.math.BigDecimal(tx.value)
            .divide(java.math.BigDecimal(java.math.BigInteger.TEN.pow(6)))
        assertEquals(expectedValue, tx.displayValue)
        println("Raw value: ${tx.value} -> Token value: ${tx.displayValue} ${tx.tokenSymbol}")
    }

    @Test
    fun `getErc20Transactions from and to match wallet`() {
        val txs = client.getErc20Transactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = WALLET_ADDRESS,
            tokenAddress = MUSDC_TOKEN_ADDRESS
        )

        assertTrue(txs.isNotEmpty(), "Should have at least one ERC-20 transaction")

        val lowerWallet = WALLET_ADDRESS.lowercase()
        for (tx in txs) {
            val matches = tx.from.lowercase() == lowerWallet || tx.to.lowercase() == lowerWallet
            assertTrue(matches, "Transaction ${tx.hash} should involve wallet address")
        }
        println("All ${txs.size} ERC-20 transactions involve the queried wallet address")
    }

    @Test
    fun `getErc20Transactions returns empty for token with no transfers`() {
        val txs = client.getErc20Transactions(
            chainId = SEPOLIA_CHAIN_ID,
            walletAddress = "0xdead000000000000000000000000000000099999",
            tokenAddress = MUSDC_TOKEN_ADDRESS
        )

        assertEquals(0, txs.size, "Dead address should have no ERC-20 transactions")
        println("Empty result verified for zero-transaction address")
    }

    companion object {
        private const val API_KEY = "PNQJ9UZ4PECH7Q31TGDD21DQB3QYK6A5SR"
        private const val WALLET_ADDRESS = "0x12Ce45FD9110984C75C67517dAeBD64CF601345b"
        private const val MUSDC_TOKEN_ADDRESS = "0x161b10e5169a7D9752575fddE204214B6CBAEB8f"
        private const val SEPOLIA_CHAIN_ID = 11155111L
    }
}
