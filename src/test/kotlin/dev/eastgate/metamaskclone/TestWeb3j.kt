package dev.eastgate.metamaskclone

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.utils.Convert
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.tx.Transfer
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger

@Disabled
class TestWeb3j {

    companion object {
        // BNB Testnet (BSC Testnet) configuration
        const val BNB_TESTNET_RPC_URL = "https://data-seed-prebsc-1-s1.binance.org:8545/"
        const val BNB_TESTNET_CHAIN_ID = 97L

        // A known testnet faucet address (BSC Testnet Faucet)
        const val TEST_ADDRESS = "<your-wallet-address>"
        const val privateKey = "<your-key>"
        const val recipientAddress = "0x758C9dd3f46E64Cd01C1E83b3e4D76feE18a2301"
    }

    @Test
    fun `test get BNB balance from testnet`() {
        // Create Web3j instance connecting to BNB Testnet
        val web3j = Web3j.build(HttpService(BNB_TESTNET_RPC_URL))

        try {
            // Verify connection by getting chain ID
            val chainId = web3j.ethChainId().send()
            assertFalse(chainId.hasError(), "Failed to get chain ID: ${chainId.error?.message}")
            assertEquals(BNB_TESTNET_CHAIN_ID, chainId.chainId.toLong(), "Chain ID mismatch")
            println("Connected to BNB Testnet, Chain ID: ${chainId.chainId}")

            // Get balance for the test address
            val balanceResponse = web3j.ethGetBalance(
                TEST_ADDRESS,
                DefaultBlockParameterName.LATEST
            ).send()

            assertFalse(balanceResponse.hasError(), "Failed to get balance: ${balanceResponse.error?.message}")

            val balanceInWei = balanceResponse.balance
            val balanceInBnb = Convert.fromWei(BigDecimal(balanceInWei), Convert.Unit.ETHER)

            println("Address: $TEST_ADDRESS")
            println("Balance in Wei: $balanceInWei")
            println("Balance in BNB: $balanceInBnb BNB")

            // Assert that we got a valid balance (non-null)
            assertNotNull(balanceInWei, "Balance should not be null")
            assertTrue(balanceInWei >= java.math.BigInteger.ZERO, "Balance should be non-negative")

        } finally {
            web3j.shutdown()
        }
    }

    @Test
    fun `test get block number from BNB testnet`() {
        val web3j = Web3j.build(HttpService(BNB_TESTNET_RPC_URL))

        try {
            val blockNumber = web3j.ethBlockNumber().send()

            assertFalse(blockNumber.hasError(), "Failed to get block number: ${blockNumber.error?.message}")

            println("Current BNB Testnet block number: ${blockNumber.blockNumber}")

            assertTrue(
                blockNumber.blockNumber > java.math.BigInteger.ZERO,
                "Block number should be greater than 0"
            )
        } finally {
            web3j.shutdown()
        }
    }

    @Test
    fun `test get gas price from BNB testnet`() {
        val web3j = Web3j.build(HttpService(BNB_TESTNET_RPC_URL))

        try {
            val gasPrice = web3j.ethGasPrice().send()

            assertFalse(gasPrice.hasError(), "Failed to get gas price: ${gasPrice.error?.message}")

            val gasPriceInGwei = Convert.fromWei(BigDecimal(gasPrice.gasPrice), Convert.Unit.GWEI)

            println("Current gas price: ${gasPrice.gasPrice} Wei")
            println("Current gas price: $gasPriceInGwei Gwei")

            assertTrue(
                gasPrice.gasPrice > java.math.BigInteger.ZERO,
                "Gas price should be greater than 0"
            )
        } finally {
            web3j.shutdown()
        }
    }

    @Test
    fun `test transfer BNB on testnet`() {
        val web3j = Web3j.build(HttpService(BNB_TESTNET_RPC_URL))
        val amountToSend = Convert.toWei(BigDecimal("0.00001"), Convert.Unit.ETHER).toBigInteger()

        try {
            // Load credentials from private key
            val credentials = Credentials.create(privateKey)
            println("Sender address: ${credentials.address}")
            println("Recipient address: $recipientAddress")
            println("Amount to send: 0.00001 BNB ($amountToSend Wei)")

            // Get sender's balance before transfer
            val balanceBefore = web3j.ethGetBalance(
                credentials.address,
                DefaultBlockParameterName.LATEST
            ).send().balance
            val balanceInBnb = Convert.fromWei(BigDecimal(balanceBefore), Convert.Unit.ETHER)
            println("Sender balance before: $balanceInBnb BNB")

            // Check if sender has enough balance
            if (balanceBefore < amountToSend) {
                println("WARNING: Insufficient balance for transfer. Need testnet BNB from faucet.")
                println("Get testnet BNB from: https://testnet.bnbchain.org/faucet-smart")
                return
            }

            // Get the nonce (transaction count)
            val nonce = web3j.ethGetTransactionCount(
                credentials.address,
                DefaultBlockParameterName.PENDING
            ).send().transactionCount
            println("Nonce: $nonce")

            // Get current gas price
            val gasPrice = web3j.ethGasPrice().send().gasPrice
            println("Gas price: $gasPrice Wei")

            // Gas limit for simple BNB transfer (21000 is standard for ETH/BNB transfers)
            val gasLimit = BigInteger.valueOf(21000)

            // Create raw transaction
            val rawTransaction = RawTransaction.createEtherTransaction(
                nonce,
                gasPrice,
                gasLimit,
                recipientAddress,
                amountToSend
            )

            // Sign the transaction with chain ID (EIP-155)
            val signedMessage = TransactionEncoder.signMessage(
                rawTransaction,
                BNB_TESTNET_CHAIN_ID,
                credentials
            )
            val hexValue = Numeric.toHexString(signedMessage)
            println("Signed transaction: ${hexValue.take(66)}...")

            // Send the transaction
            val transactionResponse = web3j.ethSendRawTransaction(hexValue).send()

            if (transactionResponse.hasError()) {
                fail<Nothing>("Transaction failed: ${transactionResponse.error.message}")
            }

            val txHash = transactionResponse.transactionHash
            assertNotNull(txHash, "Transaction hash should not be null")
            println("Transaction hash: $txHash")
            println("View on BSCScan: https://testnet.bscscan.com/tx/$txHash")

            // Wait for transaction receipt (poll every 1 second, max 30 seconds)
            println("Waiting for transaction confirmation...")
            var receipt: org.web3j.protocol.core.methods.response.TransactionReceipt? = null
            for (i in 1..30) {
                Thread.sleep(1000)
                val receiptResponse = web3j.ethGetTransactionReceipt(txHash).send()
                if (receiptResponse.transactionReceipt.isPresent) {
                    receipt = receiptResponse.transactionReceipt.get()
                    break
                }
                print(".")
            }
            println()

            assertNotNull(receipt, "Transaction receipt should not be null after 30 seconds")
            println("Transaction confirmed in block: ${receipt!!.blockNumber}")
            println("Gas used: ${receipt.gasUsed}")
            println("Status: ${if (receipt.status == "0x1") "SUCCESS" else "FAILED"}")

            assertEquals("0x1", receipt.status, "Transaction should be successful")

            // Wait a moment for balance to update
            Thread.sleep(2000)

            // Get sender's balance after transfer
            val balanceAfter = web3j.ethGetBalance(
                credentials.address,
                DefaultBlockParameterName.LATEST
            ).send().balance
            val balanceAfterInBnb = Convert.fromWei(BigDecimal(balanceAfter), Convert.Unit.ETHER)
            println("Sender balance after: $balanceAfterInBnb BNB")

            // Calculate and show the difference
            val balanceDiff = balanceBefore.subtract(balanceAfter)
            val balanceDiffInBnb = Convert.fromWei(BigDecimal(balanceDiff), Convert.Unit.ETHER)
            println("Balance difference: $balanceDiffInBnb BNB ($balanceDiff Wei)")
            println("Expected minimum deduction: 0.00001 BNB (transfer) + gas fees")

            // Verify balance decreased (using compareTo for BigInteger)
            assertTrue(
                balanceAfter.compareTo(balanceBefore) < 0,
                "Balance should decrease after transfer. Before: $balanceBefore, After: $balanceAfter"
            )

        } finally {
            web3j.shutdown()
        }
    }

    @Test
    fun testSendBnbTransfer() {
        val web3j = Web3j.build(HttpService(BNB_TESTNET_RPC_URL))
        // Load credentials from private key
        val credentials = Credentials.create(privateKey)

        val transactionReceipt = Transfer.sendFunds(
            web3j,
            credentials,
            recipientAddress,
            0.0001.toBigDecimal(),
            Convert.Unit.ETHER
        ).send()

        println("Transaction receipt: ${transactionReceipt.transactionHash} >> $transactionReceipt.")
    }
}
