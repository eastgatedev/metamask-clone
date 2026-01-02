package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.core.wallet.WalletManager
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class ImportWalletDialog(
    private val project: Project,
    private val walletManager: WalletManager
) : DialogWrapper(project) {
    private val nameField = JBTextField()
    private val privateKeyArea = JBTextArea(3, 40)
    private val passwordField = JBPasswordField()
    private val confirmPasswordField = JBPasswordField()

    init {
        title = "Import Wallet"
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
        nameField.text = "Imported Wallet"
        panel.add(nameField, gbc)

        // Private Key
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        panel.add(JLabel("Private Key:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        privateKeyArea.lineWrap = true
        privateKeyArea.wrapStyleWord = true
        privateKeyArea.border = BorderFactory.createEtchedBorder()
        val scrollPane = JScrollPane(privateKeyArea)
        panel.add(scrollPane, gbc)

        // Password
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        panel.add(JLabel("Password:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(passwordField, gbc)

        // Confirm Password
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.weightx = 0.0
        panel.add(JLabel("Confirm Password:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        panel.add(confirmPasswordField, gbc)

        // Security Note
        gbc.gridx = 0
        gbc.gridy = 4
        gbc.gridwidth = 2
        val noteLabel = JLabel("<html><small>⚠️ Never share your private key with anyone!</small></html>")
        panel.add(noteLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        val name = nameField.text.trim()
        val privateKey = privateKeyArea.text.trim()
        val password = String(passwordField.password)
        val confirmPassword = String(confirmPasswordField.password)

        if (name.isEmpty()) {
            Messages.showErrorDialog("Please enter a wallet name", "Validation Error")
            return
        }

        if (privateKey.isEmpty()) {
            Messages.showErrorDialog("Please enter a private key", "Validation Error")
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
            val wallet = walletManager.importWallet(privateKey, name, password)
            Messages.showInfoMessage(
                "Wallet imported successfully!\nAddress: ${wallet.address}",
                "Success"
            )
            super.doOKAction()
        } catch (e: Exception) {
            Messages.showErrorDialog("Failed to import wallet: ${e.message}", "Error")
        }
    }
}
