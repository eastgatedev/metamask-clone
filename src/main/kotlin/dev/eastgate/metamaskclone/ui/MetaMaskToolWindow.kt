package dev.eastgate.metamaskclone.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import dev.eastgate.metamaskclone.core.blockchain.BalanceResult
import dev.eastgate.metamaskclone.core.blockchain.BlockchainService
import dev.eastgate.metamaskclone.core.blockchain.TokenBalanceResult
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.ProjectStorage
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import dev.eastgate.metamaskclone.models.Token
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.ui.dialogs.*
import dev.eastgate.metamaskclone.ui.panels.*
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

    // State
    private var tokens: MutableList<Token> = mutableListOf()

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
        val headerPanel = JPanel()
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
    }

    private fun observeChanges() {
        // Observe network changes
        scope.launch {
            networkManager.selectedNetwork.collect { network ->
                SwingUtilities.invokeLater {
                    networkSelectorBar.updateNetwork(network)
                    updateBalanceDisplay()
                    updateTokensUI()
                }
                // Refresh token balances when network changes
                refreshTokenBalances()
            }
        }

        // Observe wallet list changes
        scope.launch {
            walletManager.wallets.collect { wallets ->
                SwingUtilities.invokeLater {
                    // If no wallet selected, select the first one
                    if (walletManager.selectedWallet.value == null && wallets.isNotEmpty()) {
                        walletManager.selectWallet(wallets.first().address)
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
                // Refresh token balances when wallet changes
                if (wallet != null) {
                    refreshTokenBalances()
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

    private fun showNetworkSelectionDialog() {
        val dialog = NetworkSelectionDialog(project, networkManager)
        dialog.show()
    }

    private fun showWalletSelectionPopup() {
        val wallets = walletManager.wallets.value
        if (wallets.isEmpty()) {
            showCreateOrImportDialog()
            return
        }

        val popup = JPopupMenu()

        // Wallet list
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

        // Create new wallet
        val createItem = JMenuItem("+ Create New Wallet")
        createItem.addActionListener {
            val dialog = CreateWalletDialog(project, walletManager)
            dialog.show()
        }
        popup.add(createItem)

        // Import wallet
        val importItem = JMenuItem("+ Import Wallet")
        importItem.addActionListener {
            val dialog = ImportWalletDialog(project, walletManager)
            dialog.show()
        }
        popup.add(importItem)

        popup.addSeparator()

        // Wallet management
        val selectedWallet = walletManager.selectedWallet.value
        if (selectedWallet != null) {
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

    private fun showCreateOrImportDialog() {
        val options = arrayOf("Create New", "Import Existing", "Cancel")
        val result = Messages.showDialog(
            project,
            "No wallets found. Would you like to create a new wallet or import an existing one?",
            "No Wallet",
            options,
            0,
            Messages.getQuestionIcon()
        )

        when (result) {
            0 -> {
                val dialog = CreateWalletDialog(project, walletManager)
                dialog.show()
            }
            1 -> {
                val dialog = ImportWalletDialog(project, walletManager)
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
