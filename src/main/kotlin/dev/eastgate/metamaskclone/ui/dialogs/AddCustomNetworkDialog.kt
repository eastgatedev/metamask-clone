package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.network.NetworkManager
import dev.eastgate.metamaskclone.core.storage.Network
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AddCustomNetworkDialog(
    private val project: Project,
    private val networkManager: NetworkManager,
    private val existingNetwork: Network? = null
) : DialogWrapper(project) {

    private val nameField = JBTextField()
    private val rpcUrlField = JBTextField()
    private val chainIdField = JBTextField()
    private val symbolField = JBTextField()
    private val blockExplorerField = JBTextField()
    private val isTestnetCheckbox = JBCheckBox("This is a testnet")

    private val isEditMode = existingNetwork != null

    init {
        title = if (isEditMode) "Edit Network" else "Add Custom Network"

        if (isEditMode && existingNetwork != null) {
            nameField.text = existingNetwork.name
            rpcUrlField.text = existingNetwork.rpcUrl
            chainIdField.text = existingNetwork.chainId.toString()
            symbolField.text = existingNetwork.symbol
            blockExplorerField.text = existingNetwork.blockExplorerUrl ?: ""
            isTestnetCheckbox.isSelected = existingNetwork.isTestnet
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // Network Name
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Network Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        nameField.columns = 25
        panel.add(nameField, gbc)

        // RPC URL
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("RPC URL:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        rpcUrlField.toolTipText = "e.g., https://rpc.example.com or http://localhost:8545"
        panel.add(rpcUrlField, gbc)

        // Chain ID
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Chain ID:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        chainIdField.toolTipText = "e.g., 1 for Ethereum Mainnet, 97 for BNB Testnet"
        panel.add(chainIdField, gbc)

        // Currency Symbol
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Currency Symbol:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        symbolField.toolTipText = "e.g., ETH, BNB, MATIC"
        panel.add(symbolField, gbc)

        // Block Explorer URL (optional)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Block Explorer URL:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        blockExplorerField.toolTipText = "Optional, e.g., https://etherscan.io"
        panel.add(blockExplorerField, gbc)

        // Is Testnet
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        panel.add(isTestnetCheckbox, gbc)

        // Note
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        val noteLabel = JLabel("<html><small>Note: No validation is performed. Ensure the RPC URL and Chain ID are correct.</small></html>")
        panel.add(noteLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        val name = nameField.text.trim()
        val rpcUrl = rpcUrlField.text.trim()
        val chainIdText = chainIdField.text.trim()
        val symbol = symbolField.text.trim()
        val blockExplorerUrl = blockExplorerField.text.trim().ifEmpty { null }
        val isTestnet = isTestnetCheckbox.isSelected

        // Basic validation
        if (name.isEmpty()) {
            Messages.showErrorDialog("Please enter a network name", "Validation Error")
            return
        }

        if (rpcUrl.isEmpty()) {
            Messages.showErrorDialog("Please enter an RPC URL", "Validation Error")
            return
        }

        val chainId = chainIdText.toIntOrNull()
        if (chainId == null || chainId <= 0) {
            Messages.showErrorDialog("Please enter a valid Chain ID (positive integer)", "Validation Error")
            return
        }

        if (symbol.isEmpty()) {
            Messages.showErrorDialog("Please enter a currency symbol", "Validation Error")
            return
        }

        // Create or update the network
        val networkId = existingNetwork?.id ?: networkManager.generateCustomNetworkId()
        val network = Network(
            id = networkId,
            name = name,
            rpcUrl = rpcUrl,
            chainId = chainId,
            symbol = symbol,
            blockExplorerUrl = blockExplorerUrl,
            isTestnet = isTestnet,
            isCustom = true
        )

        val success = if (isEditMode) {
            networkManager.updateCustomNetwork(network)
        } else {
            networkManager.addCustomNetwork(network)
        }

        if (!success) {
            Messages.showErrorDialog(
                "A network with Chain ID $chainId already exists",
                "Duplicate Chain ID"
            )
            return
        }

        super.doOKAction()
    }
}
