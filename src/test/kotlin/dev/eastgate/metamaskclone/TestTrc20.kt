package dev.eastgate.metamaskclone

import dev.eastgate.metamaskclone.core.blockchain.TronGrpcClient
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Integration tests for TRC20 token operations.
 * These tests require access to Shasta testnet.
 */
@Disabled("Integration test - requires Shasta testnet")
class TestTrc20 {
    companion object {
        const val WALLET_ADDRESS = "<your-wallet-address>"
        const val RECIPIENT_ADDRESS = "TByuNoiAoxapDqJeT2PtrxSCgh2oaiGHxq"
        const val TRC20_CONTRACT = "TYzo8tjvbRRFQwYfrrbf74vDE3S3apu2nD"
        const val PRIVATE_KEY = "<your-key>"
    }

    @Test
    fun `test get TRC20 token info`() {
        TronGrpcClient.ofShasta().use { client ->
            val tokenInfo = client.getTrc20TokenInfo(TRC20_CONTRACT)
            println("Token Name: ${tokenInfo.name}")
            println("Token Symbol: ${tokenInfo.symbol}")
            println("Token Decimals: ${tokenInfo.decimals}")
            println("Contract Address: ${tokenInfo.contractAddress}")
        }
    }

    @Test
    fun `test get TRC20 balance`() {
        TronGrpcClient.ofShasta().use { client ->
            // First get token info
            val tokenInfo = client.getTrc20TokenInfo(TRC20_CONTRACT)
            println("Token: ${tokenInfo.symbol} (${tokenInfo.decimals} decimals)")

            // Get balance (auto-fetches decimals)
            val balance = client.getTrc20Balance(WALLET_ADDRESS, TRC20_CONTRACT)
            println("Balance: $balance ${tokenInfo.symbol}")

            // Also get raw balance for comparison
            val rawBalance = client.getTrc20BalanceRaw(WALLET_ADDRESS, TRC20_CONTRACT)
            println("Raw Balance: $rawBalance")
        }
    }

    @Test
    fun `test send TRC20 tokens`() {
        TronGrpcClient.ofShasta().use { client ->
            // Get token info
            val tokenInfo = client.getTrc20TokenInfo(TRC20_CONTRACT)
            println("Sending ${tokenInfo.symbol}")

            // Check balances before
            val senderBalanceBefore = client.getTrc20Balance(WALLET_ADDRESS, TRC20_CONTRACT)
            val recipientBalanceBefore = client.getTrc20Balance(RECIPIENT_ADDRESS, TRC20_CONTRACT)
            println("Sender balance before: $senderBalanceBefore ${tokenInfo.symbol}")
            println("Recipient balance before: $recipientBalanceBefore ${tokenInfo.symbol}")

            // Send 1.2 EGT
            val amount = BigDecimal("1.2")
            val txId = client.sendTrc20(
                fromAddress = WALLET_ADDRESS,
                toAddress = RECIPIENT_ADDRESS,
                contractAddress = TRC20_CONTRACT,
                privateKey = PRIVATE_KEY,
                amount = amount
            )
            println("Transaction ID: $txId")
            println("Explorer: https://shasta.tronscan.org/#/transaction/$txId")

            // Wait for confirmation (10 seconds)
            Thread.sleep(10000)

            // Check transaction status
            val txInfo = client.getTransactionInfo(txId)
            val txResult = txInfo.receipt.result
            println("Transaction Result: $txResult")
            if (txResult.name != "SUCCESS") {
                throw RuntimeException("Transaction failed: $txResult")
            }

            // Check balances after
            val senderBalanceAfter = client.getTrc20Balance(WALLET_ADDRESS, TRC20_CONTRACT)
            val recipientBalanceAfter = client.getTrc20Balance(RECIPIENT_ADDRESS, TRC20_CONTRACT)
            println("Sender balance after: $senderBalanceAfter ${tokenInfo.symbol}")
            println("Recipient balance after: $recipientBalanceAfter ${tokenInfo.symbol}")
        }
    }
}
