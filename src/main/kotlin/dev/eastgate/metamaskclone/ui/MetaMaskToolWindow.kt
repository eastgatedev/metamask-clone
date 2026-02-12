package dev.eastgate.metamaskclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.blockchain.BalanceResult
import dev.eastgate.metamaskclone.core.blockchain.BitcoinAddressesResult
import dev.eastgate.metamaskclone.core.blockchain.BitcoinTransactionsResult
import dev.eastgate.metamaskclone.core.blockchain.BlockchainService
import dev.eastgate.metamaskclone.core.blockchain.EvmTransactionsResult
import dev.eastgate.metamaskclone.core.blockchain.TokenBalanceResult
import dev.eastgate.metamaskclone.core.blockchain.TronTransactionsResult
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.ProjectStorage
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Token
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.ui.dialogs.*
import dev.eastgate.metamaskclone.ui.panels.*
import dev.eastgate.metamaskclone.ui.panels.toActivityItem
import kotlinx.coroutines.*
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class MetaMaskToolWindow(private val project: Project) {
    private val walletManager = WalletManager.getInstance(project)
    private val networkManager = NetworkManager.getInstance(project)
    private val blockchainService = BlockchainService.getInstance(project)
    private val storage = ProjectStorage.getInstance(project)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val mainPanel = SimpleToolWindowPanel(false, true)

    // UI Components
    private val networkSelectorBar = NetworkSelectorBar()
    private val walletSelectorBar = WalletSelectorBar()
    private val balanceDisplayPanel = BalanceDisplayPanel()
    private val actionButtonsRow = ActionButtonsRow()
    private val mainTabPanel = MainTabPanel()

    // Bitcoin UI components
    private val bitcoinAddressesPanel = BitcoinAddressesPanel()
    private val bitcoinActivityPanel = BitcoinActivityPanel()

    // EVM/TRON Activity panel
    private val activityPanel = ActivityPanel()

    // State
    private var tokens: MutableList<Token> = mutableListOf()
    private var isBitcoinMode = false
    private lateinit var headerPanel: JPanel

    init {
        loadTokens()
        setupUI()
        observeChanges()
        setupEventListeners()
        // Refresh token balances on startup
        refreshTokenBalances()
    }

    private fun loadTokens() {
        tokens = storage.getTokens().toMutableList()
    }

    private fun setupUI() {
        mainPanel.layout = BorderLayout()
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        // Header section (Title + Network + Wallet)
        headerPanel = JPanel()
        headerPanel.layout = BoxLayout(headerPanel, BoxLayout.Y_AXIS)

        // Title
        val titleLabel = JBLabel("MetaMask Clone")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD, 16f)
        titleLabel.alignmentX = JPanel.CENTER_ALIGNMENT
        titleLabel.border = JBUI.Borders.empty(0, 0, 10, 0)

        val titlePanel = JPanel(BorderLayout())
        titlePanel.add(titleLabel, BorderLayout.CENTER)

        headerPanel.add(titlePanel)
        headerPanel.add(networkSelectorBar)
        headerPanel.add(walletSelectorBar)

        // Balance and actions section
        val middlePanel = JPanel(BorderLayout())
        middlePanel.add(balanceDisplayPanel, BorderLayout.NORTH)
        middlePanel.add(actionButtonsRow, BorderLayout.CENTER)

        // Content panel combining middle and tabs
        val contentPanel = JPanel(BorderLayout())
        contentPanel.add(middlePanel, BorderLayout.NORTH)
        contentPanel.add(mainTabPanel, BorderLayout.CENTER)

        // Main layout
        mainPanel.add(headerPanel, BorderLayout.NORTH)
        mainPanel.add(contentPanel, BorderLayout.CENTER)

        // Initial state
        updateNetworkUI()
        updateWalletUI()
        updateTokensUI()

        // Wire up activity panel for EVM/TRON on startup
        val initialNetwork = networkManager.selectedNetwork.value
        if (initialNetwork.blockchainType != BlockchainType.BITCOIN) {
            mainTabPanel.replaceActivityPanel(activityPanel)
            activityPanel.onRefreshClick = { loadActivityTransactions() }
            activityPanel.onTransactionClick = { item -> showTransactionDetails(item) }
            loadActivityTransactions()
        }
    }

    private fun observeChanges() {
        // Observe network changes
        scope.launch {
            networkManager.selectedNetwork.collect { network ->
                SwingUtilities.invokeLater {
                    networkSelectorBar.updateNetwork(network)

                    // Switch UI mode based on blockchain type
                    if (network.blockchainType == BlockchainType.BITCOIN) {
                        showBitcoinUI()
                    } else {
                        showStandardUI()
                    }

                    // Select appropriate wallet for this blockchain type
                    selectWalletForCurrentBlockchainType()
                    updateBalanceDisplay()
                    updateTokensUI()
                }
                // Refresh token balances when network changes
                refreshTokenBalances()
                // Refresh activity for EVM/TRON
                if (network.blockchainType != BlockchainType.BITCOIN) {
                    SwingUtilities.invokeLater { loadActivityTransactions() }
                }
            }
        }

        // Observe wallet list changes
        scope.launch {
            walletManager.wallets.collect { wallets ->
                SwingUtilities.invokeLater {
                    val blockchainType = networkManager.getCurrentBlockchainType()
                    val walletsOfType = walletManager.getWalletsForBlockchainType(blockchainType)

                    // If no wallet selected for this type, select the first one
                    val currentWallet = walletManager.selectedWallet.value
                    if (currentWallet == null || currentWallet.blockchainType != blockchainType) {
                        if (walletsOfType.isNotEmpty()) {
                            walletManager.selectWallet(walletsOfType.first().address)
                        }
                    }
                }
            }
        }

        // Observe selected wallet changes
        scope.launch {
            walletManager.selectedWallet.collect { wallet ->
                SwingUtilities.invokeLater {
                    walletSelectorBar.updateWallet(wallet)
                    updateBalanceDisplay()
                }
                // Refresh token balances and activity when wallet changes
                if (wallet != null) {
                    refreshTokenBalances()
                    val network = networkManager.selectedNetwork.value
                    if (network.blockchainType != BlockchainType.BITCOIN) {
                        SwingUtilities.invokeLater { loadActivityTransactions() }
                    }
                }
            }
        }
    }

    private fun setupEventListeners() {
        // Network selector click
        networkSelectorBar.onNetworkClick = {
            showNetworkSelectionDialog()
        }

        // Wallet selector click
        walletSelectorBar.onWalletClick = {
            showWalletSelectionPopup()
        }

        walletSelectorBar.onCopyAddress = { address ->
            Messages.showInfoMessage(project, "Address copied to clipboard", "Copied")
        }

        // Balance refresh
        balanceDisplayPanel.onRefreshClick = {
            updateBalanceDisplay()
        }

        // Action buttons
        actionButtonsRow.onSendClick = {
            showSendDialog()
        }

        actionButtonsRow.onReceiveClick = {
            showReceiveDialog()
        }

        // Token list events
        mainTabPanel.onAddTokenClick = {
            showAddTokenDialog()
        }

        mainTabPanel.onTokenSelected = { token ->
            showTokenDetails(token)
        }

        mainTabPanel.onDeleteToken = { token ->
            handleDeleteToken(token)
        }
    }

    private fun updateNetworkUI() {
        val network = networkManager.selectedNetwork.value
        networkSelectorBar.updateNetwork(network)
    }

    private fun updateWalletUI() {
        val wallet = walletManager.selectedWallet.value
        walletSelectorBar.updateWallet(wallet)
    }

    private fun updateBalanceDisplay() {
        val wallet = walletManager.selectedWallet.value
        val network = networkManager.selectedNetwork.value

        if (wallet == null) {
            balanceDisplayPanel.updateBalance("0", network.symbol, null)
            return
        }

        // Show loading state
        balanceDisplayPanel.showLoading()

        // Fetch balance asynchronously
        scope.launch {
            val result = blockchainService.getBalance(wallet.address, network)
            SwingUtilities.invokeLater {
                when (result) {
                    is BalanceResult.Success -> {
                        balanceDisplayPanel.updateBalance(
                            result.balanceFormatted,
                            result.symbol,
                            null // USD value - future enhancement
                        )
                    }
                    is BalanceResult.Error -> {
                        balanceDisplayPanel.showError(result.message)
                    }
                }
            }
        }
    }

    private fun updateTokensUI() {
        val networkId = networkManager.selectedNetwork.value.id
        val networkTokens = tokens.filter { it.networkId == networkId }
        mainTabPanel.updateTokens(networkTokens)
    }

    /**
     * Select a wallet matching the current blockchain type.
     * If current wallet doesn't match, switch to the first wallet of that type.
     */
    private fun selectWalletForCurrentBlockchainType() {
        val blockchainType = networkManager.getCurrentBlockchainType()
        val currentWallet = walletManager.selectedWallet.value

        // If current wallet doesn't match blockchain type, switch
        if (currentWallet == null || currentWallet.blockchainType != blockchainType) {
            walletManager.selectFirstWalletForType(blockchainType)
        }
    }

    /**
     * Refresh token balances from blockchain.
     * Called on startup, network/wallet changes, and after transactions.
     */
    private fun refreshTokenBalances() {
        val wallet = walletManager.selectedWallet.value ?: return
        val network = networkManager.selectedNetwork.value
        val networkTokens = tokens.filter { it.networkId == network.id }

        if (networkTokens.isEmpty()) return

        scope.launch {
            var hasUpdates = false
            for (token in networkTokens) {
                val result = blockchainService.getTokenBalance(
                    contractAddress = token.contractAddress,
                    walletAddress = wallet.address,
                    decimals = token.decimals,
                    network = network
                )

                when (result) {
                    is TokenBalanceResult.Success -> {
                        val index = tokens.indexOfFirst { it.id == token.id }
                        if (index >= 0 && tokens[index].balance != result.balanceFormatted) {
                            tokens[index] = tokens[index].copy(balance = result.balanceFormatted)
                            hasUpdates = true
                        }
                    }
                    is TokenBalanceResult.Error -> {
                        // Log error but continue with other tokens
                        println("Failed to fetch balance for ${token.symbol}: ${result.message}")
                    }
                }
            }

            if (hasUpdates) {
                SwingUtilities.invokeLater {
                    storage.saveTokens(tokens)
                    updateTokensUI()
                }
            }
        }
    }

    // ==================== Bitcoin UI Switching ====================

    private fun showBitcoinUI() {
        if (isBitcoinMode) return
        isBitcoinMode = true

        // Ensure Bitcoin pseudo-wallet exists and select it
        val bitcoinWallet = walletManager.ensureBitcoinCoreWallet()
        walletManager.selectWallet(bitcoinWallet.address)

        // Replace wallet selector with Bitcoin addresses panel in header
        headerPanel.remove(walletSelectorBar)
        headerPanel.add(bitcoinAddressesPanel)

        // Replace Activity tab with Bitcoin activity panel, hide Tokens tab
        mainTabPanel.replacePlaceholderWithBitcoin(bitcoinActivityPanel)
        mainTabPanel.hideTokenAddButton()
        mainTabPanel.showBitcoinTabs()

        // Wire up Bitcoin event handlers
        bitcoinAddressesPanel.onGenerateNewAddress = { generateBitcoinAddress() }
        bitcoinAddressesPanel.onAddressCopied = { address ->
            Messages.showInfoMessage(project, "Address copied to clipboard", "Copied")
        }
        bitcoinActivityPanel.onRefreshClick = { loadBitcoinTransactions() }
        bitcoinActivityPanel.onTransactionClick = { tx ->
            val message = buildString {
                append("Transaction ID: ${tx.txid}\n")
                append("Category: ${tx.category}\n")
                append("Address: ${tx.address}\n")
                append("Amount: ${String.format("%.8f", tx.amount)} BTC\n")
                append("Confirmations: ${tx.confirmations}\n")
                tx.fee?.let { append("Fee: ${String.format("%.8f", it)} BTC\n") }
                tx.blockhash?.let { append("Block: $it\n") }
            }
            Messages.showInfoMessage(project, message, "Transaction Details")
        }

        headerPanel.revalidate()
        headerPanel.repaint()

        // Auto-prompt RPC config if not yet configured
        val network = networkManager.selectedNetwork.value
        if (!networkManager.isBitcoinNetworkConfigured(network.id)) {
            val configDialog = BitcoinRpcConfigDialog(project, networkManager, network)
            if (configDialog.showAndGet()) {
                // User configured successfully — invalidate cached client and load data
                blockchainService.invalidateNetwork(network.id)
                loadBitcoinAddresses()
                loadBitcoinTransactions()
                updateBalanceDisplay()
                return
            }
        }

        // Load Bitcoin data (either already configured, or user cancelled config)
        loadBitcoinAddresses()
        loadBitcoinTransactions()
    }

    private fun showStandardUI() {
        if (!isBitcoinMode) return
        isBitcoinMode = false

        // Replace Bitcoin addresses panel with wallet selector
        headerPanel.remove(bitcoinAddressesPanel)
        headerPanel.add(walletSelectorBar)

        // Replace Activity tab with EVM/TRON activity panel and show Tokens tab
        mainTabPanel.replaceActivityPanel(activityPanel)
        mainTabPanel.showTokenAddButton()
        mainTabPanel.showStandardTabs()

        // Wire up event handlers
        activityPanel.onRefreshClick = { loadActivityTransactions() }
        activityPanel.onTransactionClick = { item -> showTransactionDetails(item) }

        headerPanel.revalidate()
        headerPanel.repaint()

        // Load transactions for the current network/wallet
        loadActivityTransactions()
    }

    private fun loadBitcoinAddresses() {
        val network = networkManager.selectedNetwork.value
        bitcoinAddressesPanel.showLoading()

        scope.launch {
            val result = blockchainService.getBitcoinAddresses(network)
            SwingUtilities.invokeLater {
                when (result) {
                    is BitcoinAddressesResult.Success -> bitcoinAddressesPanel.updateAddresses(result.addresses)
                    is BitcoinAddressesResult.Error -> bitcoinAddressesPanel.showError(result.message)
                }
            }
        }
    }

    private fun loadBitcoinTransactions() {
        val network = networkManager.selectedNetwork.value
        bitcoinActivityPanel.showLoading()

        scope.launch {
            val result = blockchainService.getBitcoinTransactions(network)
            SwingUtilities.invokeLater {
                when (result) {
                    is BitcoinTransactionsResult.Success -> bitcoinActivityPanel.updateTransactions(result.transactions)
                    is BitcoinTransactionsResult.Error -> bitcoinActivityPanel.showError(result.message)
                }
            }
        }
    }

    // ==================== EVM/TRON Activity ====================

    private fun loadActivityTransactions() {
        val wallet = walletManager.selectedWallet.value ?: return
        val network = networkManager.selectedNetwork.value

        if (network.blockchainType == BlockchainType.BITCOIN) return

        activityPanel.showLoading()

        scope.launch {
            when (network.blockchainType) {
                BlockchainType.EVM -> {
                    val networkTokens = tokens.filter { it.networkId == network.id }
                    val result = blockchainService.getEvmTransactions(network, wallet.address, networkTokens)
                    SwingUtilities.invokeLater {
                        when (result) {
                            is EvmTransactionsResult.Success -> {
                                val items = result.transactions.map { it.toActivityItem(wallet.address, network) }
                                activityPanel.updateTransactions(items)
                            }
                            is EvmTransactionsResult.Error -> activityPanel.showError(result.message)
                        }
                    }
                }
                BlockchainType.TRON -> {
                    val result = blockchainService.getTronTransactions(network, wallet.address)
                    SwingUtilities.invokeLater {
                        when (result) {
                            is TronTransactionsResult.Success -> {
                                val items = result.transactions.map { it.toActivityItem(wallet.address, network) }
                                activityPanel.updateTransactions(items)
                            }
                            is TronTransactionsResult.Error -> activityPanel.showError(result.message)
                        }
                    }
                }
                BlockchainType.BITCOIN -> { /* handled by BitcoinActivityPanel */ }
            }
        }
    }

    private fun showTransactionDetails(item: ActivityItem) {
        val message = buildString {
            append("Transaction: ${item.txHash}\n")
            append("Direction: ${item.direction}\n")
            append("Address: ${item.counterpartyAddress}\n")
            append("Amount: ${if (item.isSend) "-" else "+"}${item.amount} ${item.symbol}\n")
            item.explorerUrl?.let { append("\nExplorer: $it\n") }
        }
        Messages.showInfoMessage(project, message, "Transaction Details")
    }

    private fun generateBitcoinAddress() {
        val network = networkManager.selectedNetwork.value

        scope.launch {
            val result = blockchainService.generateBitcoinAddress(network)
            SwingUtilities.invokeLater {
                when (result) {
                    is dev.eastgate.metamaskclone.core.blockchain.BitcoinAddressResult.Success -> {
                        Messages.showInfoMessage(project, "New address: ${result.address}", "Address Generated")
                        loadBitcoinAddresses()
                    }
                    is dev.eastgate.metamaskclone.core.blockchain.BitcoinAddressResult.Error -> {
                        Messages.showErrorDialog(project, "Failed: ${result.message}", "Error")
                    }
                }
            }
        }
    }

    private fun showNetworkSelectionDialog() {
        val dialog = NetworkSelectionDialog(project, networkManager)
        dialog.show()
    }

    private fun showWalletSelectionPopup() {
        val blockchainType = networkManager.getCurrentBlockchainType()

        // Bitcoin wallets are managed by Bitcoin Core — show info instead
        if (blockchainType == BlockchainType.BITCOIN) {
            Messages.showInfoMessage(
                project,
                "Bitcoin wallets are managed by Bitcoin Core.\nUse bitcoin-cli to manage wallets and keys.",
                "Bitcoin Core Wallet"
            )
            return
        }

        val wallets = walletManager.getWalletsForBlockchainType(blockchainType)

        if (wallets.isEmpty()) {
            showCreateOrImportDialog(blockchainType)
            return
        }

        val popup = JPopupMenu()

        // Wallet list (filtered by blockchain type)
        for (wallet in wallets) {
            val isSelected = walletManager.selectedWallet.value?.address == wallet.address
            val menuItem = JMenuItem(if (isSelected) "✓ ${wallet.name}" else "   ${wallet.name}")
            menuItem.toolTipText = wallet.address
            menuItem.addActionListener {
                walletManager.selectWallet(wallet.address)
            }
            popup.add(menuItem)
        }

        popup.addSeparator()

        // Create new wallet (with blockchain type label)
        val createLabel = when (blockchainType) {
            BlockchainType.EVM -> "+ Create New Wallet"
            BlockchainType.TRON -> "+ Create New TRON Wallet"
            BlockchainType.BITCOIN -> "" // unreachable
        }
        val createItem = JMenuItem(createLabel)
        createItem.addActionListener {
            val dialog = CreateWalletDialog(project, walletManager, blockchainType)
            dialog.show()
        }
        popup.add(createItem)

        // Import wallet (with blockchain type label)
        val importLabel = when (blockchainType) {
            BlockchainType.EVM -> "+ Import Wallet"
            BlockchainType.TRON -> "+ Import TRON Wallet"
            BlockchainType.BITCOIN -> "" // unreachable
        }
        val importItem = JMenuItem(importLabel)
        importItem.addActionListener {
            val dialog = ImportWalletDialog(project, walletManager, blockchainType)
            dialog.show()
        }
        popup.add(importItem)

        popup.addSeparator()

        // Wallet management
        val selectedWallet = walletManager.selectedWallet.value
        if (selectedWallet != null && selectedWallet.blockchainType == blockchainType) {
            val exportItem = JMenuItem("Export Private Key")
            exportItem.addActionListener {
                exportPrivateKey(selectedWallet)
            }
            popup.add(exportItem)

            val deleteItem = JMenuItem("Delete Wallet")
            deleteItem.addActionListener {
                deleteWallet(selectedWallet)
            }
            popup.add(deleteItem)
        }

        popup.show(walletSelectorBar, 0, walletSelectorBar.height)
    }

    private fun showCreateOrImportDialog(blockchainType: BlockchainType = BlockchainType.EVM) {
        if (blockchainType == BlockchainType.BITCOIN) {
            Messages.showInfoMessage(
                project,
                "Bitcoin wallets are managed by Bitcoin Core.\nConnect to a Bitcoin Core node to use Bitcoin functionality.",
                "Bitcoin Core Wallet"
            )
            return
        }

        val chainName = when (blockchainType) {
            BlockchainType.EVM -> "EVM"
            BlockchainType.TRON -> "TRON"
            BlockchainType.BITCOIN -> return // unreachable
        }

        val options = arrayOf("Create New", "Import Existing", "Cancel")
        val result = Messages.showDialog(
            project,
            "No $chainName wallets found. Would you like to create a new wallet or import an existing one?",
            "No Wallet",
            options,
            0,
            Messages.getQuestionIcon()
        )

        when (result) {
            0 -> {
                val dialog = CreateWalletDialog(project, walletManager, blockchainType)
                dialog.show()
            }
            1 -> {
                val dialog = ImportWalletDialog(project, walletManager, blockchainType)
                dialog.show()
            }
        }
    }

    private fun showSendDialog() {
        val wallet = walletManager.selectedWallet.value
        if (wallet == null) {
            Messages.showWarningDialog(project, "Please select a wallet first", "No Wallet Selected")
            return
        }

        val network = networkManager.selectedNetwork.value
        val networkTokens = tokens.filter { it.networkId == network.id }

        val dialog = SendTokenDialog(
            project = project,
            wallet = wallet,
            network = network,
            walletManager = walletManager,
            blockchainService = blockchainService,
            token = null,
            availableTokens = networkTokens
        )

        if (dialog.showAndGet()) {
            // Refresh balance after successful send
            updateBalanceDisplay()
            refreshTokenBalances()
        }
    }

    private fun showReceiveDialog() {
        // In Bitcoin mode, show QR for the first receiving address
        if (isBitcoinMode) {
            val firstAddress = bitcoinAddressesPanel.getFirstAddress()
            if (firstAddress != null) {
                bitcoinAddressesPanel.showAddressQRDialog(firstAddress)
            } else {
                Messages.showWarningDialog(project, "No Bitcoin addresses available. Generate one first.", "No Address")
            }
            return
        }

        val wallet = walletManager.selectedWallet.value
        if (wallet == null) {
            Messages.showWarningDialog(project, "Please select a wallet first", "No Wallet Selected")
            return
        }

        val dialog = ReceiveDialog(project, wallet)
        dialog.show()
    }

    private fun showAddTokenDialog() {
        val network = networkManager.selectedNetwork.value
        val dialog = AddTokenDialog(
            project = project,
            networkId = network.id,
            network = network,
            blockchainService = blockchainService
        )

        if (dialog.showAndGet()) {
            dialog.resultToken?.let { token ->
                // Check for duplicates
                val existingIndex = tokens.indexOfFirst {
                    it.contractAddress.equals(token.contractAddress, ignoreCase = true) &&
                        it.networkId == token.networkId
                }

                if (existingIndex >= 0) {
                    tokens[existingIndex] = token
                } else {
                    tokens.add(token)
                }

                storage.saveTokens(tokens)
                updateTokensUI()
                // Refresh balance for the newly added token
                refreshTokenBalances()
            }
        }
    }

    private fun showTokenDetails(token: Token) {
        val options = arrayOf("OK", "Delete Token")
        val result = Messages.showDialog(
            project,
            """
            Token: ${token.symbol}
            Name: ${token.name}
            Contract: ${token.contractAddress}
            Decimals: ${token.decimals}
            Balance: ${token.getFormattedBalance()} ${token.symbol}
            """.trimIndent(),
            "Token Details",
            options,
            0,
            Messages.getInformationIcon()
        )

        if (result == 1) { // Delete Token clicked
            handleDeleteToken(token)
        }
    }

    private fun handleDeleteToken(token: Token) {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete ${token.symbol}?\n\nThis will remove the token from your list. This action cannot be undone.",
            "Delete Token",
            "Delete",
            "Cancel",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            tokens.removeIf { it.id == token.id }
            storage.saveTokens(tokens)
            updateTokensUI()
        }
    }

    private fun exportPrivateKey(wallet: Wallet) {
        val password = Messages.showPasswordDialog(
            "Enter password to export private key:",
            "Export Private Key"
        )

        if (password != null) {
            try {
                val privateKey = walletManager.exportPrivateKey(wallet.address, password)
                val result = Messages.showYesNoDialog(
                    project,
                    "Private Key:\n$privateKey\n\nWARNING: Never share your private key!\n\nCopy to clipboard?",
                    "Private Key",
                    Messages.getWarningIcon()
                )
                if (result == Messages.YES) {
                    dev.eastgate.metamaskclone.utils.ClipboardUtil.copyToClipboard(privateKey)
                    Messages.showInfoMessage(project, "Private key copied to clipboard", "Copied")
                }
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Failed to export: ${e.message}", "Export Failed")
            }
        }
    }

    private fun deleteWallet(wallet: Wallet) {
        val result = Messages.showYesNoDialog(
            project,
            "Are you sure you want to delete wallet '${wallet.name}'?\n\nThis action cannot be undone!",
            "Delete Wallet",
            Messages.getWarningIcon()
        )

        if (result == Messages.YES) {
            walletManager.deleteWallet(wallet.address)
        }
    }

    fun getContent(): JComponent {
        return mainPanel
    }

    fun dispose() {
        scope.cancel()
        blockchainService.shutdown()
    }
}
