package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.models.Token
import dev.eastgate.metamaskclone.models.Wallet
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class SendTokenDialog(
    private val project: Project,
    private val wallet: Wallet,
    private val token: Token? = null,
    private val availableTokens: List<Token> = emptyList()
) : DialogWrapper(project) {

    private val toAddressField = JBTextField()
    private val amountField = JBTextField()
    private val tokenSelector = JComboBox<String>()
    private val gasLimitField = JBTextField("21000")
    private val gasPriceField = JBTextField("5")

    init {
        title = "Send"
        init()
        setupTokenSelector()
    }

    private fun setupTokenSelector() {
        // Add native token option
        tokenSelector.addItem("Native Token")

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
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // From address (read-only)
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("From:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        val fromField = JBTextField(wallet.getShortAddress())
        fromField.isEditable = false
        fromField.toolTipText = wallet.address
        panel.add(fromField, gbc)

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
        val gasLabel = JLabel("Gas Settings (placeholder - not functional)")
        gasLabel.font = gasLabel.font.deriveFont(java.awt.Font.ITALIC)
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
        gasLimitField.isEnabled = false
        panel.add(gasLimitField, gbc)

        // Gas Price
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Gas Price (Gwei):"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        gasPriceField.isEnabled = false
        panel.add(gasPriceField, gbc)

        // Warning note
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        val noteLabel = JLabel("<html><small>⚠️ This is a UI preview. Actual sending is not implemented yet.</small></html>")
        panel.add(noteLabel, gbc)

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
        if (amountValue == null || amountValue <= java.math.BigDecimal.ZERO) {
            Messages.showErrorDialog("Please enter a valid positive amount", "Validation Error")
            return
        }

        // Show confirmation (UI only - doesn't actually send)
        val selectedToken = if (tokenSelector.selectedIndex == 0) "Native Token" else availableTokens.getOrNull(tokenSelector.selectedIndex - 1)?.symbol ?: "Token"
        val result = Messages.showYesNoDialog(
            project,
            "Send $amount $selectedToken to $toAddress?\n\n(This is a UI preview - no actual transaction will be sent)",
            "Confirm Send",
            Messages.getQuestionIcon()
        )

        if (result == Messages.YES) {
            Messages.showInfoMessage(
                project,
                "Transaction preview completed.\nActual blockchain integration coming in Phase 3.",
                "Send Preview"
            )
            super.doOKAction()
        }
    }

    override fun createActions(): Array<Action> {
        return arrayOf(okAction, cancelAction)
    }
}
