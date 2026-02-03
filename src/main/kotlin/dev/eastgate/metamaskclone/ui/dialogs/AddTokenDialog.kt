package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.blockchain.BlockchainService
import dev.eastgate.metamaskclone.core.blockchain.TokenMetadataResult
import dev.eastgate.metamaskclone.core.storage.Network
import dev.eastgate.metamaskclone.models.BlockchainType
import dev.eastgate.metamaskclone.models.Token
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingUtilities

class AddTokenDialog(
    private val project: Project,
    private val networkId: String,
    private val network: Network,
    private val blockchainService: BlockchainService,
    private val existingToken: Token? = null
) : DialogWrapper(project) {
    private val contractAddressField = JBTextField()
    private val symbolField = JBTextField()
    private val nameField = JBTextField()
    private val decimalsField = JBTextField("18")
    private val fetchButton = JButton("Fetch")
    private val statusLabel = JLabel()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    var resultToken: Token? = null
        private set

    private val isEditMode = existingToken != null
    private val isTronNetwork = network.blockchainType == BlockchainType.TRON

    init {
        title = if (isEditMode) "Edit Token" else "Add Token"

        if (isEditMode && existingToken != null) {
            contractAddressField.text = existingToken.contractAddress
            symbolField.text = existingToken.symbol
            nameField.text = existingToken.name
            decimalsField.text = existingToken.decimals.toString()
            fetchButton.isEnabled = false
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // Contract Address with Fetch button
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        gbc.gridwidth = 1
        panel.add(JLabel("Contract Address:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        contractAddressField.columns = 30
        contractAddressField.toolTipText = if (isTronNetwork) {
            "TRC20 token contract address (T...)"
        } else {
            "Token contract address (0x...)"
        }
        contractAddressField.isEditable = !isEditMode
        panel.add(contractAddressField, gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        fetchButton.toolTipText = "Fetch token info from blockchain"
        fetchButton.isEnabled = !isEditMode
        fetchButton.addActionListener { fetchTokenMetadata() }
        panel.add(fetchButton, gbc)

        // Token Symbol
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Token Symbol:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        symbolField.toolTipText = "e.g., USDT, LINK, UNI"
        panel.add(symbolField, gbc)

        // Token Name (optional)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Token Name:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        nameField.toolTipText = "Optional, e.g., Tether USD"
        panel.add(nameField, gbc)

        // Decimals
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Decimals:"), gbc)

        gbc.gridx = 1
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        decimalsField.toolTipText = "Token decimals (usually 18)"
        panel.add(decimalsField, gbc)

        // Status label
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 3
        statusLabel.text = " "
        panel.add(statusLabel, gbc)

        return panel
    }

    private fun fetchTokenMetadata() {
        val contractAddress = contractAddressField.text.trim()

        // Validate address format
        if (contractAddress.isEmpty()) {
            statusLabel.foreground = JBColor.RED
            statusLabel.text = "Please enter a contract address"
            return
        }

        // Validate based on network type
        if (isTronNetwork) {
            if (!blockchainService.isValidTronAddress(contractAddress)) {
                statusLabel.foreground = JBColor.RED
                statusLabel.text = "Invalid TRON address format (must start with T, 34 chars)"
                return
            }
        } else {
            if (!contractAddress.startsWith("0x") || contractAddress.length != 42) {
                statusLabel.foreground = JBColor.RED
                statusLabel.text = "Invalid address format"
                return
            }
        }

        // Start fetching
        fetchButton.isEnabled = false
        statusLabel.foreground = JBColor.GRAY
        statusLabel.text = "Fetching token info..."

        scope.launch {
            val result = if (isTronNetwork) {
                blockchainService.getTrc20TokenMetadata(contractAddress, network)
            } else {
                blockchainService.getTokenMetadata(contractAddress, network)
            }

            SwingUtilities.invokeLater {
                when (result) {
                    is TokenMetadataResult.Success -> {
                        symbolField.text = result.symbol
                        nameField.text = result.name
                        decimalsField.text = result.decimals.toString()
                        statusLabel.foreground = JBColor(0x4CAF50, 0x81C784)
                        statusLabel.text = "Token info loaded successfully"
                    }
                    is TokenMetadataResult.Error -> {
                        statusLabel.foreground = JBColor.RED
                        statusLabel.text = "Failed: ${result.message}"
                    }
                }
                fetchButton.isEnabled = true
            }
        }
    }

    override fun doOKAction() {
        val contractAddress = contractAddressField.text.trim()
        val symbol = symbolField.text.trim()
        val name = nameField.text.trim()
        val decimalsText = decimalsField.text.trim()

        // Validation
        if (contractAddress.isEmpty()) {
            Messages.showErrorDialog("Please enter a contract address", "Validation Error")
            return
        }

        // Validate based on network type
        if (isTronNetwork) {
            if (!blockchainService.isValidTronAddress(contractAddress)) {
                Messages.showErrorDialog(
                    "Invalid TRON contract address format. Must be a 34-character address starting with T",
                    "Validation Error"
                )
                return
            }
        } else {
            if (!contractAddress.startsWith("0x") || contractAddress.length != 42) {
                Messages.showErrorDialog(
                    "Invalid contract address format. Must be a 42-character hex address starting with 0x",
                    "Validation Error"
                )
                return
            }
        }

        if (symbol.isEmpty()) {
            Messages.showErrorDialog("Please enter a token symbol", "Validation Error")
            return
        }

        val decimals = decimalsText.toIntOrNull()
        if (decimals == null || decimals < 0 || decimals > 18) {
            Messages.showErrorDialog("Please enter valid decimals (0-18)", "Validation Error")
            return
        }

        // Create the token
        resultToken = Token(
            id = existingToken?.id ?: Token.generateId(),
            contractAddress = contractAddress,
            symbol = symbol.uppercase(),
            name = name.ifEmpty { symbol },
            decimals = decimals,
            balance = existingToken?.balance ?: "0",
            networkId = networkId
        )

        super.doOKAction()
    }

    override fun dispose() {
        scope.cancel()
        super.dispose()
    }
}
