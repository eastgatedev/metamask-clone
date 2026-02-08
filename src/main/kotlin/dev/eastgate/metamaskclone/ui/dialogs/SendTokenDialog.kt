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
import dev.eastgate.metamaskclone.models.BlockchainType
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

    // Gas UI components that may be hidden for TRON
    private lateinit var gasSettingsSeparator: JSeparator
    private lateinit var gasSettingsLabel: JLabel
    private lateinit var gasLimitLabel: JLabel
    private lateinit var gasPriceLabel: JLabel

    // TRC20 fee limit UI components
    private val feeLimitField = JBTextField("50")
    private lateinit var feeLimitLabel: JLabel
    private lateinit var tokenSelectorLabel: JLabel

    // Check blockchain type
    private val isTronNetwork = network.blockchainType == BlockchainType.TRON
    private val isBitcoinNetwork = network.blockchainType == BlockchainType.BITCOIN

    init {
        title = "Send ${network.symbol}"
        isOKActionEnabled = false // Disable until data is loaded
        init()
        setupTokenSelector()
        setupForBlockchainType()
    }

    private fun setupTokenSelector() {
        // Add native token option
        tokenSelector.addItem("${network.symbol} (Native Token)")

        // Add available tokens (ERC20 for EVM, TRC20 for TRON)
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

    /**
     * Setup UI based on blockchain type.
     * TRON networks hide gas fields and show bandwidth info instead.
     */
    private fun setupForBlockchainType() {
        if (isBitcoinNetwork) {
            // Hide gas, token selector, and fee limit for Bitcoin
            gasSettingsSeparator.isVisible = false
            gasSettingsLabel.isVisible = false
            gasLimitLabel.isVisible = false
            gasLimitField.isVisible = false
            gasPriceLabel.isVisible = false
            gasPriceField.isVisible = false
            tokenSelectorLabel.isVisible = false
            tokenSelector.isVisible = false
            feeLimitLabel.isVisible = false
            feeLimitField.isVisible = false

            // Update fee display for Bitcoin
            estimatedFeeLabel.text = "Calculated by Bitcoin Core"
            estimatedFeeLabel.foreground = java.awt.Color(0x059669)

            // Mark gas as loaded since Bitcoin Core handles fees
            gasPriceLoaded = true

            // Fetch balance
            fetchBalance()
        } else if (isTronNetwork) {
            // Hide gas-related UI for TRON
            gasSettingsSeparator.isVisible = false
            gasSettingsLabel.isVisible = false
            gasLimitLabel.isVisible = false
            gasLimitField.isVisible = false
            gasPriceLabel.isVisible = false
            gasPriceField.isVisible = false

            // Update fee display for TRON (uses bandwidth, typically free)
            estimatedFeeLabel.text = "Free (uses bandwidth)"
            estimatedFeeLabel.foreground = java.awt.Color(0x059669) // Green

            // Mark gas as loaded since we don't need it for TRON
            gasPriceLoaded = true

            // Fetch balance only
            fetchBalance()
        } else {
            // EVM networks: fetch gas price and balance
            fetchGasPrice()
            fetchBalance()
        }
    }

    private fun onTokenSelectionChanged() {
        val isNativeToken = tokenSelector.selectedIndex == 0
        val isTrc20Token = isTronNetwork && !isNativeToken

        // Show/hide fee limit field for TRC20
        feeLimitLabel.isVisible = isTrc20Token
        feeLimitField.isVisible = isTrc20Token

        if (isNativeToken) {
            // Native token selected
            if (!isTronNetwork) {
                gasLimitField.text = "21000"
            }
            fetchBalance()
        } else {
            // Token selected (ERC20 or TRC20)
            if (!isTronNetwork) {
                gasLimitField.text = "100000"
            }
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
            val isNativeToken = tokenSelector.selectedIndex == 0
            val isTrc20Token = isTronNetwork && !isNativeToken

            if (isTronNetwork) {
                if (isTrc20Token) {
                    // TRC20: Show fee limit info
                    val feeLimit = feeLimitField.text.trim().toBigDecimalOrNull() ?: BigDecimal("50")
                    estimatedFeeLabel.text = "Up to $feeLimit TRX (energy)"
                    estimatedFeeLabel.foreground = java.awt.Color(0xF59E0B) // Amber
                    // Get token symbol for display
                    val tokenIndex = tokenSelector.selectedIndex - 1
                    val tokenSymbol = if (tokenIndex >= 0 && tokenIndex < availableTokens.size) {
                        availableTokens[tokenIndex].symbol
                    } else {
                        "tokens"
                    }
                    val totalFormatted = amount.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                    totalLabel.text = "$totalFormatted $tokenSymbol"
                } else {
                    // TRX native: Free bandwidth
                    estimatedFeeLabel.text = "Free (uses bandwidth)"
                    estimatedFeeLabel.foreground = java.awt.Color(0x059669) // Green
                    val totalFormatted = amount.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
                    totalLabel.text = "$totalFormatted ${network.symbol}"
                }
                totalLabel.font = totalLabel.font.deriveFont(java.awt.Font.BOLD)
            } else {
                // EVM: Calculate gas fees
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
            }
        } catch (e: Exception) {
            if (!isTronNetwork) {
                estimatedFeeLabel.text = "--"
            }
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
        val fromText = if (wallet.blockchainType == BlockchainType.BITCOIN) {
            wallet.name
        } else {
            "${wallet.name} (${wallet.getShortAddress()})"
        }
        val fromField = JBTextField(fromText)
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
        toAddressField.toolTipText = when {
            isBitcoinNetwork -> "Recipient Bitcoin address"
            isTronNetwork -> "Recipient TRON address (T...)"
            else -> "Recipient address (0x...)"
        }
        panel.add(toAddressField, gbc)

        // Token selector
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        tokenSelectorLabel = JLabel("Token:")
        panel.add(tokenSelectorLabel, gbc)

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

        // Gas settings section (hidden for TRON)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        gasSettingsSeparator = JSeparator()
        panel.add(gasSettingsSeparator, gbc)

        row++
        gbc.gridy = row
        gasSettingsLabel = JLabel("Gas Settings")
        gasSettingsLabel.font = gasSettingsLabel.font.deriveFont(java.awt.Font.BOLD)
        panel.add(gasSettingsLabel, gbc)

        // Gas Limit
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        gasLimitLabel = JLabel("Gas Limit:")
        panel.add(gasLimitLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gasLimitField.toolTipText = "Gas limit (21000 for simple transfers)"
        panel.add(gasLimitField, gbc)

        // Gas Price
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        gasPriceLabel = JLabel("Gas Price (Gwei):")
        panel.add(gasPriceLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gasPriceField.toolTipText = "Gas price in Gwei"
        panel.add(gasPriceField, gbc)

        // Fee Limit (TRC20 only)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        feeLimitLabel = JLabel("Fee Limit (TRX):")
        panel.add(feeLimitLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        feeLimitField.toolTipText = "Maximum TRX to burn for energy (default: 50 TRX)"
        panel.add(feeLimitField, gbc)

        // Initially hide fee limit (shown only for TRC20 tokens)
        feeLimitLabel.isVisible = false
        feeLimitField.isVisible = false

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
        feeLimitField.document.addDocumentListener(updateListener)

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

        // Validate address format based on blockchain type
        if (isBitcoinNetwork) {
            if (toAddress.length < 26 || toAddress.length > 62) {
                Messages.showErrorDialog("Invalid Bitcoin address format (expected 26-62 characters)", "Validation Error")
                return
            }
        } else if (isTronNetwork) {
            if (!toAddress.startsWith("T") || toAddress.length != 34) {
                Messages.showErrorDialog("Invalid TRON address format. Must be a 34-character address starting with T", "Validation Error")
                return
            }
        } else {
            if (!toAddress.startsWith("0x") || toAddress.length != 42) {
                Messages.showErrorDialog("Invalid address format. Must be a 42-character hex address starting with 0x", "Validation Error")
                return
            }
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

        // Gas validation only for EVM networks
        val gasLimitValue: Long
        val gasPriceValue: BigDecimal
        val gasPriceWei: BigInteger
        val gasLimit: BigInteger
        val gasFeeWei: BigInteger

        if (isBitcoinNetwork || isTronNetwork) {
            // Bitcoin and TRON don't use EVM gas - use zero values
            gasLimitValue = 0L
            gasPriceValue = BigDecimal.ZERO
            gasPriceWei = BigInteger.ZERO
            gasLimit = BigInteger.ZERO
            gasFeeWei = BigInteger.ZERO
        } else {
            // Validate gas settings for EVM
            gasLimitValue = gasLimitField.text.toLongOrNull() ?: 0L
            if (gasLimitValue <= 0) {
                Messages.showErrorDialog("Please enter a valid gas limit", "Validation Error")
                return
            }

            gasPriceValue = gasPriceField.text.toBigDecimalOrNull() ?: BigDecimal.ZERO
            if (gasPriceValue <= BigDecimal.ZERO) {
                Messages.showErrorDialog("Please enter a valid gas price", "Validation Error")
                return
            }

            // Calculate gas fees
            gasPriceWei = Convert.toWei(gasPriceValue, Convert.Unit.GWEI).toBigInteger()
            gasLimit = BigInteger.valueOf(gasLimitValue)
            gasFeeWei = gasPriceWei.multiply(gasLimit)
        }

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
                            if (isBitcoinNetwork) {
                                // Bitcoin: compare in satoshis (1 BTC = 100,000,000 satoshis)
                                val amountSatoshis = amountValue.multiply(BigDecimal(100_000_000L)).toBigInteger()

                                if (nativeBalanceResult.balanceWei < amountSatoshis) {
                                    Messages.showErrorDialog(
                                        project,
                                        """
                                        Insufficient BTC balance.

                                        Your balance: ${nativeBalanceResult.balanceFormatted} ${network.symbol}
                                        Amount to send: $amountValue ${network.symbol}
                                        """.trimIndent(),
                                        "Insufficient Balance"
                                    )
                                    return@invokeLater
                                }

                                proceedWithBitcoinTransaction(toAddress, amountValue)
                            } else if (isTronNetwork) {
                                // TRON: Convert amount to SUN for comparison (1 TRX = 1,000,000 SUN)
                                val amountSun = amountValue.multiply(BigDecimal(1_000_000L)).toBigInteger()

                                if (nativeBalanceResult.balanceWei < amountSun) {
                                    Messages.showErrorDialog(
                                        project,
                                        """
                                        Insufficient TRX balance.

                                        Your balance: ${nativeBalanceResult.balanceFormatted} ${network.symbol}
                                        Amount to send: $amountValue ${network.symbol}
                                        """.trimIndent(),
                                        "Insufficient Balance"
                                    )
                                    return@invokeLater
                                }

                                // Balance is sufficient, proceed with TRON transaction
                                proceedWithTronTransaction(toAddress, amountValue)
                            } else {
                                // EVM: check amount + gas fees
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
                            }
                        } else {
                            // Token transfer (ERC20 or TRC20)
                            if (isTronNetwork) {
                                // TRC20 token transfer
                                // Check TRX balance for fees (recommend at least 1 TRX)
                                val trxBalance = nativeBalanceResult.balanceWei // in SUN
                                val minTrxRequired = BigInteger.valueOf(1_000_000L) // 1 TRX in SUN
                                if (trxBalance < minTrxRequired) {
                                    Messages.showWarningDialog(
                                        project,
                                        "Low TRX balance. TRC20 transfers require TRX for energy fees.\n\nCurrent TRX: ${nativeBalanceResult.balanceFormatted} TRX",
                                        "Low TRX Balance"
                                    )
                                }

                                // Check token balance
                                val tokenAmountRaw = amountValue.multiply(BigDecimal(BigInteger.TEN.pow(selectedToken!!.decimals))).toBigInteger()
                                if (currentBalanceWei < tokenAmountRaw) {
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

                                // Proceed with TRC20 transfer
                                proceedWithTrc20TokenTransaction(selectedToken, toAddress, amountValue)
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

                                // Balance is sufficient, proceed with ERC20 token transfer
                                proceedWithTokenTransaction(selectedToken, toAddress, amountValue, gasLimit, gasPriceWei)
                            }
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

    /**
     * Handle TRON native coin (TRX) transfer.
     * TRON uses bandwidth instead of gas, so no gas parameters needed.
     */
    private fun proceedWithTronTransaction(
        toAddress: String,
        amountValue: BigDecimal
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

        // Confirm transaction (no gas info for TRON)
        val confirmMessage = """
            Send $amountValue ${network.symbol} to:
            $toAddress

            Network Fee: Free (uses bandwidth)

            Continue?
        """.trimIndent()

        val confirmed = Messages.showYesNoDialog(
            project,
            confirmMessage,
            "Confirm TRX Transfer",
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) {
            return
        }

        // Disable dialog while sending
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        scope.launch {
            val result = blockchainService.sendTronNativeCoin(
                fromAddress = wallet.address,
                toAddress = toAddress,
                amount = amountValue,
                privateKey = privateKey,
                network = network
            )

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (result) {
                    is TransactionResult.Success -> {
                        val message = buildString {
                            append("TRX transfer sent successfully!\n\n")
                            append("Transaction ID:\n${result.transactionHash}\n\n")
                            result.explorerUrl?.let {
                                append("View on TronScan:\n$it")
                            }
                        }
                        Messages.showInfoMessage(project, message, "TRX Transfer Sent")

                        // Copy tx hash to clipboard
                        ClipboardUtil.copyToClipboard(result.transactionHash)

                        super.doOKAction()
                    }
                    is TransactionResult.Error -> {
                        Messages.showErrorDialog(project, "TRX transfer failed: ${result.message}", "Error")
                    }
                }
            }
        }
    }

    /**
     * Handle Bitcoin transaction.
     * Bitcoin Core signs the transaction server-side — no password needed.
     */
    private fun proceedWithBitcoinTransaction(
        toAddress: String,
        amountValue: BigDecimal
    ) {
        val confirmMessage = """
            Send $amountValue ${network.symbol} to:
            $toAddress

            Fee: Calculated by Bitcoin Core

            Continue?
        """.trimIndent()

        val confirmed = Messages.showYesNoDialog(
            project,
            confirmMessage,
            "Confirm Bitcoin Transfer",
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) {
            return
        }

        // Disable dialog while sending
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        scope.launch {
            val result = blockchainService.sendBitcoin(
                toAddress = toAddress,
                amount = amountValue.toDouble(),
                network = network
            )

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (result) {
                    is TransactionResult.Success -> {
                        val message = buildString {
                            append("Bitcoin transfer sent successfully!\n\n")
                            append("Transaction ID:\n${result.transactionHash}\n\n")
                            result.explorerUrl?.let {
                                append("View on Explorer:\n$it")
                            }
                        }
                        Messages.showInfoMessage(project, message, "Bitcoin Transfer Sent")

                        ClipboardUtil.copyToClipboard(result.transactionHash)

                        super.doOKAction()
                    }
                    is TransactionResult.Error -> {
                        Messages.showErrorDialog(project, "Bitcoin transfer failed: ${result.message}", "Error")
                    }
                }
            }
        }
    }

    /**
     * Handle TRC20 token transfer.
     */
    private fun proceedWithTrc20TokenTransaction(
        token: Token,
        toAddress: String,
        amountValue: BigDecimal
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

        // Get fee limit in SUN (1 TRX = 1,000,000 SUN)
        val feeLimitTrx = feeLimitField.text.trim().toBigDecimalOrNull() ?: BigDecimal("50")
        val feeLimitSun = feeLimitTrx.multiply(BigDecimal(1_000_000L)).toLong()

        // Confirm transaction
        val confirmMessage = """
            Send $amountValue ${token.symbol} to:
            $toAddress

            Token Contract: ${token.contractAddress}
            Fee Limit: $feeLimitTrx TRX (max energy cost)

            Continue?
        """.trimIndent()

        val confirmed = Messages.showYesNoDialog(
            project,
            confirmMessage,
            "Confirm TRC20 Transfer",
            Messages.getQuestionIcon()
        )

        if (confirmed != Messages.YES) {
            return
        }

        // Disable dialog while sending
        isOKActionEnabled = false
        rootPane?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)

        scope.launch {
            val result = blockchainService.sendTrc20Token(
                contractAddress = token.contractAddress,
                fromAddress = wallet.address,
                toAddress = toAddress,
                amount = amountValue,
                privateKey = privateKey,
                network = network,
                feeLimit = feeLimitSun
            )

            SwingUtilities.invokeLater {
                rootPane?.cursor = Cursor.getDefaultCursor()
                isOKActionEnabled = true

                when (result) {
                    is TokenTransferResult.Success -> {
                        val message = buildString {
                            append("TRC20 transfer sent successfully!\n\n")
                            append("Transaction ID:\n${result.transactionHash}\n\n")
                            result.explorerUrl?.let {
                                append("View on TronScan:\n$it")
                            }
                        }
                        Messages.showInfoMessage(project, message, "TRC20 Transfer Sent")

                        // Copy tx hash to clipboard
                        ClipboardUtil.copyToClipboard(result.transactionHash)

                        super.doOKAction()
                    }
                    is TokenTransferResult.Error -> {
                        Messages.showErrorDialog(project, "TRC20 transfer failed: ${result.message}", "Error")
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
