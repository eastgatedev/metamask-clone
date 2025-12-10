package dev.eastgate.metamaskclone.ui.dialogs

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBTextField
import dev.eastgate.metamaskclone.models.Token
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class AddTokenDialog(
    private val project: Project,
    private val networkId: String,
    private val existingToken: Token? = null
) : DialogWrapper(project) {

    private val contractAddressField = JBTextField()
    private val symbolField = JBTextField()
    private val nameField = JBTextField()
    private val decimalsField = JBTextField("18")

    var resultToken: Token? = null
        private set

    private val isEditMode = existingToken != null

    init {
        title = if (isEditMode) "Edit Token" else "Add Token"

        if (isEditMode && existingToken != null) {
            contractAddressField.text = existingToken.contractAddress
            symbolField.text = existingToken.symbol
            nameField.text = existingToken.name
            decimalsField.text = existingToken.decimals.toString()
        }

        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)

        var row = 0

        // Contract Address
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Contract Address:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        contractAddressField.columns = 35
        contractAddressField.toolTipText = "Token contract address (0x...)"
        contractAddressField.isEditable = !isEditMode // Can't change address in edit mode
        panel.add(contractAddressField, gbc)

        // Token Symbol
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Token Symbol:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        symbolField.toolTipText = "e.g., USDT, LINK, UNI"
        panel.add(symbolField, gbc)

        // Token Name (optional)
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Token Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        nameField.toolTipText = "Optional, e.g., Tether USD"
        panel.add(nameField, gbc)

        // Decimals
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.weightx = 0.0
        panel.add(JLabel("Decimals:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        decimalsField.toolTipText = "Token decimals (usually 18)"
        panel.add(decimalsField, gbc)

        // Note
        row++
        gbc.gridx = 0
        gbc.gridy = row
        gbc.gridwidth = 2
        val noteLabel = JLabel("<html><small>Note: Token balance fetching will be available in Phase 3 blockchain integration.</small></html>")
        panel.add(noteLabel, gbc)

        return panel
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

        if (!contractAddress.startsWith("0x") || contractAddress.length != 42) {
            Messages.showErrorDialog("Invalid contract address format. Must be a 42-character hex address starting with 0x", "Validation Error")
            return
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
}
