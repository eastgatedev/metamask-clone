package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.blockchain.BalanceResult
import dev.eastgate.metamaskclone.core.blockchain.BlockchainService
import dev.eastgate.metamaskclone.core.blockchain.GasPriceResult
import dev.eastgate.metamaskclone.core.blockchain.TokenBalanceResult
import dev.eastgate.metamaskclone.core.blockchain.TokenTransferResult
import dev.eastgate.metamaskclone.core.blockchain.TransactionResult
import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import dev.eastgate.metamaskclone.models.Token
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.utils.ClipboardUtil
import kotlinx.coroutines.*
import org.web3j.utils.Convert
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.swing.*

class SendTokenDialog(
    private val project: Project,
    private val wallet: Wallet,
    private val network: Network,
    private val walletManager: WalletManager,
    private val blockchainService: BlockchainService,
    private val token: Token? = null,
    private val availableTokens: List<Token> = emptyList()
) : DialogWrapper(project) {
    private val toAddressField = JBTextField()
    private val amountField = JBTextField()
    private val tokenSelector = JComboBox<String>()
    private val gasLimitField = JBTextField("21000")
    private val gasPriceField = JBTextField("Loading...")
    private val balanceLabel = JLabel("Loading...")
    private val estimatedFeeLabel = JLabel("--")
    private val totalLabel = JLabel("--")

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Store balance for validation
    private var currentBalanceWei: BigInteger = BigInteger.ZERO

    // Loading state tracking
    private val statusLabel = JLabel()
    private var gasPriceLoaded = false
    private var balanceLoaded = false

    init {
        title = "Send ${network.symbol}"
        isOKActionEnabled = false // Disable until data is loaded
        init()
        setupTokenSelector()
        fetchGasPrice()
        fetchBalance()
    }

    private fun setupTokenSelector() {
        // Add native token option
        tokenSelector.addItem("${network.symbol} (Native Token)")

        // Add available tokens
        for (t in availableTokens) {
            tokenSelector.addItem("${t.symbol} - ${t.name}")
        }

        // Select the provided token if any
        if (token != null) {
            val index = availableTokens.indexOfFirst { it.id == token.id }
            if (index >= 0) {
                tokenSelector.selectedIndex = index + 1 // +1 because of native token
            }
        }

        // Add listener to update gas limit and balance when token changes
        tokenSelector.addItemListener { event ->
            if (event.stateChange == java.awt.event.ItemEvent.SELECTED) {
                onTokenSelectionChanged()
            }
        }
    }

    private fun onTokenSelectionChanged() {
        if (tokenSelector.selectedIndex == 0) {
            // Native token selected
            gasLimitField.text = "21000"
            fetchBalance()
        } else {
            // ERC20 token selected
            gasLimitField.text = "100000"
            fetchTokenBalance()
        }
        updateTotalEstimate()
    }

    private fun fetchTokenBalance() {
        val tokenIndex = tokenSelector.selectedIndex - 1
        if (tokenIndex < 0 || tokenIndex >= availableTokens.size) return

        val selectedToken = availableTokens[tokenIndex]
        balanceLabel.text = "Loading..."
        balanceLabel.foreground = java.awt.Color.GRAY

        scope.launch {
            val result = blockchainService.getTokenBalance(
                contractAddress = selectedToken.contractAddress,
                walletAddress = wallet.address,
                decimals = selectedToken.decimals,
                network = network
            )

            SwingUtilities.invokeLater {
                when (result) {
                    is TokenBalanceResult.Success -> {
                        currentBalanceWei = result.balanceRaw
                        val balanceFormatted = formatBalance(result.balanceFormatted)
                        balanceLabel.text = "$balanceFormatted ${selectedToken.symbol}"
                        balanceLabel.foreground = java.awt.Color.DARK_GRAY
                    }
                    is TokenBalanceResult.Error -> {
                        balanceLabel.text = "Failed to load"
                        balanceLabel.foreground = java.awt.Color.RED
                    }
                }
            }
        }
    }

    private fun fetchGasPrice() {
        gasPriceField.isEnabled = false
        gasLimitField.isEnabled = false

        scope.launch {
            val result = blockchainService.getGasPrice(network)
            SwingUtilities.invokeLater {
                when (result) {
                    is GasPriceResult.Success -> {
                        val gasPriceGwei = Convert.fromWei(BigDecimal(result.gasPrice), Convert.Unit.GWEI)
                        gasPriceField.text = gasPriceGwei.setScale(2, RoundingMode.CEILING).toPlainString()
                        gasPriceField.isEnabled = true
                        gasLimitField.isEnabled = true
                        gasPriceLoaded = true
                        checkLoadingComplete()
                    }
                    is GasPriceResult.Error -> {
                        gasPriceField.text = "5" // Default fallback
                        gasPriceField.isEnabled = true
                        gasLimitField.isEnabled = true
                        gasPriceLoaded = true
                        checkLoadingComplete()
                    }
                }
            }
        }
    }

    private fun fetchBalance() {
        balanceLabel.text = "Loading..."
        balanceLabel.foreground = java.awt.Color.GRAY

        scope.launch {
            val result = blockchainService.getBalance(wallet.address, network)
            SwingUtilities.invokeLater {
                when (result) {
                    is BalanceResult.Success -> {
                        currentBalanceWei = result.balanceWei
                        val balanceFormatted = formatBalance(result.balanceFormatted)
                        balanceLabel.text = "$balanceFormatted ${network.symbol}"
                        balanceLabel.foreground = java.awt.Color.DARK_GRAY
                        balanceLoaded = true
                        checkLoadingComplete()
                    }
                    is BalanceResult.Error -> {
                        balanceLabel.text = "Failed to load"
                        balanceLabel.foreground = java.awt.Color.RED
                        statusLabel.text = "Error loading balance"
                        statusLabel.foreground = java.awt.Color.RED
                        balanceLoaded = true
                        isOKActionEnabled = true // Allow retry or cancel
                    }
                }
            }
        }
    }

    private fun formatBalance(balance: String): String {
        return try {
            val value = balance.toBigDecimalOrNull() ?: return balance
            if (value.scale() > 6) {
                value.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            } else {
                value.stripTrailingZeros().toPlainString()
            }
        } catch (e: Exception) {
            balance
        }
    }

    private fun checkLoadingComplete() {
        if (gasPriceLoaded && balanceLoaded) {
            statusLabel.text = "Ready to send"
            statusLabel.foreground = java.awt.Color(0x059669) // Green
            isOKActionEnabled = true
            updateTotalEstimate() // Calculate initial estimate
        }
    }

    private fun updateTotalEstimate() {
        try {
            val amount = amountField.text.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
            val gasLimit = gasLimitField.text.trim().toLongOrNull() ?: 21000L
            val gasPriceGwei = gasPriceField.text.trim().toBigDecimalOrNull() ?: BigDecimal.ZERO

            // Calculate fee: gasLimit * gasPrice (convert Gwei to Ether)
            val gasPriceEther = gasPriceGwei.divide(BigDecimal("1000000000"), 18, RoundingMode.HALF_UP)
            val fee = BigDecimal(gasLimit).multiply(gasPriceEther)
            val total = amount.add(fee)

            // Format for display (max 8 decimal places)
            val feeFormatted = fee.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
            val totalFormatted = total.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

            estimatedFeeLabel.text = "$feeFormatted ${network.symbol}"
            totalLabel.text = "$totalFormatted ${network.symbol}"
            totalLabel.font = totalLabel.font.deriveFont(java.awt.Font.BOLD)
        } catch (e: Exception) {
            estimatedFeeLabel.text = "--"
            totalLabel.text = "--"
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // Status label at top
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        statusLabel.text = "Fetching network data..."
        statusLabel.font = statusLabel.font.deriveFont(java.awt.Font.ITALIC)
        statusLabel.foreground = java.awt.Color.GRAY
        panel.add(statusLabel, gbc)

        // Network info
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        val networkLabel = JLabel("Network: ${network.name}")
        networkLabel.font = networkLabel.font.deriveFont(java.awt.Font.ITALIC)
        panel.add(networkLabel, gbc)

        // From address (read-only)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("From:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        val fromField = JBTextField("${wallet.name} (${wallet.getShortAddress()})")
        fromField.isEditable = false
        fromField.toolTipText = wallet.address
        panel.add(fromField, gbc)

        // Balance display
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Balance:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        balanceLabel.font = balanceLabel.font.deriveFont(java.awt.Font.BOLD)
        panel.add(balanceLabel, gbc)

        // To address
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("To:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        toAddressField.columns = 30
        toAddressField.toolTipText = "Recipient address (0x...)"
        panel.add(toAddressField, gbc)

        // Token selector
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Token:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(tokenSelector, gbc)

        // Amount
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Amount:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        amountField.toolTipText = "Amount to send"
        panel.add(amountField, gbc)

        // Gas settings section
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        panel.add(JSeparator(), gbc)

        row++
        gbc.gridy = row
        val gasLabel = JLabel("Gas Settings")
        gasLabel.font = gasLabel.font.deriveFont(java.awt.Font.BOLD)
        panel.add(gasLabel, gbc)

        // Gas Limit
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Gas Limit:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gasLimitField.toolTipText = "Gas limit (21000 for simple transfers)"
        panel.add(gasLimitField, gbc)

        // Gas Price
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Gas Price (Gwei):"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gasPriceField.toolTipText = "Gas price in Gwei"
        panel.add(gasPriceField, gbc)

        // Estimated fee
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Est. Fee:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        estimatedFeeLabel.foreground = java.awt.Color.GRAY
        panel.add(estimatedFeeLabel, gbc)

        // Total
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Total:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        totalLabel.foreground = java.awt.Color(0x059669) // Green
        panel.add(totalLabel, gbc)

        // Add document listeners to update total when values change
        val updateListener = object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = updateTotalEstimate()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = updateTotalEstimate()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = updateTotalEstimate()
        }
        amountField.document.addDocumentListener(updateListener)
        gasLimitField.document.addDocumentListener(updateListener)
        gasPriceField.document.addDocumentListener(updateListener)

        return panel
    }

    override fun doOKAction() {
        val toAddress = toAddressField.text.trim()
        val amount = amountField.text.trim()

        // Basic validation
        if (toAddress.isEmpty()) {
            Messages.showErrorDialog("Please enter a recipient address", "Validation Error")
            return
        }

        if (!toAddress.startsWith("0x") || toAddress.length != 42) {
            Messages.showErrorDialog("Invalid address format. Must be a 42-character hex address starting with 0x", "Validation Error")
            return
        }

        if (amount.isEmpty()) {
            Messages.showErrorDialog("Please enter an amount", "Validation Error")
            return
        }

        val amountValue = amount.toBigDecimalOrNull()
        if (amountValue == null || amountValue <= BigDecimal.ZERO) {
            Messages.showErrorDialog("Please enter a valid positive amount", "Validation Error")
            return
        }

        // Validate gas settings
        val gasLimitValue = gasLimitField.text.toLongOrNull()
        if (gasLimitValue == null || gasLimitValue <= 0) {
            Messages.showErrorDialog("Please enter a valid gas limit", "Validation Error")
            return
        }

        val gasPriceValue = gasPriceField.text.toBigDecimalOrNull()
        if (gasPriceValue == null || gasPriceValue <= BigDecimal.ZERO) {
            Messages.showErrorDialog("Please enter a valid gas price", "Validation Error")
            return
        }

        // Calculate gas fees
        val gasPriceWei = Convert.toWei(gasPriceValue, Convert.Unit.GWEI).toBigInteger()
        val gasLimit = BigInteger.valueOf(gasLimitValue)
        val gasFeeWei = gasPriceWei.multiply(gasLimit)

        // Disable dialog while checking balance
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        // Check balance before prompting for password
        val isNativeToken = tokenSelector.selectedIndex == 0
        val selectedToken = if (!isNativeToken && tokenSelector.selectedIndex - 1 < availableTokens.size) {
            availableTokens[tokenSelector.selectedIndex - 1]
        } else {
            null
        }

        scope.launch {
            // Always need native token for gas
            val nativeBalanceResult = blockchainService.getBalance(wallet.address, network)

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (nativeBalanceResult) {
                    is BalanceResult.Success -> {
                        if (isNativeToken) {
                            // Native token: check amount + gas fees
                            val amountWei = Convert.toWei(amountValue, Convert.Unit.ETHER).toBigInteger()
                            val totalRequiredWei = amountWei.add(gasFeeWei)

                            if (nativeBalanceResult.balanceWei < totalRequiredWei) {
                                val balanceFormatted = Convert.fromWei(BigDecimal(nativeBalanceResult.balanceWei), Convert.Unit.ETHER)
                                val totalRequired = Convert.fromWei(BigDecimal(totalRequiredWei), Convert.Unit.ETHER)
                                val gasFee = Convert.fromWei(BigDecimal(gasFeeWei), Convert.Unit.ETHER)

                                Messages.showErrorDialog(
                                    project,
                                    """
                                    Insufficient balance for this transaction.

                                    Your balance: $balanceFormatted ${network.symbol}
                                    Amount to send: $amountValue ${network.symbol}
                                    Estimated gas fee: $gasFee ${network.symbol}
                                    Total required: $totalRequired ${network.symbol}
                                    """.trimIndent(),
                                    "Insufficient Balance"
                                )
                                return@invokeLater
                            }

                            // Balance is sufficient, proceed with password prompt
                            proceedWithNativeTransaction(toAddress, amountValue, gasLimit, gasPriceWei)
                        } else {
                            // ERC20 token: check gas fees only from native balance
                            if (nativeBalanceResult.balanceWei < gasFeeWei) {
                                val balanceFormatted = Convert.fromWei(BigDecimal(nativeBalanceResult.balanceWei), Convert.Unit.ETHER)
                                val gasFee = Convert.fromWei(BigDecimal(gasFeeWei), Convert.Unit.ETHER)

                                Messages.showErrorDialog(
                                    project,
                                    """
                                    Insufficient ${network.symbol} for gas fees.

                                    Your ${network.symbol} balance: $balanceFormatted
                                    Estimated gas fee: $gasFee ${network.symbol}
                                    """.trimIndent(),
                                    "Insufficient Gas"
                                )
                                return@invokeLater
                            }

                            // Check token balance (use stored balance or fetch again)
                            val tokenAmountWei = amountValue.multiply(BigDecimal(BigInteger.TEN.pow(selectedToken!!.decimals))).toBigInteger()
                            if (currentBalanceWei < tokenAmountWei) {
                                Messages.showErrorDialog(
                                    project,
                                    """
                                    Insufficient ${selectedToken.symbol} balance.

                                    Your balance: ${balanceLabel.text}
                                    Amount to send: $amountValue ${selectedToken.symbol}
                                    """.trimIndent(),
                                    "Insufficient Balance"
                                )
                                return@invokeLater
                            }

                            // Balance is sufficient, proceed with token transfer
                            proceedWithTokenTransaction(selectedToken, toAddress, amountValue, gasLimit, gasPriceWei)
                        }
                    }
                    is BalanceResult.Error -> {
                        Messages.showErrorDialog(
                            project,
                            "Failed to check balance: ${nativeBalanceResult.message}",
                            "Error"
                        )
                    }
                }
            }
        }
    }

    private fun proceedWithNativeTransaction(
        toAddress: String,
        amountValue: BigDecimal,
        gasLimit: BigInteger,
        gasPrice: BigInteger
    ) {
        // Request password for private key
        val password = Messages.showPasswordDialog(
            "Enter wallet password to sign transaction:",
            "Sign Transaction"
        )

        if (password.isNullOrEmpty()) {
            return
        }

        // Get private key
        val privateKey: String
        try {
            privateKey = walletManager.exportPrivateKey(wallet.address, password)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Invalid password or wallet error: ${e.message}", "Error")
            return
        }

        // Confirm transaction
        val gasPriceGwei = Convert.fromWei(BigDecimal(gasPrice), Convert.Unit.GWEI)
        val confirmMessage = """
            Send $amountValue ${network.symbol} to:
            $toAddress

            Gas Limit: $gasLimit
            Gas Price: $gasPriceGwei Gwei

            Continue?
        """.trimIndent()

        val confirmed = Messages.showYesNoDialog(
            project,
            confirmMessage,
            "Confirm Transaction",
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) {
            return
        }

        // Disable dialog while sending
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        scope.launch {
            val result = blockchainService.sendNativeCoin(
                fromAddress = wallet.address,
                toAddress = toAddress,
                amount = amountValue,
                privateKey = privateKey,
                network = network,
                gasLimit = gasLimit,
                gasPrice = gasPrice
            )

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (result) {
                    is TransactionResult.Success -> {
                        val message = buildString {
                            append("Transaction sent successfully!\n\n")
                            append("Transaction Hash:\n${result.transactionHash}\n\n")
                            result.explorerUrl?.let {
                                append("View on Explorer:\n$it")
                            }
                        }
                        Messages.showInfoMessage(project, message, "Transaction Sent")

                        // Copy tx hash to clipboard
                        ClipboardUtil.copyToClipboard(result.transactionHash)

                        super.doOKAction()
                    }
                    is TransactionResult.Error -> {
                        Messages.showErrorDialog(project, "Transaction failed: ${result.message}", "Error")
                    }
                }
            }
        }
    }

    private fun proceedWithTokenTransaction(
        token: Token,
        toAddress: String,
        amountValue: BigDecimal,
        gasLimit: BigInteger,
        gasPrice: BigInteger
    ) {
        // Request password for private key
        val password = Messages.showPasswordDialog(
            "Enter wallet password to sign transaction:",
            "Sign Transaction"
        )

        if (password.isNullOrEmpty()) {
            return
        }

        // Get private key
        val privateKey: String
        try {
            privateKey = walletManager.exportPrivateKey(wallet.address, password)
        } catch (e: Exception) {
            Messages.showErrorDialog(project, "Invalid password or wallet error: ${e.message}", "Error")
            return
        }

        // Confirm transaction
        val gasPriceGwei = Convert.fromWei(BigDecimal(gasPrice), Convert.Unit.GWEI)
        val confirmMessage = """
            Send $amountValue ${token.symbol} to:
            $toAddress

            Token Contract: ${token.contractAddress}
            Gas Limit: $gasLimit
            Gas Price: $gasPriceGwei Gwei

            Continue?
        """.trimIndent()

        val confirmed = Messages.showYesNoDialog(
            project,
            confirmMessage,
            "Confirm Token Transfer",
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) {
            return
        }

        // Disable dialog while sending
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        scope.launch {
            val result = blockchainService.sendToken(
                contractAddress = token.contractAddress,
                toAddress = toAddress,
                amount = amountValue,
                decimals = token.decimals,
                privateKey = privateKey,
                network = network,
                gasLimit = gasLimit,
                gasPrice = gasPrice
            )

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (result) {
                    is TokenTransferResult.Success -> {
                        val message = buildString {
                            append("Token transfer sent successfully!\n\n")
                            append("Transaction Hash:\n${result.transactionHash}\n\n")
                            result.explorerUrl?.let {
                                append("View on Explorer:\n$it")
                            }
                        }
                        Messages.showInfoMessage(project, message, "Token Transfer Sent")

                        // Copy tx hash to clipboard
                        ClipboardUtil.copyToClipboard(result.transactionHash)

                        super.doOKAction()
                    }
                    is TokenTransferResult.Error -> {
                        Messages.showErrorDialog(project, "Token transfer failed: ${result.message}", "Error")
                    }
                }
            }
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }

    override fun dispose() {
        scope.cancel()
        super.dispose()
    }
}
