package dev.eastgate.metamaskclone

import dev.eastgate.metamaskclone.core.blockchain.BitcoinRpcClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Integration tests for BitcoinRpcClient against a local regtest node.
 *
 * Prerequisites:
 * - Bitcoin Core running in regtest mode
 * - RPC credentials: bitcoin:bitcoin on port 18443
 *
 * Remove @Disabled to run against a live regtest node.
 */
@Disabled("Requires a running Bitcoin Core regtest node")
class TestBitcoin {
    private val rpcUrl = "http://bitcoinrpc:your_password_here@127.0.0.1:18443"

    @Test
    fun `generateNewAddress returns a non-empty address`() {
        BitcoinRpcClient(rpcUrl).use { client ->
            val address = client.generateNewAddress()
            assertTrue(address.isNotEmpty(), "Address should not be empty")
            println("Generated address: $address")
        }
    }

    @Test
    fun `getBalance returns a non-negative balance`() {
        BitcoinRpcClient(rpcUrl).use { client ->
            val balance = client.getBalance()
            assertNotNull(balance)
            assertTrue(balance >= 0.0, "Balance should be non-negative")
            println("Wallet balance: $balance BTC")
        }
    }

    @Test
    fun `sendToAddress sends BTC and returns txid`() {
        BitcoinRpcClient(rpcUrl).use { client ->
            // Required on regtest — no fee estimation data available
            client.setTxFee(0.00001)

            val toAddress = "bcrt1q625xghkkvytks2usqw7yw4tpzlgwy7az3zacce"
            val amount = 0.1

            val txid = client.sendToAddress(toAddress, amount)
            assertTrue(txid.isNotEmpty(), "Transaction ID should not be empty")
            println("Sent $amount BTC to $toAddress")
            println("Transaction ID: $txid")
        }
    }

    @Test
    fun `listTransactions returns recent transactions`() {
        BitcoinRpcClient(rpcUrl).use { client ->
            val transactions = client.listTransactions(10)
            assertTrue(transactions.isNotEmpty(), "Should have at least one transaction")
            println("Transaction count: ${transactions.size}")
            for ((i, tx) in transactions.withIndex()) {
                println("--- Transaction #${i + 1} ---")
                println("  address:       ${tx.address}")
                println("  category:      ${tx.category}")
                println("  amount:        ${tx.amount}")
                println("  txid:          ${tx.txid}")
                println("  time:          ${tx.time}")
                println("  timereceived:  ${tx.timereceived}")
                println("  blockheight:   ${tx.blockheight ?: "N/A"}")
                println("  blockhash:     ${tx.blockhash ?: "N/A"}")
                println("  fee:           ${tx.fee ?: "N/A"}")
                println("  confirmations: ${tx.confirmations}")
            }
        }
    }
}
