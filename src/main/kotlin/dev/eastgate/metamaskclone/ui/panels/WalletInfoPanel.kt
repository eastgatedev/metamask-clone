package dev.eastgate.metamaskclone.ui.panels

import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import dev.eastgate.metamaskclone.models.Wallet
import dev.eastgate.metamaskclone.utils.ClipboardUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class WalletInfoPanel : JPanel(BorderLayout()) {
    private val nameField = JBTextField()
    private val addressField = JBTextField()
    private val createdField = JBTextField()
    private val copyButton = JButton("Copy")

    private var currentWallet: Wallet? = null

    init {
        setupUI()
    }

    private fun setupUI() {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Selected Wallet"),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        )
        preferredSize = Dimension(0, 160)
        maximumSize = Dimension(Int.MAX_VALUE, 160)

        val infoPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(3, 5, 3, 5)
        gbc.anchor = GridBagConstraints.WEST

        // Name
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        val nameLabel = JBLabel("Name:")
        nameLabel.font = nameLabel.font.deriveFont(Font.BOLD)
        infoPanel.add(nameLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        nameField.isEditable = false
        infoPanel.add(nameField, gbc)

        // Address
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        val addressLabel = JBLabel("Address:")
        addressLabel.font = addressLabel.font.deriveFont(Font.BOLD)
        infoPanel.add(addressLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        addressField.isEditable = false
        addressField.font = Font("Monospaced", Font.PLAIN, 12)
        infoPanel.add(addressField, gbc)

        gbc.gridx = 2
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        copyButton.addActionListener { copyAddress() }
        infoPanel.add(copyButton, gbc)

        // Created date
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        val createdLabel = JBLabel("Created:")
        createdLabel.font = createdLabel.font.deriveFont(Font.BOLD)
        infoPanel.add(createdLabel, gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        createdField.isEditable = false
        infoPanel.add(createdField, gbc)

        add(infoPanel, BorderLayout.NORTH)

        // Initially show empty state
        showEmptyState()
    }

    fun updateWallet(wallet: Wallet?) {
        currentWallet = wallet

        if (wallet != null) {
            nameField.text = wallet.name
            addressField.text = wallet.address
            createdField.text = wallet.createdAt

            nameField.isVisible = true
            addressField.isVisible = true
            createdField.isVisible = true
            copyButton.isEnabled = true
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        nameField.text = ""
        addressField.text = ""
        createdField.text = ""
        copyButton.isEnabled = false
    }

    private fun copyAddress() {
        currentWallet?.let { wallet ->
            ClipboardUtil.copyToClipboard(wallet.address)
            Messages.showInfoMessage("Address copied to clipboard", "Success")
        }
    }

    fun exportPrivateKey(
        wallet: Wallet,
        walletManager: WalletManager
    ) {
        val password = Messages.showPasswordDialog(
            "Enter password to export private key:",
            "Export Private Key"
        )

        if (!password.isNullOrBlank()) {
            try {
                val privateKey = walletManager.exportPrivateKey(wallet.address, password)

                val result = Messages.showYesNoDialog(
                    "Private Key:\n$privateKey\n\nCopy to clipboard?",
                    "Private Key Export",
                    Messages.getWarningIcon()
                )

                if (result == Messages.YES) {
                    ClipboardUtil.copyToClipboard(privateKey)
                    Messages.showInfoMessage("Private key copied to clipboard", "Success")
                }
            } catch (e: Exception) {
                Messages.showErrorDialog("Failed to export private key: ${e.message}", "Error")
            }
        }
    }
}
