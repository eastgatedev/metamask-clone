package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class CreateWalletDialog(
    private val project: Project,
    private val walletManager: WalletManager
) : DialogWrapper(project) {
    private val nameField = JBTextField()
    private val passwordField = JBPasswordField()
    private val confirmPasswordField = JBPasswordField()

    init {
        title = "Create New Wallet"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        // Wallet Name
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Wallet Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        nameField.text = "Wallet ${walletManager.wallets.value.size + 1}"
        panel.add(nameField, gbc)

        // Password
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Password:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(passwordField, gbc)

        // Confirm Password
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        panel.add(JLabel("Confirm Password:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(confirmPasswordField, gbc)

        // Security Note
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        val noteLabel = JLabel("<html><small>⚠️ Store your password safely. It cannot be recovered!</small></html>")
        panel.add(noteLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        val name = nameField.text.trim()
        val password = String(passwordField.password)
        val confirmPassword = String(confirmPasswordField.password)

        if (name.isEmpty()) {
            Messages.showErrorDialog("Please enter a wallet name", "Validation Error")
            return
        }

        if (password.isEmpty()) {
            Messages.showErrorDialog("Please enter a password", "Validation Error")
            return
        }

        if (password.length < 8) {
            Messages.showErrorDialog("Password must be at least 8 characters", "Validation Error")
            return
        }

        if (password != confirmPassword) {
            Messages.showErrorDialog("Passwords do not match", "Validation Error")
            return
        }

        try {
            val wallet = walletManager.createWallet(name, password)
            Messages.showInfoMessage(
                "Wallet created successfully!\nAddress: ${wallet.address}",
                "Success"
            )
            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog("Failed to create wallet: ${e.message}", "Error")
        }
    }
}
